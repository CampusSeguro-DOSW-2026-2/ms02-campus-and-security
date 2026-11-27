#!/usr/bin/env bash
# Despliega MS02 en su VM: stack ms02-<entorno> detrás del Caddy de borde, y verifica el despliegue.
# RF-MS02-06: la base de datos ya no es parte del stack; MS02 se conecta a una instancia administrada
# (Azure Database for PostgreSQL) con los datos de conexión de DB_HOST/DB_USER/DB_PASSWORD (y DB_NAME/DB_PORT,
# opcionales) del Environment de GitHub.
# Lo ejecuta el runner self-hosted desde .github/workflows/deploy.yml con las variables y secretos del Environment
# ms02-<entorno> de GitHub. Es idempotente: volver a ejecutarlo con otro TAG es el rollback.
# El borde (Caddy con mTLS) se levanta una sola vez a mano, con la guía "Conexión al API Gateway" de la wiki.
# Nunca borra los volúmenes: las fotografías se conservan entre despliegues (la base de datos la conserva el
# servicio administrado, no un volumen de este stack).
set -euo pipefail

falta() { echo "::error::Falta la variable $1 (Environment ms02-${ENTORNO:-?} en GitHub)" >&2; exit 1; }
for VAR in ENTORNO DB_HOST DB_USER DB_PASSWORD QR_BASE_URL; do
    [ -n "${!VAR:-}" ] || falta "$VAR"
done
[[ "$ENTORNO" =~ ^(test|prod)$ ]] || { echo "::error::ENTORNO debe ser test o prod" >&2; exit 1; }

# Fotografías: local (volumen "imagenes", por defecto) o azure (Azure Blob Storage, un contenedor por entorno).
TIPO_IMAGENES="${IMAGEN_STORAGE_TYPE:-local}"
[[ "$TIPO_IMAGENES" =~ ^(local|azure)$ ]] || { echo "::error::IMAGEN_STORAGE_TYPE debe ser local o azure" >&2; exit 1; }
[ "$TIPO_IMAGENES" != azure ] || [ -n "${AZURE_STORAGE_CONNECTION_STRING:-}" ] || falta AZURE_STORAGE_CONNECTION_STRING

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
# Misma etiqueta que publica el CI (docker/metadata-action, type=sha): los 7 primeros caracteres del commit.
TAG="${TAG:-sha-$(git -C "$REPO_DIR" rev-parse HEAD | cut -c1-7)}"
[[ "$TAG" =~ ^sha-[0-9a-f]{7}$ ]] || { echo "::error::TAG debe tener la forma sha-xxxxxxx (recibido: $TAG)" >&2; exit 1; }
IMAGE="${IMAGE:-ghcr.io/campusseguro-dosw-2026-2/ms02-campus-and-security}"
DEPLOY_DIR="${DEPLOY_DIR:-$HOME/campusseguro/ms02}"
PUERTO=$([ "$ENTORNO" = prod ] && echo 443 || echo 8443)
STACK_DIR="$DEPLOY_DIR/$ENTORNO"

docker network inspect campus-edge >/dev/null 2>&1 || {
    echo "::error::No existe la red campus-edge: levante primero el borde Caddy de la VM (README, Despliegue en la nube)" >&2
    exit 1
}

# Valores de una línea para el .env (entre comillas simples, sin interpolar).
linea_env() {
    [[ "$2" != *"'"* ]] || { echo "::error::$1 no puede contener comillas simples" >&2; exit 1; }
    printf "%s='%s'\n" "$1" "$2"
}

umask 077
mkdir -p "$STACK_DIR"

echo "== Archivos del despliegue ($ENTORNO, $TAG) en $STACK_DIR"
cp "$REPO_DIR/infra/docker-compose.prod.yml" "$STACK_DIR/docker-compose.yml"
{
    linea_env ENTORNO "$ENTORNO"
    linea_env IMAGE "$IMAGE"
    linea_env TAG "$TAG"
    linea_env DB_HOST "$DB_HOST"
    linea_env DB_USER "$DB_USER"
    linea_env DB_PASSWORD "$DB_PASSWORD"
    [ -z "${DB_PORT:-}" ] || linea_env DB_PORT "$DB_PORT"
    [ -z "${DB_NAME:-}" ] || linea_env DB_NAME "$DB_NAME"
    linea_env QR_BASE_URL "$QR_BASE_URL"
    [ -z "${NAVIGATION_BASE_URL:-}" ] || linea_env NAVIGATION_BASE_URL "$NAVIGATION_BASE_URL"
    [ -z "${NAVIGATION_PROFILE:-}" ] || linea_env NAVIGATION_PROFILE "$NAVIGATION_PROFILE"
    [ -z "${IMAGEN_STORAGE_TYPE:-}" ] || linea_env IMAGEN_STORAGE_TYPE "$IMAGEN_STORAGE_TYPE"
    [ -z "${AZURE_STORAGE_CONNECTION_STRING:-}" ] || linea_env AZURE_STORAGE_CONNECTION_STRING "$AZURE_STORAGE_CONNECTION_STRING"
    [ -z "${AZURE_STORAGE_CONTAINER:-}" ] || linea_env AZURE_STORAGE_CONTAINER "$AZURE_STORAGE_CONTAINER"
} > "$STACK_DIR/.env"
printf '%s\n' "${MS02_EXTRA_ENV:-}" > "$STACK_DIR/extra.env"

echo "== Imagen $IMAGE:$TAG"
# En la VM el runner entrega GHCR_USER/GHCR_TOKEN; PULL_POLICY=missing solo sirve para probar con una imagen local.
if [ -n "${GHCR_TOKEN:-}" ]; then
    printf '%s' "$GHCR_TOKEN" | docker login ghcr.io -u "${GHCR_USER:?Falta GHCR_USER}" --password-stdin >/dev/null
fi
C=(docker compose --project-directory "$STACK_DIR")
"${C[@]}" config --quiet
"${C[@]}" up -d --remove-orphans --pull "${PULL_POLICY:-always}"
[ -z "${GHCR_TOKEN:-}" ] || docker logout ghcr.io >/dev/null

# El firewall y el mTLS impiden probar desde fuera: se prueba dentro de la VM, por la red campus-edge.
echo "== Verificación de http://ms02-$ENTORNO:8082/actuator/health (espera hasta 3 min: arranque de MS02 y conexión con la base de datos administrada)"
CURL=(docker run --rm --network campus-edge curlimages/curl:8.11.1 -s -o /dev/null -w '%{http_code}')
for _ in $(seq 1 36); do
    if [ "$("${CURL[@]}" "http://ms02-$ENTORNO:8082/actuator/health" || true)" = 200 ]; then
        # Sin el certificado del GW, el borde debe cortar la conexión en TLS (curl 35, 55 o 56), nunca responder.
        # Otro código (p. ej. 0 = respondió, 7 = nadie escucha) significa que el borde falta o no exige el certificado.
        RC=0; curl -sk -o /dev/null "https://127.0.0.1:$PUERTO/actuator/health" || RC=$?
        [[ "$RC" =~ ^(35|55|56)$ ]] || {
            echo "::error::El borde :$PUERTO no rechazó la conexión sin certificado de cliente (curl terminó con $RC)"
            exit 1
        }
        echo "Despliegue verificado: health 200 y borde :$PUERTO exige el certificado del GW ($ENTORNO, $TAG)"
        exit 0
    fi
    sleep 5
done
echo "::error::MS02 no respondió 200 en /actuator/health. Revise: docker compose --project-directory $STACK_DIR logs"
"${C[@]}" logs --tail=50 ms02 || true
exit 1
