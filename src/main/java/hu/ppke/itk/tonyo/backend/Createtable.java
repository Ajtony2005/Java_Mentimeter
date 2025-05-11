package hu.ppke.itk.tonyo.backend;

import java.sql.*;

public class Createtable {
    private final Connection connection;

    public Createtable(String dbName, Connection connection) {
        this.connection = connection;
    }

    public void createTable() {
        try (Statement stmt = connection.createStatement()) {
            // Felhasználók tábla
            String createUsers = """
                CREATE TABLE IF NOT EXISTS felhasznalok (
                    felhasznalo_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    felhasznalonev TEXT NOT NULL UNIQUE,
                    jelszo TEXT NOT NULL,
                    letrehozva TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """;

            // Szavazások tábla
            String createPolls = """
                CREATE TABLE IF NOT EXISTS szavazasok (
                    szavazas_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    felhasznalo_id INTEGER NOT NULL,
                    cim TEXT NOT NULL,
                    kerdes TEXT NOT NULL,
                    tipus TEXT NOT NULL CHECK (tipus IN ('SZO_FELHO', 'TOBBVALASZTOS', 'SKALA')),
                    beallitasok TEXT NOT NULL,
                    csatlakozasi_kod TEXT NOT NULL UNIQUE CHECK (length(csatlakozasi_kod) = 8),
                    allapot TEXT NOT NULL CHECK (allapot IN ('LEZART', 'NYITOTT', 'SZAVAZAS', 'EREDMENY')),
                    letrehozva TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (felhasznalo_id) REFERENCES felhasznalok(felhasznalo_id) ON DELETE CASCADE
                );
            """;

            // Szavazási opciók tábla
            String createPollOptions = """
                CREATE TABLE IF NOT EXISTS szavazasi_opciok (
                    opcio_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    szavazas_id INTEGER NOT NULL,
                    opcio_szoveg TEXT,
                    skala_min REAL,
                    skala_max REAL,
                    FOREIGN KEY (szavazas_id) REFERENCES szavazasok(szavazas_id) ON DELETE CASCADE
                );
            """;


            String createResponses = """
                CREATE TABLE IF NOT EXISTS valaszok (
                    valasz_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    szavazas_id INTEGER NOT NULL,
                    opcio_id INTEGER,
                    szo_felho_valasz TEXT,
                    skala_ertek REAL,
                    leadva TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (szavazas_id) REFERENCES szavazasok(szavazas_id) ON DELETE CASCADE,
                    FOREIGN KEY (opcio_id) REFERENCES szavazasi_opciok(opcio_id) ON DELETE CASCADE
                );
            """;

            String createJoinCodes = """
                CREATE TABLE IF NOT EXISTS csatlakozasi_kodok (
                    csatlakozasi_kod TEXT PRIMARY KEY CHECK (length(csatlakozasi_kod) = 8),
                    szavazas_id INTEGER NOT NULL,
                    letrehozva TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (szavazas_id) REFERENCES szavazasok(szavazas_id) ON DELETE CASCADE
                );
            """;

            stmt.execute(createUsers);
            stmt.execute(createPolls);
            stmt.execute(createPollOptions);
            stmt.execute(createResponses);
            stmt.execute(createJoinCodes);

            System.out.println("Táblák sikeresen létrejöttek");
        } catch (SQLException e) {
            System.err.println("Táblák létrehozása sikertelen: " + e.getMessage());
        }
    }
}