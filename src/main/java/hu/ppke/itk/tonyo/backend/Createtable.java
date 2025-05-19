package hu.ppke.itk.tonyo.backend;

import java.sql.*;

/**
 * A {@code Createtable} osztály egy SQLite adatbázis tábláinak létrehozására szolgál egy szavazási
 * rendszer számára. Öt táblát hoz létre: {@code felhasznalok}, {@code szavazasok},
 * {@code szavazasi_opciok}, {@code valaszok} és {@code csatlakozasi_kodok}. Az osztály a megadott
 * {@code Connection} objektumot használja az adatbázis műveletekhez.
 */
public class Createtable {

    /** Az SQLite adatbázishoz való kapcsolat. */
    private final Connection connection;

    /**
     * Konstruktor, amely inicializálja az adatbázis kapcsolatot.
     *
     * @param dbName az adatbázis neve (jelenleg nem használatos)
     * @param connection az SQLite adatbázishoz való kapcsolat
     */
    public Createtable(String dbName, Connection connection) {
        this.connection = connection;
    }

    /**
     * Létrehozza a szavazási rendszerhez szükséges táblákat az adatbázisban. A táblák csak akkor
     * jönnek létre, ha még nem léteznek. A táblák a következőkből állnak:
     * <ul>
     *     <li>{@code felhasznalok}: Felhasználók adatai</li>
     *     <li>{@code szavazasok}: Szavazások metaadatai</li>
     *     <li>{@code szavazasi_opciok}: Szavazási opciók (válaszlehetőségek)</li>
     *     <li>{@code valaszok}: Leadott szavazatok</li>
     *     <li>{@code csatlakozasi_kodok}: Szavazásokhoz tartozó csatlakozási kódok</li>
     * </ul>
     *
     * @throws SQLException ha a táblák létrehozása során hiba történik
     */
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

            // Válaszok tábla
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

            // Csatlakozási kódok tábla
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