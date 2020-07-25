# Vier Gewinnt (PiS, SoSe 2020)

Autor: Lars Wächter, 5280456

Ich habe die Zulassung für PiS im SoSe 2020 bei Herrn Herzberg erhalten.

## Einleitung

### Spielregeln

Die Spielregeln entsprechen dem des klassischen Vier-Gewinnts: Ziel ist es, vier Steine seiner eigenen Farbe in einer Reihe (horizontal, vertikal oder diagonal) zu platzieren. Die Spieler wechseln sich dabei gegenseitig ab. Erreicht dies kein Spieler bis alle Züge (42) gespielt wurden, endet das Spiel Unentschieden.

### Bedienungsanleitung

...

### Dateiübersicht

...

## Spiel-Engine (ENG)

| Feature    | M    | H + S | MC   | eD   | B+I  | Summe    |
| ---------- | ---- | ----- | ---- | ---- | ---- | -------- |
| Umsetzung  | 100  | 100   | 100  | 130  | 66.6 |          |
| Gewichtung | 0.4  | 0.3   | 0.3  | 0.3  | 0.3  |          |
| Ergebnis   | 40   | 30    | 30   | 39   | 20   | **159%** |

Folgender Abschnitt behandelt die Implementierung der Spiel-Engine sowie Besonderheiten im Code.

### KI-Algorithmus

*Im Code*

- `Minimax.minimax()`

---

Zur Berechnung des bestmöglichen Zugs, wird der Minimax-Algorithmus verwendet. Dieser rechnet bis zu einer Tiefe von fünf. Mittels des Parameters `maximize` wird gesteuert, ob man sich gerade in der Rolle des Maximizers oder Minimizers befindet.

Der Algorithmus enthält zwei zusätzliche Performance-Optimierungen:

**Eine Überprüfung, ob der vorheriger Spieler gewonnen hat:**

Eine Abbruchbedingung des Minimax-Algorithmus ist, wenn einer der beiden Spieler das Spiel gewonnen hat. Da der Zug eines Spielers nicht zum Sieg des anderen Spielers führen kann, muss man innerhalb des Algorithmus nicht jedes Mal auf den Sieg beider Spieler prüfen. Es reicht zu überprüfen, ob der vorherige Spieler das Spiel gewonnen hat, da dieser den letzten Zug gespielt hat. Für den aktuellen Spieler spielt der Zug des vorherigen Spielers keine Rolle, weshalb dieser nicht als Sieger geprüft werden muss.

**Eine Überprüfung, ob man mit einem der möglichen Züge sofort gewinnen kann:**

Bevor innerhalb des Minimax-Algorithmus alle möglichen Züge bewerten werden, wird einmal über die Liste aller möglichen Züge iteriert und geprüft, ob der aktuelle Spieler mit einem dieser Züge direkt gewinnen kann. Ist dies der Fall, spart man sich die Evaluierung der restlichen Züge.

### Datenbank

Eine weitere Performance-Optimierung ist das Anlegen einer Datenbank, auch Transposition Tables genannt, bestehend aus bereits evaluierten Boards und deren bestmöglichen Züge. Eine Datenbank ermöglicht, dass innerhalb des Minimax-Algorithmus nicht jede mögliche Board-Stellung neu evaluiert werden muss, da manche bereits in der Datenbank vorhanden sind und dort ausgelesen werden können.

Anknüpfend wird die Realisierung einer solchen Datenbank beschrieben.

#### Aufbau 

Die komplette Datenbank besteht aus sieben einzelnen Transposition Tables, welche als Text-Dateien umgesetzt sind. Sie beinhalten bereits berechnete Spielstellungen mit deren dazugehörigen bestmöglichen Zügen. Zu finden sind diese in `src/main/resources/transposition_tables`.

Der Dateiname einer solchen Transposition Table gibt an, wie viele Züge jeweils in dem Spiel gespielt wurden, dessen Evaluierung in die Tabelle eingetragen wurde. Die Tabelle `00_table_0_5.txt` beinhaltet somit beispielsweise nur Spielstellungen, bei denen zwischen 0 und 5 Zügen gespielt wurde. Tabelle `01_table_6_11.txt` besteht nur aus Spielen mit 6 bis 11 gespielten Zügen.

#### Transposition Table

*Im Code*

- `class Minimax.Storage` 
- `class Minimax.Storage.Record`

---

Eine Transposition Table beinhaltet Spielstellungen und deren Bewertungen für eine jeweils angegebene Anzahl an gespielten Spielzügen. Sie besteht aus mehreren Einträgen nach folgendem Schema:

`#Hash# #Move# #Score# #Player#`

- **Hash**: der Zobrist-Hash des Boards
- **Move**: der bestmögliche Zug in der jeweiligen Situation
- **Score**: die Bewertung des Zugs
- **Player**: der durchführende Spieler

Beispiel: `1369919444299124995 3 -5727.0 -1`

Ein solcher Eintrag einer Transposition Table wird mit Hilfe der Klasse `Minimax.Storage.Record` repräsentiert. Diese beinhaltet unter anderem die oben genannten Begriffe als Instanzvariablen.

Die einzelnen Transposition werden als Instanz der Klasse `Minimax.Storage` implementiert. Diese kümmert sich um das Lesen und Schreiben der dazugehörigen Textdatei. Außerdem beinhaltet sie eine `HashMap`, welche die Tabelleneinträge als Instanz der Klasse `Minimax.Storage.Record` umfasst.

#### Zobrist Hash

*Im Code*

- `class Minimax.Storage`

---

Der Zobrist Hash wird verwendet, um die verschiedenen Spielsituationen als Hashwert darzustellen. Wenn keine Kollisionen auftreten, sollte jede mögliche Spielstellung ihren eigenen einzigartigen Zobrist-Hash besitzen. Unter diesem Hashwert werden die Spielstellungen in die Datenbank gespeichert.

##### Vorbereitung

*Im Code*

- `Minimax.Storage.generateZobristHashes()` 
- `Minimax.Storage.readZobristHashes()`
- `Minimax.Storage.buildZobristTable()`

---

Um den Zobrist Hash einer Spielstellung zu berechnen, müssen zuerst für jede vorhandene Spielfeldzelle und für jeden möglichen Spieler innerhalb einer Zelle, zufällige Zahlen generiert werden (`generateZobristHashes`). Hierbei wurden Zahlen vom Typ `Long` (64 Bit) genommen. Diese dienen als "Schlüssel". Insgesamt werden 96 solcher Schlüssel erzeugt.

Die erzeugten Schlüssel werden persistent in einer Textdatei abgespeichert: `src/main/resources/transposition_tables/zobrist_hashes.txt`

Bei Programmstart werden die Schlüssel aus der Textdatei geladen und in ein 2D-Array hinterlegt. Dies passiert mittels der Methode `Minimax.Storage.buildZobristTable()`.

Folgende Tabelle soll den Aufbau nochmal verdeutlichen: Für jede Zelle des Spielbretts gibt es für beide Spieler eine zufällige Zahl (aus Platzgründen sind diese hier verkürzt dargestellt).

| Spielfeld Zelle | 0            | 1            | 2            | 3            | ...  |
| --------------- | ------------ | ------------ | ------------ | ------------ | ---- |
| Spieler 1       | ... 6389 ... | ... 2725 ... | ... 6402 ... | ... 3206 ... |      |
| Spieler -1      | ... 8625 ... | ... 2805 ... | ... 4394 ... | ... 1259 ... |      |

##### Berechnung

*Im Code*

- `ConnectFour.calcZobristHash()`

---

Nachdem eine Tabelle mit zufälligen (pseudo) Zahlen erstellt wurde, ist es möglich, den Zobrist-Hash für eine jeweilige Stellung zu berechnen. 

Hierfür wird für jeden gesetzten Stein beider Spieler der Wert der dazugehörigen Zelle aus der zuvor erzeugten Tabelle (siehe oben) entnommen und per `XOR` Operation miteinander verknüpft. Der dadurch entstandene Wert entspricht dem Zobrist-Hash der jeweiligen Stellung.

Ein großer Vorteil dieses Verfahrens ist, dass der Hash nicht nach jedem Spielzug komplett neu berechnet werden muss: 

Führt ein Spieler einen Zug aus, muss nur der Zobrist-Schlüssel für diesen Spieler und der Spielfeldzelle, in die der Stein gesetzt wurde, aus der Tabelle geladen und per `XOR` Operation mit dem Zobrist-Hash der aktuellen Spielstellung verknüpft werden.

### Symmetrien

Wie viele andere Spiele auch, beinhaltet Vier-Gewinnt Symmetrien in dessen Spielbrett. Diese entstehen beispielsweise durch Drehungen oder Spiegelungen des Spielbretts. Sie haben die Eigenschaft, dass sie jeweils zu dem selben Spielergebnis führen bzw. die selbe Evaluierung haben. Die Verwendung solcher Symmetrien kann die Anzahl der zu berechnenden Boards reduzieren und somit einen großen Einfluss auf die Laufzeit des KI-Algorithmus haben.

#### Arten von Symmetrien

Vier-Gewinnt besitzt insgesamt drei Symmetrien.

##### 1. Spiegelung an der mittleren Y-Achse

Bei dieser Symmetrie wird das Spielbrett an der mittleren Y-Achse (Spalte #4) gespiegelt. Hierbei werden die Spielsteine in den jeweiligen Spalten wie folgt getauscht:

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

Folgender Abschnitt thematisiert die Implementierung der Symmetrien sowie deren Anwendung innerhalb des Minimax-Algorithmus.

Die verschiedenen Symmetrien wurden mittels einer Art "Schlüssel-System" implementiert. Für jede mögliche Board-Stellung gibt es vier dazugehörige Schlüssel, welche aus dem Zobrist-Hash des Boards nach Anwendung der jeweiligen Symmetrie erzeugt werden.

#### 

##### Hashes

Der erste Hash ist der `storageRecordPrimaryKey`. Dieser repräsentiert den reinen Zobrist-Hash der aktuellen Board-Stellung ohne jegliche angewandte Symmetrie. Unter diesem Hash werden berechnete Board-Stellungen in den Transposition-Tables gespeichert.

**Hinweis**: Die Berechnung des Zobrist-Hashs und die Speicherung von Board-Stellungen werden im Abschnitt "Verwendung einer Datenbank mit Stellungswerten" genauer behandelt.

Die restlichen drei Hashs werden mittels des Zobrist-Hashs nach Anwendung einer Symmetrie auf das Board berechnet:

- Zweiter Hash: 1. Symmetrie (Spiegelung) + Berechnung des Hashs
- Dritter Hash: 2. Symmetrie (Invertierung) + Berechnung des Hashs 
- Vierter Hash: 3. Symmetrie (Spiegelung & Invertierung) + Berechnung des Hashs

Die berechneten Hashes werden innerhalb des Minimax-Algorithmus verwendet, um zu überprüfen, ob bereits ein Eintrag unter einem der jeweiligen Schlüssel im Speicher vorliegt. Ist ein Schlüssel vorhanden, wird der dazugehörige Eintrag aus dem Speicher gelesen und weiterverarbeitet.

Im Code werden alle möglichen Schlüssel eines Boards mit Hilfe der Methode `ConnectFour.getStorageRecordKeys` erzeugt und in einer Liste zurückgegeben. Zu jedem Schlüssel gibt es zusätzlich noch eine Processing-Methode, welche benötigt wird, um einen Speicher-Eintrag zu verarbeiten.

## Tests

| Szenario | 1    | 2    | 3    | 4    | 5    | Summe |
| -------- | ---- | ---- | ---- | ---- | ---- | ----- |
| ok       | X    | X    | X    | X    | -    | 0.8   |

Die Tests werden wie folgt ausgeführt:

...

Die Testausführung protokolliert sich über die Konsole wie folgt:

## Umsetzung der GUI

## Hinweise

## Quellennachweis