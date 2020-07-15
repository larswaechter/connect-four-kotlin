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

Die Spielregeln entsprechen dem des klassischen Vier-Gewinnt: Ziel ist es, vier Steine seiner eigenen Farbe in einer Reihe (horizontal, vertikal oder diagonal) zu platzieren. Erreicht dies kein Spieler bis alle Züge (42) gespielt wurden, endet das Spiel Unentschieden.

## Dateiübersicht und Lines-Of-Code

Die folgende Veranschaulichung spiegelt die Ordnerstruktur des Projektes wider. Im Anschluss darauf wird genauer auf die einzelnen Dateien und deren Rollen eingegangen.

```
connect-four-kotlin (root)
└───src
    └───main
        └───kotlin/connect/four
        │   │   App.kt
        │   │   ConnectFour.kt
        │   │   Minimax.kt
        │   │   Move.kt
        │   │   Server.kt
        │   │   Tests.kt
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

| Dateiname      | Typ       | Rolle                                                    | LOC  |
| -------------- | --------- | -------------------------------------------------------- | ---- |
| App.kt         | Klasse    | Einstiegspunkt der Anwendung                             |      |
| ConnectFour.kt | Klasse    | Spiel-Logik, Darstellung des Spiels                      |      |
| Minimax.kt     | Interface | KI-Engine, Datenbankmanagement                           |      |
| Move.kt        | Klasse    | Repräsentiert einen Zug                                  |      |
| Server.kt      | Klasse    | Steuert ein- und ausgehende HTTP Requests bzw. Responses |      |
| Tests.kt       | Klasse    | Durchführung der fünf Testszenarien                      |      |

---

### Dateien in `src/main/resources/public`

| Dateiname  | Rolle                                                   | LOC  |
| ---------- | ------------------------------------------------------- | ---- |
| index.css  | Cascading Style Sheets für User-Interface               |      |
| index.js   | JavaScript zur Kommunikation zwischen Client und Server |      |
| index.html | User-Interface                                          |      |

---

### Dateien in `src/main/resources/transposition_tables`

Dieser Ordner beinhaltet die einzelnen Transposition Tables, welche als Datenbank dienen.

---

## Engine

Folgender Abschnitt behandelt die im Projekt umgesetzte Spiel-Engine. Dies beinhaltet unter anderem die Wahl des AI Algorithmus zur Berechnung des bestmöglichen Zuges, dessen Performance-Optimierung, als auch die Bewertung einer Spielsituation mittels der Monte-Carlo-Methode.

### Wahl des Algorithmus

Zur Berechnung des bestmöglichen nächsten Zuges, wird der Minimax-Algorithmus verwendet. Dieser rechnet bis zu einer Tiefer von 5.

### Wiederverwendung von berechneten Stellungswerten

Wie viele andere Spiele auch, beinhaltet Vier-Gewinnt einige Symmetrien in dessen Spielbrett,
welche jeweils zu dem selben Ausgangsergebnis führen. Die Verwendung solcher Symmetrien können die Anzahl der zu berechnenden Boards reduzieren und somit einen großen Einfluss auf die Laufzeit des Minimax-Algorithmus haben.

Im folgenden Abschnitt wird genauer auf die verschieden Arten der Spielbrett-Symmetrien eingegangen und wie diese im Code implementiert sind. Insgesamt gibt es drei Symmetrien.

#### Arten von Symmetrien

##### 1. Spiegelung an der mittleren Y-Achse

Bei dieser Symmetrie wird das Spielbrett an der mittleren Y-Achse (Spalte #4) gespiegelt.
Hierbei werden die Spielsteine wie folgt getauscht:

- Spalte 1 <=> 7
- Spalte 2 <=> 6
- Spalte 3 <=> 5
- Spalte 4 <=> 4 (unverändert)

**Hinweis:** In der Implementierung im Code beginnen die Spalten bei 0 und gehen bis 6.

Beispiel:

```
Board #1 (best move = 2)

. . . . . . .
. . . . . . .
. . . . . . .
. X . . . . .
. X . O O . .
. X O X O . .

Board #2 (Board #1 gespiegelt) (best move = 6)

. . . . . . .
. . . . . . .
. . . . . . .
. . . . . X .
. . O O . X .
. . O X O X .
```

Ausgehend von einer Board-Stellung wie in `Board #1`, wäre für Spieler X der bestmögliche Zug, einen Stein in Spalte 2 zu werfen. Er hätte damit gewonnen.

Ausgehend von einer Board-Stellung wie in `Board #2`, wäre für Spieler X der bestmögliche Zug, einen Stein in Spalte 6 zu werfen. Er hätte damit ebenfalls gewonnen.

Hierbei ist zu erkennen, dass beide Board-Stellungen zu demselben Ergebnis führen: Spieler X gewinnt.

Hat man nun beispielsweise den bestmöglichen Zug für `Board #1` bereits berechnet und im Speicher vorliegen, kann man im Falle von `Board #2` das Board spiegeln, wodurch man `Board #1` erhält, und den bestmöglichen Zug von `Board #1` aus dem Speicher lesen und übernehmen. Es gilt zu beachten, dass dieser Zug dann ebenfalls gespiegelt werden muss. Aus dem Zug `2` wird also `6`.

Wichtig hierbei ist, dass dies nur gilt, wenn man die Board-Stellung in beiden Situationen aus der Sicht desselben Spielers (X) betrachtet. Für Spieler O wären die eben genannten Züge nicht die bestmöglichen.

**Hinweis:** Maßnahmen zur Anpassung eines aus dem Speicher geladenen Zugs und wann diese verwendet werden dürfen, werden im Abschnitt "Processing-Methode" genauer behandelt.

##### 2. Invertierung des Spielboards

Hierbei werden die einzelnen gesetzten Steine der Spieler invertiert.

- Steine X <=> O
- Steine O <=> X

Beispiel:

```
Board #1 (best move = 2)

. . . . . . .
. . . . . . .
. . . . . . .
. X . . . . .
. X . O O . .
. X O X O . .

Board #2 (Board #1 invertiert) (best move = 2, allerdings für Gegenspieler)

. . . . . . .
. . . . . . .
. . . . . . .
. O . . . . .
. O . X X . .
. O X O X . .
```

Ausgehend von einer Board-Stellung wie in `Board #1`, wäre für Spieler X der bestmögliche Zug, einen Stein in Spalte 2 zu werfen. Er hätte damit gewonnen.

Ausgehend von einer Board-Stellung wie in `Board #2`, wäre für Spieler O der bestmögliche Zug, ebenfalls einen Stein in Spalte 2 zu werfen. Er hätte damit ebenfalls gewonnen.

Hat man nun beispielsweise den bestmöglichen Zug für `Board #1` bereits berechnet und im Speicher vorliegen, kann man im Falle von `Board #2` das Board invertieren, wodurch man `Board #1` erhält, und den bestmöglichen Zug von `Board #1` aus dem Speicher lesen und übernehmen.

Wichtig hierbei ist, dass dies nur gilt, wenn man die Board-Stellung in `Board #2` aus der Sicht des anderen Spielers als in `Board #1` betrachtet. Im Falle von `Board #2` also als Spieler O, da in `Board #1` das Board als Spieler X betrachtet wurde.

##### 3. Spiegelung an der mittleren Y-Achse und Invertierung des Spielboards

Diese Symmetrie ist eine Kombination aus den ersten beiden. Zuerst wird das Board gespiegelt und anschließend invertiert.

Beispiel:

```
Board #1 (best move = 2)

. . . . . . .
. . . . . . .
. . . . . . .
. X . . . . .
. X . O O . .
. X O X O . .

Board #2 (Board #1 gespiegelt und invertiert) (best move = 6, allerdings für Gegenspieler)

. . . . . . .
. . . . . . .
. . . . . . .
. . . . . O .
. . X X . O .
. . X O X O .
```

#### Implementierung der Symmetrien

##### Beschreibung

Die verschiedenen Symmetrien wurden mittels einer Art Schlüssel-System implementiert. Für jede mögliche Board-Stellung gibt es vier dazugehörige Schlüssel, welche aus dem Zobrist-Hash des Boards nach Anwendung der jeweiligen Symmetrie erzeugt werden.

##### Schlüssel Arten

Der erste Schlüssel ist der `storageRecordPrimaryKey`. Dieser repräsentiert den reinen Zobrist-Hash der aktuellen Board-Stellung ohne jegliche angewandte Symmetrie. Unter diesem Schlüssel werden berechnete Board-Stellungen in den Transposition-Tables gespeichert.

**Hinweis**: Die Berechnung des Zobrist-Hashs und die Speicherung von Board-Stellungen werden
im Abschnitt "Verwendung einer Datenbank mit Stellungswerten" genauer behandelt.

Die restlichen drei Schlüssel werden mittels des Zobrist-Hashs nach Anwendung einer Symmetrie auf das Board berechnet:

- Zweiter Schlüssel: 1. Symmetrie (Spiegelung) + Berechnung des Hashs
- Dritter Schlüssel: 2. Symmetrie (Invertierung) + Berechnung des Hashs 
- Vierter Schlüssel: 3. Symmetrie (Spiegelung & Invertierung) + Berechnung des Hashs

Die berechneten Schlüssel werden innerhalb des Minimax-Algorithmus verwendet, um zu überprüfen, ob bereits ein Eintrag unter einem der jeweiligen Schlüssel im Speicher vorliegt. Ist ein Schlüssel vorhanden,
wird der dazugehörige Eintrag aus dem Speicher gelesen und weiterverarbeitet.

Im Code werden alle möglichen Schlüssel eines Boards mit Hilfe der Methode `ConnectFour.getStorageRecordKeys` erzeugt und in einer Liste zurückgegeben. Zu jedem Schlüssel gibt es zusätzlich noch eine Processing-Methode, welche benötigt wird, um einen Speicher-Eintrag zu verarbeiten.

##### Processing-Methode

Die Processing Methode wird benötigt, da nicht ohne Weiteres ein aus dem Speicher geladener Eintrag
verwendet werden darf. Je nach Schlüssel bzw. Symmetrie gibt es verschiedene Kritieren, die erfüllt sein müssen, damit ein Eintrag aus dem Speicher verwendet werden darf.

Die Processing-Methode dient also dazu, um einen Eintrag auf die jeweiligen Kritieren zu überprüfen.

- Erster Schlüssel (`storageRecordPrimaryKey`):
  - Wurde ein Eintrag unter diesem Schlüssel gefunden, darf der Eintrag nur verwendet werden,
  wenn der aktuelle Spieler dem des Spielers im Eintrag entspricht
- Zweiter Schlüssel (Spiegelung):
  - Wurde ein Eintrag unter diesem Schlüssel gefunden, darf der Eintrag nur verwendet werden,
  wenn der aktuelle Spieler dem des Spielers im Eintrag entspricht
  - Da bei diesem Schlüssel die Symmetrie der Spiegelung verwendet wurde, muss ebenfalls der im Eintrag gespeicherte Move gespiegelt werden
- Dritter Schlüssel (Invertierung):
  - Wurde ein Eintrag unter diesem Schlüssel gefunden, darf der Eintrag nur verwendet werden,
  wenn der aktuelle Spieler NICHT dem des Spielers im Eintrag entspricht, da die Steine invertiert wurden
  - Zusätzlich muss der Score des Eintrags invertiert werden
- Vierter Schlüssel (Spiegelung & Invertiertung):
  - Wurde ein Eintrag unter diesem Schlüssel gefunden, darf der Eintrag nur verwendet werden,
  wenn der aktuelle Spieler NICHT dem des Spielers im Eintrag entspricht, da die Steine invertiert wurden
  - Da bei diesem Schlüssel die Symmetrie der Spiegelung verwendet wurde, muss ebenfalls der im Eintrag gespeicherte Move gespiegelt werden
  - Zusätzlich muss der Score des Eintrags invertiert werden

Ein Schlüssel und dessen Processing-Methode werden im Code als `Pair<>` repräsentiert. Der `first` Value entspricht dem Schlüssel und der `second` Value beinhaltet die Processing-Methode.

Um einen Eintrag im Speicher auf die Kritieren eines Schlüssels zu überprüfen, wird dieser als Argument beim Aufruf der Processing Methode mit übergeben.

Sind alle Kritieren für einen Schlüssel erfüllt, gibt die Processing Methode eine neue Instanz der Klasse `Minimax.Storage.Record` mit angepassten Werten zurück und Minimax führt ein `return` dieser Instanz durch. Sind die Kritieren nicht erfüllt, wird `null` von der Methode zurückgegeben, worauf der ursprünglich im Speicher gefundene Eintrag verworfen und der nächste Schlüssel innerhalb von Minimax geprüft wird.

### Stellungsbewertung bei imperfektem Spiel

Damit der Minimax-Algorithmus ein Board aus der Sicht eines beliebigen Spielers bewerten kann, ist eine `evaluate`-Methode notwendig. Im Projekt wurde eine solche Evaluierung mittels der **Monte-Carlo-Methode** umgesetzt.

Hierbei wird ausgehend von einer gegebenen Stellung abwechselnd für jeden Spieler ein zufälliger Zug ausgeführt, bis das schließlich Spiel beendet ist (keine Züge mehr möglich oder Sieg eines Spielers). Dieses Vorgehen wird eine gewünschte Anzahl, im Projet 200, Mal wiederholt.

Anhand der Anzahl der Gewinne für einen gegebenen Spieler wird ein Score ermittelt, welcher als Evaluations-Wert für das aktuelle Board dient.

Je höher dieser Wert für den Maximizer bzw. umso niedriger er für den Minimizer ist, desto
besser ist der Score und dementsprechend auch der Zug, der zu der gegebenen Ausgangsstellung führte.

### Verwendung einer Datenbank mit Stellungswerten

Eine weitere Performance-Optimierung ist das Anlegen einer Datenbank, auch Transposition Tables genannt, bestehend aus bereits evaluierten Boards und deren bestmöglichen Züge. Eine Datenbank ermöglicht, dass innerhalb des Minimax-Algorithmus nicht jede mögliche Board-Stellung neu evaluiert werden muss, da manche bereits in der Datenbank vorhanden sind und dort ausgelesen werden können.

Anknüpfend wird die Realisierung einer solchen Datenbank beschrieben.
#### Verzeichnisstruktur der Datenbanken

Die komplette Datenbank besteht aus 14 einzelnen Transposition Tables, welche als Text-Dateien umgesetzt sind.

Der Name einer solchen Transposition Table ergibt sich wie folgt:

`#index#_table_#playedMovesFrom#_#playedMovesTo#.txt`

- **index**: Index der Transposition Table (aufsteigende Zahlen)
- **playedMovesFrom**:
- **playedMovesTo**:

In welche Transposition Table ein Eintrag bzw. Spiel-Board geschrieben wird, hängt davon ab, wie viele Züge bisher in dem Spiel gespielt wurden.

#### Aufbau einer Datenbank

Eine Transposition Table besteht aus mehreren Eintägen nach folgendem Schema:

`#StorageRecordPrimaryKey# #Move# #Score# #Player#`

- **StorageRecordPrimaryKey**: der Zobrist-Hash des Boards
- **Move**: der bestmögliche Zug in der jeweiligen Situation
- **Score**: die Bewertung des Zugs
- **Player**: der durchführende Spieler

#### Seeding - Befüllung der Datenbanken

##### Beschreibung

Da die Transposition Tables basierend auf der Anzahl an gespielten Zügen voneinander getrennt sind,
ist es möglich, gezielt einzelne dieser zu befüllen.

Im Code ist dies mittels der Methode `Minimax.Storage.seedByMovesPlayed` möglich. Diese erwartet zwei Parameter:

- `amount: Int` - Anzahl an Datensätzen, die erstellt werden sollen
- `movesPlayed: Int` - Anzahl an gespielten Zügen

Der Algorithmus generiert also `amount` viele Datensätze bestehend aus Spielen mit `movesPlayed` gespielten Zügen.

##### Ablauf einer Befüllung

1. Anlegen einer neuen leeren HashMap
2. Erstellung eines Boards mit `movesPlayed` gespielten Zügen
3. Überprüfung, ob Board bereits im Speicher vorhanden ist
4. Berechung des bestmöglichen des Zugs für das Board mittels Minimax-Algorithmus
5. Das Ergebnis in die neu angelegte HashMap hinzufügen

Diese Prozedur wird genau `amount` Mal wiederholt. Wurden diese Wiederholungen alle abgearbeitet,
wird die zuvor neu angelegte HashMap persistent in den jeweiligen Speicher geschrieben.

Ist eine Board-Stellung bereits im Speicher vorhanden, wird dies im 3. Schritt erkannt und das Board wird übersprungen.

Um ein Board mit einer genauen Anzahl an gespielten Zügen zu generieren, wird die Methode `ConnectFour.playRandomMoves` verwendet.

Es ergibt sich als effizient, wenn man bei der Befüllung der Datenbanken erst Datensätze mit möglichst vielen gespielten Zügen generiert und diese Schrittweise reduziert. Hierdurch kann der Minimax-Algorithmus auf bereits vorhandene Einträge im Speicher zurückgreifen.


### Programmierstil

#### Immutabilität

#### Interface

#### Bitboards

## Testing

## GUI

## Quellen