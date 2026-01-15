# Tour de App – Think Different Academy (verze 14.6)

## Tým
- **Název týmu:** Poleno
- **Členové a role:**
  - Matyáš Neckář – backend & frontend (full‑stack)

## Použité technologie
- Java 17
- Spring Boot 3.2.1
- Maven
- H2 Database (file‑based, perzistentní mezi restartem)
- HTML/CSS/JS (bez frameworku; responzivní layout)

## Jak aplikaci spustit
### Požadavky
- JDK 17+
- Maven

### Spuštění (lokálně)
1. V kořeni projektu:
   - Windows: spusťte `start.bat` (zavolá `mvn spring-boot:run`)
   - Alternativně: `mvn spring-boot:run`
2. Aplikace poběží na `http://localhost:8080/`.

### Doba spuštění / nasazení
- **Lokální spuštění:**
  - První spuštění (stažení Maven závislostí): typicky 1–3 minuty dle připojení.
  - Další spuštění: obvykle 10–30 sekund.
- **Nasazení v hodnoticím systému:** dle zadání probíhá z commitu a trvá řádově minuty (doporučení je odevzdat s předstihem).

## Přihlašovací údaje (lektor)
- **Uživatelské jméno:** `lecturer`
- **Heslo:** `TdA26!`

## Funkce pro manuální testování
### Student
- Seznam kurzů: `/courses.html`
- Detail kurzu: otevřete kurz a zobrazí se:
  - Materiály (soubor / odkaz), řazeno od nejnovějších
  - Aktivity / kvízy (pokud existují)
  - Informační kanál (feed) s živými aktualizacemi přes SSE

### Lektor
- Přihlášení: `/loginLec.html`
- Správa kurzu (jedna obrazovka): `/manageLec.html`
  - Nejprve vyberete kurz, který chcete spravovat
  - Poté můžete nahrávat z jedné tabulky / formulářů:
    - Materiál (soubor)
    - Odkaz
    - Aktivitu / kvíz
    - Příspěvky do informačního kanálu (editace / mazání)

### Test SSE (reálný čas)
1. Otevřete detail stejného kurzu ve dvou oknech (např. student a lektor).
2. V lektorské části přidejte příspěvek do informačního kanálu.
3. Na detailu kurzu se příspěvek zobrazí bez obnovování stránky.

## Databáze
- H2 file DB je nastavena v `src/main/resources/application.properties`.
- Soubor databáze se ukládá do `./uploads/feeddb.mv.db` (vytvoří se automaticky po prvním spuštění).

## Responzivita a brand
- UI je responzivní a je určeno pro testování v Chrome (desktop i mobilní šířky).
- Styl je v `src/main/resources/static/style.css`.
- Logo je v `src/main/resources/static/images/logo.png`.
