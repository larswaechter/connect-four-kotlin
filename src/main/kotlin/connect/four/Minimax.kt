package connect.four

import java.io.File
import kotlin.math.pow
import kotlin.random.Random

/**
 * Interface for implementing Minimax algorithm in two-player zero-sum games
 * including storage based on .txt files for already evaluated board positions
 *
 * @param [Board] the type of the game board
 * @param [Move] the type of a move
 */
interface Minimax<Board, Move> {

    /**
     * Storage class used to represent different transposition tables as storages.
     * Each instance holds its own transposition table.
     *
     * @param [index] storage index
     */
    class Storage<Move>(private val index: Int) {
        private val filename: String = getFilename(this.index)
        private var mapInitialized: Boolean = false
        var map: HashMap<Long, Record<Move>> = HashMap()

        companion object {
            private const val numberOfTranspositionTables = 14
            private const val maxTreeDepthTranspositionTables = 5
            private const val transpositionTablesPath = "src/main/resources/transposition_tables"
            private val zobristTable: Array<Array<Long>> = buildZobristTable()
            private val storages: Array<Storage<*>?> = Array(numberOfTranspositionTables) { null }

            /**
             * Register all storages.
             * You might want to do this on app start
             */
            fun <Move> registerStorages() {
                for (i in storages.indices) Storage<Move>(i).register()
            }

            /**
             * Get storage based on storage index
             * Always use this method to access a storage
             *
             * @param [index] storage index
             * @return Storage
             */
            fun <Move> doStorageLookup(index: Int): Storage<Move> {
                assert(index in 0 until numberOfTranspositionTables) { "Index is higher than number of transposition tables." }
                Storage<Move>(index).register()
                return storages[index]!! as Storage<Move>
            }

            /**
             * Seed transposition tables amount-times with boards of movesPlayed-moves.
             *
             * Make sure that we evaluate boards and their best moves only for the AI (player -1)
             * since the human player (player 1) always plays for himself.
             *
             * @param [amount] number of data records
             * @param [movesPlayed] number of played moves
             * */
            fun <Move> seedByMovesPlayed(amount: Int, movesPlayed: Int) {
                assert(movesPlayed <= 40)

                println("\nStart seeding storage for #$movesPlayed played moves...")

                val startTime = System.currentTimeMillis()
                val newHashMap: HashMap<Long, Record<Move>> = HashMap()

                var countNewRecords = 0
                var countIterations = 0

                var game: ConnectFour
                var storage: Storage<Move>

                outer@
                do {
                    game = ConnectFour.playRandomMoves(movesPlayed)
                    storage = doStorageLookup(game.storageIndex)

                    assert(game.getNumberOfRemainingMoves() > 0) { "No more moves left! Cannot calculate best move." }

                    countIterations++

                    // Check if board is already stored -> Skip it
                    for (storageRecordKey in game.getStorageRecordKeys()) {
                        val key = storageRecordKey()
                        if (storage.map.contains(key.first) || newHashMap.containsKey(key.first))
                            continue@outer
                    }

                    countNewRecords++

                    val storageRecord = game.minimax(currentDepth = maxTreeDepthTranspositionTables, seeding = true)
                    newHashMap[storageRecord.key!!] = storageRecord as Record<Move>

                } while (countIterations < amount)

                // Write to .txt file
                storage.appendMapToFile(newHashMap)

                val duration = (System.currentTimeMillis() - startTime) / 1000
                println("${storage.filename}: Added $countNewRecords / $amount new data records in ${duration}s.")
            }

            /**
             * Get table file name based on storage index
             *
             * @param [storageIndex] storage index
             * @return file name
             */
            fun getFilename(storageIndex: Int): String {
                assert(storageIndex < numberOfTranspositionTables) { "Index is higher than number of transposition tables." }
                val id = if (storageIndex < 10) "0$storageIndex" else "$storageIndex"
                val from = storageIndex * 3 + 1
                val to = from + 2
                return "${id}_table_${from}_${to}.txt"
            }

            /**
             * Get zobrist-hash for given player and position
             *
             * @param [cell] from 0 to 47
             * @param [player] player 1 or -1
             * @return zobrist-hash for given positions
             */
            fun getZobristHash(cell: Int, player: Int): Long = zobristTable[cell][if (player == 1) 0 else 1]

            /**
             * Generate random zobrist-hashes and write them to storage file.
             * Warning: If you do this, already existing transposition tables become invalid
             *
             * Create one hash for each board cell (47) and each player (2)
             * We have to create some more because the top board row is unused
             */
            private fun generateZobristHashes() {
                val file = File("$transpositionTablesPath/zobrist_hashes.txt")
                var res = ""

                for (i in 0..47)
                    for (k in 0..1)
                        res += "${Random.nextLong(2F.pow(64).toLong())}\n"

                file.writeText(res)
            }


            /**
             * Load zobrist table based on zobrist-hashes
             *
             * @return 3D array of keys for every board position and player
             */
            private fun buildZobristTable(): Array<Array<Long>> {
                val keys = readZobristHashes()
                val table = Array(48) { Array(2) { 0L } }

                var count = 0
                for (i in 0..47)
                    for (k in 0..1)
                        table[i][k] = keys[count++]

                return table
            }

            /**
             * Read zobrist-hashes from .txt file.
             * Create new ones if they don't exist yet.
             *
             * @return array of hashes
             */
            private fun readZobristHashes(): Array<Long> {
                val file = File("$transpositionTablesPath/zobrist_hashes.txt")
                val keys = Array<Long>(96) { 0 }

                if (file.readLines().isEmpty()) {
                    println("No Zobrist hashes found. Creating...")
                    generateZobristHashes()
                    return readZobristHashes()
                }

                var count = 0
                file.forEachLine {
                    keys[count++] = it.toLong()
                }

                assert(count == 96) { "There should be 96 zobrist-hashes" }

                return keys
            }
        }

        /**
         * Append new HashMap to storage .txt file (persistent) and update map instance
         *
         * @param [map] new HashMap to append
         */
        fun appendMapToFile(map: HashMap<Long, Record<Move>>) {
            val file = this.getFile()
            var res = ""

            map.forEach { (k, v) ->
                // Prevent duplicates
                if (!this.map.containsKey(k)) {
                    this.map[k] = v
                    res += "$v\n"
                }
            }

            file.appendText(res)
        }

        /**
         * Register storage
         * Skipped if it's already registered
         */
        fun register() {
            if (!this.isRegistered()) {
                println("Register storage #${this.index}...")
                this.initMap()
                storages[this.index] = this
            }
        }

        /**
         * Check if storage is registered
         *
         * @return whether storage is registered
         */
        private fun isRegistered(): Boolean = storages[this.index] != null

        /**
         * Get storage .txt file
         *
         * @return file
         */
        private fun getFile(): File = File("$transpositionTablesPath/${this.filename}")

        /**
         * Read HashMap from storage .txt file
         *
         * @return HashMap for played moves
         */
        private fun readMap(): HashMap<Long, Record<Move>> {
            val file = this.getFile()
            val map: HashMap<Long, Record<Move>> = HashMap()

            file.forEachLine {
                val storageRecord = Record.ofStorageRecordString<Move>(it)
                map[storageRecord.key!!] = storageRecord
            }

            return map
        }

        private fun initMap() {
            this.map = this.readMap()
            this.mapInitialized = true
        }

        /**
         * This class is used to represent records inside a storage.
         * - [toString] method is used to represent class in tables.
         *
         * @property [key] [storageRecordPrimaryKey] for given board. Used to load record from storage
         * @property [move] best move for given board
         * @property [score] evaluated score of move
         * @property [player] player who did the move
         */
        class Record<Move>(
                val key: Long?,
                val move: Move?,
                val score: Float,
                val player: Int) {

            companion object {
                /**
                 * Create instance from storage record entry
                 *
                 * @param [storageRecordString] storage record string
                 * @return instance of class
                 */
                fun <M> ofStorageRecordString(storageRecordString: String): Record<M> {
                    val elements = storageRecordString.split(" ")
                    return Record(elements[0].toLong(), Move.ofStorageEntry(elements[1]), elements[2].toFloat(), elements[3].toInt()) as Record<M>
                }
            }

            override fun toString(): String {
                return "$key $move $score $player"
            }
        }
    }

    /**
     * Game board
     */
    val board: Board

    /**
     * Current player
     */
    val currentPlayer: Int

    /**
     * Maximal minimax tree depth (AI difficulty)
     */
    val difficulty: Int

    /**
     * Storage index for current board.
     * Here, the index is based on the number of played moves.
     */
    val storageIndex: Int

    /**
     * Primary key of current board for storage record.
     * Board evaluations are stored under this key.
     */
    val storageRecordPrimaryKey: Long

    /**
     * Evaluate game state for current player:
     * - For player 1 (maximizer) a higher value is better.
     * - For player -1 (minimizer) a lower value is better.
     *
     * @return positive or negative float
     */
    fun evaluate(depth: Int): Float

    /**
     * Get list of all possible moves
     *
     * @param [shuffle] shuffle list
     * @return list of possible moves
     */
    fun getPossibleMoves(shuffle: Boolean = false): List<Move>

    /**
     * Get maximum number of remaining moves until [isGameOver] is true
     *
     * @return max number of remaining moves
     */
    fun getNumberOfRemainingMoves(): Int

    /**
     * Check if a player has won
     *
     * @param [player] set to 1 or -1 to check only if the given player has won
     * @return has a player won
     */
    fun hasWinner(player: Int = 0): Boolean

    /**
     * Check if a player has won or no more moves are possible
     *
     * @param [player] set to 1 or -1 to check only for the given player's win in [hasWinner]
     * @return is game over
     */
    fun isGameOver(player: Int = 0): Boolean

    /**
     * Do move and return new game
     *
     * @param [move] move to perform
     * @return new game with applied move
     */
    fun move(move: Move): Minimax<Board, Move>

    /**
     * Undo a number of moves
     *
     * @param [number] number of moves to undo
     * @return new game with undone moves
     */
    fun undoMove(number: Int): Minimax<Board, Move>

    /**
     * Pick random move from possible moves list
     *
     * @param [possibleMoves] possible moves to pick a random move from
     * @return a random move
     */
    fun getRandomMove(possibleMoves: List<Move> = this.getPossibleMoves()): Move = possibleMoves.random()

    /**
     * Search best move for current board and player in storage.
     * Including symmetries etc.
     *
     * @return best move if it exists in storage otherwise null
     */
    fun searchBestMoveInStorage(): Storage.Record<Move>?

    /**
     * Get all possible storage keys for the current board with according methods to transform the associated move.
     * - This might include symmetries or inverted boards for example
     * - Index 0 should be [storageRecordPrimaryKey]
     *
     * @return storage keys the board might be stored under
     */
    fun getStorageRecordKeys(): List<() -> Pair<Long, (record: Storage.Record<Move>) -> Storage.Record<Move>?>>

    /**
     * Minimax algorithm that finds best move
     *
     * @param [game] game to apply minimax on
     * @param [startingDepth] tree depth on start
     * @param [currentDepth] maximal tree depth (maximal number of moves to anticipate)
     * @param [maximize] maximize or minimize
     * @param [seeding] is algorithm used for storage seeding
     * @return storage record
     */
    fun minimax(
            game: Minimax<Board, Move> = this,
            startingDepth: Int = this.difficulty,
            currentDepth: Int = startingDepth,
            maximize: Boolean = game.currentPlayer == 1,
            seeding: Boolean = false
    ): Storage.Record<Move> {
        // Recursion anchor -> Evaluate board
        if (currentDepth == 0 || game.isGameOver(-game.currentPlayer))
            return Storage.Record(null, null, game.evaluate(currentDepth), game.currentPlayer)

        // Check if board exists in storage
        val storedMove = game.searchBestMoveInStorage()
        if (storedMove != null) return storedMove

        val possibleMoves = game.getPossibleMoves(true)

        /**
         * If there's a move which results in a win for the current player we immediately return this move.
         * This way we don't have to evaluate other possible moves -> Performance + 1 :)
         *
         * We add the currentDepth, otherwise the AI plays randomly if it will lose definitely.
         * This way the AI tries to "survive" as long as possible, even if it can't win anymore.
         * (if the opponent plays perfect)
         */
        possibleMoves.forEach { move ->
            val tmpGame = game.move(move)
            if (tmpGame.hasWinner(game.currentPlayer))
                return Storage.Record(
                        game.storageRecordPrimaryKey,
                        move,
                        game.currentPlayer * (1_000_000F + currentDepth),
                        game.currentPlayer
                )
        }

        // Call recursively from here on for each move to find best one
        var minOrMax: Pair<Move?, Float> = Pair(null, if (maximize) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY)

        for (move in possibleMoves) {
            val newGame = game.move(move)
            val moveScore = minimax(newGame, startingDepth, currentDepth - 1, !maximize, seeding).score

            // Check for maximum or minimum
            if ((maximize && moveScore > minOrMax.second) || (!maximize && moveScore < minOrMax.second))
                minOrMax = Pair(move, moveScore)
        }

        val finalMove = Storage.Record(game.storageRecordPrimaryKey, minOrMax.first, minOrMax.second, game.currentPlayer)


        // Add board evaluation temporary to storage
        // For data seeding we don't add it because it's added inside the seed method.
        if (game.storageIndex >= 0 && !(startingDepth == currentDepth && seeding))
            Storage.doStorageLookup<Move>(game.storageIndex).map[game.storageRecordPrimaryKey] = finalMove

        return finalMove
    }

}