# Java Mentimeter

Asztali, JavaFX alapú, kliens-szerver szavazórendszer SQLite háttérrel. A projekt célja egy Mentimeter-szerű alkalmazás megvalósítása, ahol a hosztok szavazásokat hozhatnak létre és kezelhetnek, a résztvevők pedig csatlakozási kóddal szavazhatnak.

## Tartalomjegyzék

- [Főbb képességek](#fobb-kepessegek)
- [Architektúra](#architektura)
- [Technológiák](#technologiak)
- [Előfeltételek](#elofeltetelek)
- [Gyors indítás](#gyors-inditas)
- [Részletes futtatás](#reszletes-futtatas)
- [Projektstruktúra](#projektstruktura)
- [Kommunikációs protokoll (JSON)](#kommunikacios-protokoll-json)
- [Adatbázis séma](#adatbazis-sema)
- [Konfiguráció](#konfiguracio)
- [Hibakezelés](#hibakezeles)
- [Fejlesztői tippek](#fejlesztoi-tippek)
- [Gyakori problémák](#gyakori-problemak)

## Főbb képességek

- Felhasználókezelés:
  - Regisztráció
  - Bejelentkezés / kijelentkezés
  - Jelszó hash-elés (`jBCrypt`)
- Szavazáskezelés:
  - Szavazás létrehozása
  - Saját szavazások listázása
  - Szavazás szerkesztése
  - Szavazás állapotváltás (`NYITOTT`, `SZAVAZAS`, `EREDMENY`, `LEZART`)
  - Szavazás adatainak nullázása
- Részvétel és szavazás:
  - Csatlakozás 8 karakteres kóddal
  - Szavazat leadása
- Eredmények:
  - Többválasztós eredmények összesítése
  - Skálás eredmények átlaga
  - Szófelhő jellegű válaszok összesítése
  - Broadcast frissítések a szavazáshoz csatlakozott klienseknek

## Architektúra

A rendszer két fő komponensből áll:

1. Backend szerver (`server`):
   - TCP socketen fogad klienskapcsolatokat (`42069` port).
   - Minden klienshez külön `ClientHandler` szálat indít.
   - A bejövő JSON kéréseket a `RequestProcessor` irányítja a megfelelő művelethez.
   - SQLite adatbázist használ (`mentimeter.db`).

2. JavaFX kliens (`App`):
   - Oldal-alapú felület (bejelentkezés, létrehozás, csatlakozás, szavazás, eredmények, adminisztráció).
   - A `cliens` osztály kezeli a socket kapcsolatot és a JSON üzenetváltást.
   - A szerverüzeneteket az aktív oldal komponensnek továbbítja.

## Technológiák

- Java 23
- Maven (wrapper: `mvnw`, `mvnw.cmd`)
- JavaFX 17
- SQLite (`sqlite-jdbc`)
- Gson (JSON szerializáció/deszerializáció)
- jBCrypt (jelszó hash)

## Előfeltételek

- JDK 23 telepítve
- Internetkapcsolat az első Maven függőségletöltéshez
- Linux/macOS alatt futtatható `./mvnw` script

Ellenőrzés:

```bash
java -version
./mvnw -version
```

## Gyors indítás

1. Fordítás:

```bash
./mvnw clean compile
```

2. Szerver indítása (IDE-ből ajánlott):
   - Futtasd a `hu.ppke.itk.tonyo.backend.server` osztály `main` metódusát.

3. Kliens indítása (IDE-ből ajánlott):
   - Futtasd a `hu.ppke.itk.tonyo.frontend.App` osztály `main` metódusát.

4. Használat:
   - Csatlakozás a szerverhez (`localhost:42069`)
   - Regisztráció / bejelentkezés
   - Szavazás létrehozása vagy csatlakozás kóddal

## Részletes futtatás

### 1) Build

```bash
./mvnw clean package
```

### 2) Szerver futtatása parancssorból

A projekt Maven `exec:java` pluginnel is futtatható:

```bash
./mvnw -DskipTests exec:java -Dexec.mainClass=hu.ppke.itk.tonyo.backend.server
```

### 3) JavaFX kliens futtatása

A kliens futtatásához IDE (IntelliJ / VS Code Java Extension Pack) javasolt.

Megjegyzés: a `pom.xml` jelenleg nem az `App` osztályra mutató JavaFX `mainClass` beállítást tartalmaz, ezért a `javafx:run` cél használatához a konfigurációt szükség esetén frissíteni kell.

## Projektstruktúra

```text
src/main/java/hu/ppke/itk/tonyo/
  backend/
    server.java
    ClientHandler.java
    RequestProcessor.java
    createDB.java
    Createtable.java
    ... (műveleti osztályok: login, poll, vote, result)
  frontend/
    App.java
    cliens.java
    pages/
      ConnectPage.java
      LoginPage.java
      RegisterPage.java
      HomePage.java
      CreatePollPage.java
      JoinPollPage.java
      VotePage.java
      PollResultsPage.java
      ManagePollPage.java
      PollControlPage.java
      ...
src/main/resources/
  styles.css
  errorcode.json
```

## Kommunikációs protokoll (JSON)

A kliens minden kérésben `action` mezőt küld, erre a szerver JSON választ ad.

Használt action értékek:

- `register`
- `login`
- `logout`
- `create_poll`
- `list_polls`
- `list_my_polls`
- `join_poll`
- `submit_vote`
- `get_results`
- `get_poll_results`
- `update_poll_status`
- `reset_poll_data`
- `edit_poll`
- `get_poll_details`

Broadcast események:

- `status_update`
- `results_update` (`broadcast_results` action alatt)

### Példa kérés

```json
{
  "action": "join_poll",
  "joinCode": "AB12CD34"
}
```

### Példa válasz

```json
{
  "action": "join_poll",
  "status": "success",
  "message": "Sikeres csatlakozás"
}
```

## Adatbázis séma

Az alkalmazás induláskor automatikusan létrehozza a `mentimeter.db` fájlt és az alábbi táblákat:

- `felhasznalok`
- `szavazasok`
- `szavazasi_opciok`
- `valaszok`
- `csatlakozasi_kodok`

Kulcspontok:

- A `szavazasok.tipus` értékei: `SZO_FELHO`, `TOBBVALASZTOS`, `SKALA`
- A `szavazasok.allapot` értékei: `LEZART`, `NYITOTT`, `SZAVAZAS`, `EREDMENY`
- A `csatlakozasi_kod` 8 karakteres, egyedi azonosító

## Konfiguráció

Jelenlegi alapértelmezések:

- Szerver port: `42069`
- Adatbázis fájl: `mentimeter.db`
- Kliens csatlakozás: `localhost:42069`

Ezeket a következő osztályokban tudod módosítani:

- `server` (port, DB név)
- `App` (host/port kliens oldalon)

## Hibakezelés

A bejelentkezés és regisztráció hibakódjainak szöveges megfeleltetése:

- `src/main/resources/errorcode.json`

Példák:

- Login: `3` -> `Invalid username or password`
- Registration: `3` -> `Username already exists`

## Fejlesztői tippek

- A backend kéréskezelés bővítéséhez:
  1. Hozz létre egy új `Callable<JsonObject>` műveleti osztályt a `backend` csomagban.
  2. Regisztráld az `action`-t a `RequestProcessor` `switch` ágában.
  3. Készíts hozzá kliens oldali kérést és válaszkezelést a megfelelő `pages` komponensben.

- A kliensoldali oldalváltás a JavaFX `Scene` cseréjével történik.
- A szerver oldali többklienses működés alapja: `ClientHandler` + szálankénti kapcsolatkezelés.

## Gyakori problémák

1. `java.lang.UnsupportedClassVersionError`
   - Ok: nem JDK 23 fut.
   - Megoldás: telepíts és állíts be Java 23-at.

2. Kliens nem tud csatlakozni (`Connection refused`)
   - Ok: a backend nem fut, vagy rossz host/port van megadva.
   - Megoldás: indítsd el a szervert, ellenőrizd a `localhost:42069` címet.

3. JavaFX futtatási hiba Mavenből
   - Ok: a JavaFX plugin `mainClass` beállítása nincs összhangban az aktuális belépési ponttal.
   - Megoldás: futtasd IDE-ből az `App` osztályt, vagy frissítsd a `pom.xml` JavaFX plugin beállítását.

---

Ha szeretnéd, a következő körben készítek hozzá:

- rövid API referencia táblát kötelező/optional JSON mezőkkel,
- fejlesztői CONTRIBUTING szekciót,
- és egy Docker alapú futtatási leírást is.
