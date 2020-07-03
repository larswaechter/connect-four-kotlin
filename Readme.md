# connect-four-kotlin

Eine Vier-Gewinnt Implementierung in Kotlin mittels des Javalin Frameworks.

## Allgemeines

- Name: Lars Wächter
- Matrikel-Nr: 5280456
- Hochschule: Technische Hochschule Mittelhessen
- Studiengang: Bachelor of Science - Informatik
- Modul: Programmierung interaktiver Systeme (CS1016)
- Semester: SS2020 (Herzberg)
- Zweck: Prüfungsleistung

## Anleitung

## Dateiübersicht und Lines-Of-Code

Die folgende Grafik spiegelt die Ordnerstruktur des Projektes wider.
Im Anschluss gehe ich tiefer auf die einzelnen Dateien und deren Rollen ein.

```
connect-four-kotlin (root)
└───src
    └───main
        └───kotlin/connect/four
        │   │   App.kt
        │   │   ConnectFour.kt
        │   │   Lobby.kt
        │   │   Minimax.kt
        │   │   Move.kt
        │   │   Server.kt
        │
        └───resources
        │   └───public
        │   │   └───assets
        │   │   │   │   index.css
        │   │   │   │   index.js
        │   │   │   index.html
        │   └───transposition_tables
        │   │   │   00_table_1_3.txt
        │   │   │   01_table_4_6.txt
        │   │   │   ...
        │   │   │   13_table_40_42.txt
```

---

### Dateien in `src/main/kotlin/connect/four`

#### App.kt

LOC (2000)

Diese Datei dient als Start- bzw. Einstiegspunkt der Anwendung.
Hier wird lediglich eine neue Instanz der `Server.kt` Klasse erstellt.

#### ConnectFour.kt

LOC (2000)

djaliwdjila wjldiajw lidawj lidawj ldiawjlia wjdliawj dilaj dilawjd liaw jdaiw

#### Lobby.kt

LOC (2000)

awdawdawkdo akwdp awkdp akwdpo akwpd

#### Minimax.kt

LOC (2000)

jsefil jself jseilfj selifj sliefj seli

#### Move.kt

LOC (2000)

jseifj gjdrö gjdöig f kseöof söoef kosöe fkoösef k

#### Server.kt

LOC (2000)

jwdawdawdajop awopd jawpd joapwdj opwa

---

### Dateien in `src/main/resources/public`

#### assets/index.css

LOC (2000)

awdawdawd

#### assets/index.js

LOC (2000)

ajawildjawidwa

#### index.html

LOC (2000)

ajwialwdjlawj dlawi

---

### Dateien in `src/main/resources/transposition_tables`

Dieser Ordner beinhaltet mehrere sogenannte transposition tables.

---

## Engine

Folgender Abschnitt behandelt die im Projekt umgesetzte Spiel-Engine. Dies beinhaltet
unter anderem die Wahl des AI Algorithmus zur Berechnung des bestmöglichen Zuges,
dessen Performance-Optimierung und die Bewertung einer Spielsituation mittels der Monte-Carlo-Methode.

### Wahl des Algorithmus

Zur berechnen des bestmöglichen nächsten Zuges wird der [Minimax-Algorithmus](https://de.wikipedia.org/wiki/Minimax-Algorithmus) verwendet.
Dieser geht je nach Schwierigkeitsstufe bis zur einer Tiefe von 10.

### Wiederverwendung von berechneten Stellungswerten

Wie viele andere Spiele auch, beinhaltet Vier-Gewinnt einige Symmetrien in dessen Spielbrett,
welche jeweils immer zu dem selben Ergebnis führen.

Im Folgenden gehe ich genauer auf die verschieden Arten der Spielbrett-Symmetrien ein und wie diese
im Code implementiert sind. Insgesamt gibt es drei Symmetrien:

#### Arten von Symmetrien

##### 1. Spiegelung an der mittleren Y-Achse

Bei dieser Symmetrie wird das Spielbrett an der mittleren Y-Achse (Spalte #4) gespiegelt.
Hierbei werden die Spielsteine wie folgt getauscht:

- Spalte 1 <-> 7
- Spalte 2 <-> 6
- Spalte 3 <-> 5
- Spalte 4 <-> 4 (unverändert)

Hinweis: In der Implementierung im Code beginnen die Spalten bei 0 und gehen bis 6.

##### 2. Invertierung des Spielboards

Hierbei werden die einzelnen gesetzten Steine der Spieler invertiert.

- Steine -1 <-> 1
- Steine 1 <-> -1

##### 3. Spiegelung an der mittleren Y-Achse und invertierung des Spielboards

Diese Symmetrie ist eine Kombination aus den ersten beiden.

#### Implementierung der Symmetrien

##### Beschreibung

Die verschiedenen Symmetrien wurden mittels einer Art von Schlüssel-System implementiert.
Pro Board-Stellung gibt es vier verschiedene Schlüssel, welche aus dem `contentDeepHashCode` des Boards nach
Anwendung der jeweiligen Symmetrie erzeugt werden.

Der erste Schlüssel ist der `storageRecordPrimaryKey`. Dieser repräsentiert den `contentDeepHashCode`
der aktuellen Board-Stellung ohne jegliche angewandte Symmetrie. Unter diesem Schlüssel werden
berechnete Board-Stellungen in den HashMaps bzw. in den Transposition-Tables gespeichert.
(Mehr dazu im Abschnitt "Verwendung einer Datenbank mit Stellungswerten")

Die restlichen drei Schlüssel werden anhand der jeweiligen Symmetrien berechnet

- Zweiter Schlüssel: 1. Symmetrie (Spiegelung) und `contentDeepHashCode`
- Dritter Schlüssel: 2. Symmetrie (Invertierung) und `contentDeepHashCode`
- Vierter Schlüssel: 3. Symmetrie (Spiegelung & Invertierung) und `contentDeepHashCode`

Die berechneten Schlüssel werden innerhalb des Minimax-Algorithmus verwendet, um zu überprüfen, ob bereits
ein Eintrag unter einem der jeweiligen Schlüssel in der Datenbank bzw. im Speicher vorliegt. Ist ein Schlüssel vorhanden,
können wir den Eintrag aus dem Speicher lesen und weiterverarbeiten.

Im Code werden alle möglichen Schlüssel eines Boards mittels der Methode `getStorageRecordKeys` erzeugt und in einer Liste zurückgegeben.
Zu jedem Schlüssel gibt es zusätzlich noch eine Processing-Methode, welche benötigt wird, um den Speicher-Eintrag zu verarbeiten.

##### Processing-Methode

Die Processing Methode wird benötigt, da nicht ohne Weiteres ein aus dem Speicher geladener Eintrag
wieder verwendet werden darf. Je nach Schlüssel bzw. Symmetrie gibt es verschiedene Kritieren, die erfüllt sein müssen,
damit ein Eintrag aus dem Speicher verwendet werden darf.

Die Processing-Methode dient also dazu, um einen Eintrag auf die jeweiligen Kritieren zu überprüfen.

- Erster Schlüssel (`storageRecordPrimaryKey`):
  - Wurde ein Eintrag unter diesem Schlüssel gefunden, darf der Eintrag nur verwendet werden,
  wenn der aktuelle Spieler dem des Spielers im Eintrag entspricht
- Zweiter Schlüssel:
  - Wurde ein Eintrag unter diesem Schlüssel gefunden, darf der Eintrag nur verwendet werden,
  wenn der aktuelle Spieler dem des Spielers im Eintrag entspricht
  - Da bei diesem Schlüssel die Symmetrie der Spiegelung verwendet wurde, muss ebenfalls der im Eintrag gespeicherte Move gespiegelt werden
- Dritter Schlüssel:
  - Wurde ein Eintrag unter diesem Schlüssel gefunden, darf der Eintrag nur verwendet werden,
  wenn der aktuelle Spieler NICHT dem des Spielers im Eintrag entspricht, da die Steine invertiert wurden
  - Zusätlich muss der Score des Eintrags invertiert werden
- Vierter Schlüssel:
  - Wurde ein Eintrag unter diesem Schlüssel gefunden, darf der Eintrag nur verwendet werden,
  wenn der aktuelle Spieler NICHT dem des Spielers im Eintrag entspricht, da die Steine invertiert wurden
  - Zusätlich muss der Score des Eintrags invertiert werden
  - Da bei diesem Schlüssel die Symmetrie der Spiegelung verwendet wurde, muss ebenfalls der im Eintrag gespeicherte Move gespiegelt werden

Ein Schlüssel und dessen Processing-Methode werden im Code als `Pair<>` repräsentiert.
Der `first` Value entspricht dem Schlüssel und der `second` Value beinhaltet die Processing-Methode.

Um einen Eintrag im Speicher auf die Kritieren eines Schlüssels zu überprüfen,
wird dieser als Argument beim Aufruf der Processing Methode mitgegeben.



Sind alle Kritieren für einen Schlüssel erfüllt, gibt die Processing Methode eine neue Instanz der Klasse `Minimax.Storage.Record`
mit angepassten Werten zurück und Minimax returned diese Instanz. Sind die Kritieren nicht erfüllt, wird `null` zurückgegeben worauf der ursprünglich im Speicher gefundene Eintrag verworfen
und der nächste Schlüssel innerhalb von Minimax geprüft wird.

##### Ablauf einer Schlüsselüberprüfung

Folgende Skizze soll nochmal den Ablauf einer solchen Schlüsselüberprüfung verdeutlichen:

1. Zu Beginn des Minimax-Algorithmus werden alle Schlüssel für das aktuelle Board mittels der Funktion `getStorageRecordKeys` generiert und geladen
2. Nun wird über die einzelnen Schlüssel iteriert und geprüft ob ein Schlüssel im Speicher vorhanden ist
3. Ist ein Schlüssel vorhanden, wird dessen verknüpfter Speichereintrag (`Storage.Record`) geladen
4. Anschließend wird die Processing-Methode dieses Schlüssels mit dem verknüpften Speichereintrag aufgerufen
5. Gibt die Processing-Methode `null` zurück, wird der nächste Schlüssel in der Schleife überprüft, ansonsten wird der neu erhaltene `Storage.Record` zurückgegeben

### Stellungsbewertung bei imperfektem Spiel

Monte-Carlo-Methode

### Verwendung einer Datenbank mit Stellungswerten


## Testing

## GUI

## Quellen