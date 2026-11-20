# Despliegue de MS02 en Azure (detrás del API Gateway)

MS02 corre en su propia VM de Azure, detrás del API Gateway, siguiendo la guía
[Conexión al API Gateway](https://github.com/CampusSeguro-DOSW-2026-2/documentation/wiki/Conexi%C3%B3n-al-API-Gateway)
y el mismo esquema de MS01:

* El firewall solo acepta la IP del GW.
* Un Caddy de borde exige el certificado de cliente del GW (mTLS).
* Prueba y producción comparten la VM, cada una con su stack (MS02 + PostgreSQL), sus secretos y sus certificados:

| Puerto de la VM | Uso |
|---|---|
| `443` | Producción (`ms02-prod`) |
| `8443` | Prueba (`ms02-test`) |
| `22` | SSH, solo desde la IP de quien administra |

**Empieza vacío.** El stack nuevo crea su propia base de datos: Flyway aplica el esquema y deja un campus por defecto
("Campus principal"). Nada de lo registrado en un entorno local (edificios, salidas, zonas, rutas, fotografías) viaja a la
nube; se carga allí después, con el sistema ya desplegado.

**Los datos se conservan.** La base vive en el volumen `datos` y las fotografías en el volumen `imagenes` de cada stack.
Redesplegar, cambiar de versión o volver a una anterior (rollback) no los toca. Lo único que los borraría es
`docker compose down -v` o `docker volume rm`: no se ejecutan nunca en la VM.

Los pasos 1 a 6 se hacen una sola vez. Los comandos marcados *(VM)* van en la VM por SSH; los demás, en tu PC.

## 1. VM y firewall en Azure
1. En el portal de Azure: **Virtual machines › Create**. Ubuntu Server **24.04 LTS x64**, 2 vCPU y 4 GiB
   (`Standard_B2s` o `B2ls_v2`), disco de 30 GiB, autenticación por llave SSH. Guarda la llave privada fuera del
   repositorio.
2. **IP pública Standard estática** (si la IP cambia, hay que emitir certificados nuevos).
3. Reglas de entrada del NSG:

   | Puerto | Origen |
   |---|---|
   | `443/tcp` y `8443/tcp` | Solo `135.232.229.138/32` (IP del GW) |
   | `22/tcp` | Solo tu IP |

   No abras nada más: ni `8082` ni `5432`. La salida queda abierta, porque MS02 necesita GitHub y el servicio de
   navegación (OSRM).
4. Instala Docker *(VM)*:
   ```bash
   curl -fsSL https://get.docker.com | sudo sh
   sudo usermod -aG docker "$USER"   # cierra la sesión SSH y vuelve a entrar
   ```

## 2. Alta con el equipo del GW
Por un canal privado, con el mensaje de alta de la guía. Entrega la IP estática, el puerto `8082`, los prefijos de
`swagger.yml` de MS02 (los mismos de `SQUAD_PATHS` en `.env.example`, incluido `/api/v1/images`) y las rutas públicas de
la consulta por QR (`docs/consulta-publica-qr.md`).

| Entregas | Recibes, por entorno (prueba y producción) |
|---|---|
| La IP estática de la VM | `servidor.crt`, `servidor.key` y `ca.pem` |
| Prefijos y rutas públicas | `GATEWAY_SERVICE_KEY`, solo si MS02 llama a otro MS |

## 3. Borde Caddy (mTLS) *(VM)*
```bash
mkdir -p ~/campusseguro/edge && cd ~/campusseguro/edge
curl -fsSLO https://raw.githubusercontent.com/CampusSeguro-DOSW-2026-2/api-gateway/develop/infra/caddy/Caddyfile.microservicio
curl -fsSLO https://raw.githubusercontent.com/CampusSeguro-DOSW-2026-2/api-gateway/develop/infra/caddy/docker-compose.microservicio.yml
printf 'UPSTREAM_PROD=ms02-prod:8082\nUPSTREAM_TEST=ms02-test:8082\n' > .env
```
En tu PC, deja los certificados recibidos así y cópialos a la VM (`scp` funciona en PowerShell):
```text
certs/prod/servidor.crt  certs/prod/servidor.key  certs/prod/ca.pem
certs/test/servidor.crt  certs/test/servidor.key  certs/test/ca.pem
```
```bash
scp -i <llave> -r certs <usuario>@<IP de la VM>:~/campusseguro/edge/
```
Luego, en la VM:
```bash
cd ~/campusseguro/edge
chmod 600 certs/*/servidor.key
docker compose -f docker-compose.microservicio.yml up -d
```
El borde crea la red `campus-edge`, a la que se unen los stacks. Mientras no haya stacks responde `502`. No modifiques el
Caddyfile.

## 4. Runner self-hosted *(VM)*
1. En GitHub: **Settings › Actions › Runners › New self-hosted runner** (Linux, x64). Ejecuta en la VM los comandos de
   *Download* que muestra la página.
2. Al configurarlo, agrega la etiqueta `ms02` e instálalo como servicio:
   ```bash
   ./config.sh --url https://github.com/CampusSeguro-DOSW-2026-2/ms02-campus-and-security --token <TOKEN de la página> --labels ms02 --unattended
   sudo ./svc.sh install "$USER"
   sudo ./svc.sh start
   ```
3. **Settings › Actions › General › Fork pull request workflows from outside collaborators**: elige *Require approval for
   all external contributors*, para que un PR externo no se ejecute en la VM.

## 5. Environments de GitHub
Crea `ms02-test` y `ms02-prod` en **Settings › Environments**:
* `ms02-test`: en **Deployment branches**, permite solo `develop`.
* `ms02-prod`: activa **Required reviewers** y permite solo `main`.

| En `ms02-test` y `ms02-prod` | Tipo | Valor |
|---|---|---|
| `QR_BASE_URL` | Variable | Dirección pública del frontend de ese entorno (va dentro de los códigos QR). Prueba: `https://campusseguroftest.z41.web.core.windows.net`. |
| `NAVIGATION_BASE_URL`, `NAVIGATION_PROFILE` | Variable (opcional) | Servidor OSRM peatonal. Por defecto `https://routing.openstreetmap.de/routed-foot` y `foot`. |
| `MS02_EXTRA_ENV` | Variable (opcional) | Líneas `CLAVE=valor` extra, p. ej. `RUTAS_AUTO_MAX_ZONAS=3`. |
| `DEPLOY_DIR` | Variable (opcional) | `~/campusseguro/ms02` por defecto. |
| `DB_PASSWORD` | Secreto | `openssl rand -base64 24`, distinta en cada entorno. Solo se aplica cuando se crea la BD (primer despliegue). |

Ningún valor puede contener comillas simples (`'`). Nunca subas `certs/`, `*.key`, `*.pem` ni `.env`.

## 6. Desplegar
1. **Prueba:** cada push o merge a `develop` publica la imagen en GHCR y la despliega en `ms02-test`
   (job **desplegar-test** de `ci.yml`). Si es la primera vez, el paquete `ms02-campus-and-security` de GHCR debe
   permitir que el repositorio lo lea.
2. **Producción:** tras mezclar en `main`, **Actions › Desplegar a producción › Run workflow**, rama `main`, con el tag de
   la imagen que ya funciona en prueba (`sha-` más los 7 primeros caracteres del commit, visible en el log de
   *desplegar-test*). Un tag anterior es el rollback.

`infra/deploy.sh` verifica cada despliegue dentro de la VM: `/actuator/health` responde 200 por la red `campus-edge` y el
borde rechaza conexiones sin el certificado del GW.

## 7. Alta en el GW y verificación
Cuando el health responda `UP`, avisa al equipo del GW para que agregue la ruta de MS02. Después, desde tu PC:
```bash
# Acceso directo a la VM: timeout (el firewall solo acepta al GW)
curl -m 5 -k https://<IP de la VM>:8443/
# Por el GW, sin token: 401 NO_AUTENTICADO
curl -i https://campusseguro-gw.northcentralus.cloudapp.azure.com:8443/api/v1/buildings
```
Con un token válido (login por el GW) la misma ruta responde con los edificios del campus (lista vacía al inicio).

## Operación
```bash
cd ~/campusseguro/ms02/test      # o prod
docker compose ps
docker compose logs --tail=100 ms02
docker compose restart ms02      # reinicia solo MS02, sin tocar la base
```

## 8. Fotografías en Azure Blob Storage
Por defecto MS02 guarda las fotografías en el volumen `imagenes` de la VM (`IMAGEN_STORAGE_TYPE=local`). Para guardarlas
en Azure Blob Storage, con un contenedor por entorno:

1. En el portal de Azure: **Storage accounts › Create**. Misma región que la VM, rendimiento *Standard*, redundancia
   *LRS* y *Allow Blob anonymous access* desactivado: las fotos no se publican directamente, MS02 las sigue sirviendo en
   `GET /api/v1/images/{id}` con los mismos permisos.
2. En **Security + networking › Access keys**, copia una **Connection string**. MS02 crea el contenedor al arrancar si
   no existe.
3. En `ms02-test` y `ms02-prod` (**Settings › Environments**):

| Nombre | Tipo | Valor |
|---|---|---|
| `AZURE_STORAGE_CONNECTION_STRING` | Secreto | La cadena de conexión de la cuenta. No puede contener comillas simples (`'`). |
| `AZURE_STORAGE_CONTAINER` | Variable | Un contenedor por entorno: `imagenes-test` e `imagenes-prod`. |
| `IMAGEN_STORAGE_TYPE` | Variable | `azure`. Sin esta variable, o con `local`, se usa el volumen. |

### Pasar las fotos que ya están en el volumen
La base de datos guarda rutas como `imagenes/<uuid>.jpg`, que son el mismo nombre del blob: no se toca la base. El orden
importa, porque al cambiar a `azure` las fotos que sigan solo en el volumen dejarían de verse:

1. Despliega esta versión **sin** `IMAGEN_STORAGE_TYPE`: sigue en `local` y nada cambia.
2. Copia las fotos al contenedor *(VM)*. La cadena de conexión se pide sin mostrarla ni guardarla en el historial:
   ```bash
   ENTORNO=prod                     # o test
   CONTENEDOR=imagenes-$ENTORNO
   read -rsp "Cadena de conexión: " AZURE_STORAGE_CONNECTION_STRING; export AZURE_STORAGE_CONNECTION_STRING
   docker run --rm -e AZURE_STORAGE_CONNECTION_STRING mcr.microsoft.com/azure-cli \
     az storage container create --name "$CONTENEDOR"
   docker run --rm -e AZURE_STORAGE_CONNECTION_STRING -v "ms02-${ENTORNO}_imagenes:/datos:ro" \
     mcr.microsoft.com/azure-cli az storage blob upload-batch --destination "$CONTENEDOR" --source /datos
   ```
3. Comprueba que las dos cuentas den el mismo número *(VM)*:
   ```bash
   docker run --rm -v "ms02-${ENTORNO}_imagenes:/datos:ro" alpine sh -c 'find /datos/imagenes -type f | wc -l'
   docker run --rm -e AZURE_STORAGE_CONNECTION_STRING mcr.microsoft.com/azure-cli \
     az storage blob list --container-name "$CONTENEDOR" --query "length(@)" -o tsv
   ```
4. Crea el secreto y las variables del Environment (tabla de arriba) y vuelve a desplegar el mismo tag.
5. Repite el comando `upload-batch` del paso 2 una vez más, por si se subió alguna foto entre la copia y el cambio.
6. El volumen `imagenes` se queda como respaldo: no lo borres.

**Volver atrás.** Cambia `IMAGEN_STORAGE_TYPE` a `local` y despliega de nuevo. Las fotos subidas mientras estuvo en `azure`
solo existen en el contenedor, así que habría que copiarlas de vuelta al volumen.
