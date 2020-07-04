package connect.four

import java.io.File

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
        var map: HashMap<Int, Record<Move>> = HashMap()

        companion object {
            private const val numberOfTranspositionTables = 14
            private const val maxTreeDepthTranspositionTables = 8
            private const val transpositionTablesPath = "src/main/resources/transposition_tables"
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
                assert(index in 0 until numberOfTranspositionTables)
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
                println("\nStart seeding storage for #$movesPlayed played moves...")

                val startTime = System.currentTimeMillis()
                val newHashMap: HashMap<Int, Record<Move>> = HashMap()

                var countNewRecords = 0
                var countIterations = 0

                var game: ConnectFour
                var storage: Storage<Move>

                outer@
                do {
                    game = ConnectFour.playRandomMoves(movesPlayed)
                    storage = doStorageLookup(game.storageIndex)

                    assert(game.getNumberOfRemainingMoves() > 0)

                    countIterations++

                    // Check if board is already stored -> Skip it
                    for (storageRecordKey in game.getStorageRecordKeys())
                        if (storage.map.contains(storageRecordKey.first) || newHashMap.containsKey(storageRecordKey.first))
                            continue@outer

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
                assert(storageIndex < numberOfTranspositionTables)
                val id = if (storageIndex < 10) "0$storageIndex" else "$storageIndex"
                val from = storageIndex * 3 + 1
                val to = from + 2
                return "${id}_table_${from}_${to}.txt"
            }
        }

        /**
         * Append new HashMap to storage .txt file (persistent) and update map instance
         *
         * @param [map] new HashMap to append
         */
        fun appendMapToFile(map: HashMap<Int, Record<Move>>) {
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
        private fun readMap(): HashMap<Int, Record<Move>> {
            val file = this.getFile()
            val map: HashMap<Int, Record<Move>> = HashMap()

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
                val key: Int?,
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
                    return Record(elements[0].toInt(), Move.ofStorageEntry(elements[1]), elements[2].toFloat(), elements[3].toInt()) as Record<M>
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
    val storageRecordPrimaryKey: Int

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
     * @return is game over
     */
    fun hasWinner(): Boolean

    /**
     * Check if [getNumberOfRemainingMoves] is 0 or [hasWinner] is true
     *
     * @return is game over
     */
    fun isGameOver(): Boolean

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
     * Get all possible storage keys for the current board with according methods to transform the associated move.
     * - This might include symmetries or inverted boards for example
     * - Index 0 should be [storageRecordPrimaryKey]
     *
     * @return storage keys the board might be stored under
     */
    fun getStorageRecordKeys(): List<Pair<Int, (record: Storage.Record<Move>) -> Storage.Record<Move>?>>

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
        if (currentDepth == 0 || game.isGameOver())
            return Storage.Record(null, null, game.evaluate(currentDepth), game.currentPlayer)

        // Check if board exists in storage
        var existsInStorage = false
        val storageIndex = game.storageIndex

        if (storageIndex >= 0) {
            val storage = Storage.doStorageLookup<Move>(storageIndex) // Load storage

            // We check every possible key under which the field could be stored
            // or might be associated with already existing records
            game.getStorageRecordKeys().forEach { storageRecordKey ->
                if (storage.map.containsKey(storageRecordKey.first)) {
                    val storageRecord = storage.map[storageRecordKey.first]!! // Load from storage
                    existsInStorage = true

                    // Create new storageRecord based on key
                    val newStorageRecord = storageRecordKey.second(storageRecord)
                    if (newStorageRecord != null) return newStorageRecord
                }
            }
        }

        val possibleMoves = game.getPossibleMoves(true)

        // If there's a move which results in a win for the current player we immediately return this move.
        // This way we don't have to evaluate other possible moves.
        possibleMoves.forEach { move ->
            val tmpGame = game.move(move)
            if (tmpGame.hasWinner())
                return Storage.Record(
                        tmpGame.storageRecordPrimaryKey,
                        move,
                        game.currentPlayer * Float.MAX_VALUE,
                        game.currentPlayer
                )
        }

        // Call recursively from here on for each move to find best one
        var minOrMax: Pair<Move?, Float> = Pair(null, if (maximize) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY)
        for (move in possibleMoves) {
            val newGame = game.move(move)
            val moveScore = this.minimax(newGame, startingDepth, currentDepth - 1, !maximize).score

            // Check for maximum or minimum
            if ((maximize && moveScore > minOrMax.second) || (!maximize && moveScore < minOrMax.second))
                minOrMax = Pair(move, moveScore)
        }

        val finalMove = Storage.Record(game.storageRecordPrimaryKey, minOrMax.first, minOrMax.second, game.currentPlayer)

        // We add board evaluation temporary to storage if it does not already exist in it.
        if (!seeding && !existsInStorage && storageIndex >= 0)
            Storage.doStorageLookup<Move>(storageIndex).map[game.storageRecordPrimaryKey] = finalMove

        return finalMove
    }
}