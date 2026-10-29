package edu.eci.dosw.campusseguro.campus;

import java.util.UUID;

public final class Ids {

    private Ids() {
    }

    public static UUID uuid(long n) {
        return new UUID(0L, n);
    }
}