package connect.four

import java.io.File
import kotlin.math.ceil

typealias StorageRecord = Triple<Move?, Float, Int>

/**
 * Interface for implementing Minimax algorithm in two-player zero-sum games
 * including storage for already evaluated board positions
 *
 * @param [Board] the type of the game board
 * @param [Move] the type of a move
 */
interface Minimax<Board, Move> {
    class Storage(private val index: Int) {
        private val filename: String
        private var mapInitialized: Boolean = false
        var map: HashMap<Int, StorageRecord> = HashMap()

        init {
            this.filename = getTableFilename(this.index)
        }

        /**
         * Write current map instance to storage .txt file
         *
         * @param [updateMap] update current map instance
         */
        fun syncFile(updateMap: Boolean = true) {
            val file = this.getFile()
            var res = ""

            this.map.clear()

            this.map.forEach { (k, v) ->
                if (updateMap) this.map[k] = v
                res += hashMapEntryToString(k, v) + "\n"
            }

            file.writeText(res)
        }

        /**
         * Append new HashMap to storage .txt file
         *
         * @param [map] new HashMap to append
         * @param [updateMap] update current map instance
         */
        fun appendMapToFile(map: HashMap<Int, StorageRecord>, updateMap: Boolean = true) {
            val file = this.getFile()
            var res = ""

            map.forEach { (k, v) ->
                if (!this.map.containsKey(k)) {
                    if (updateMap) this.map[k] = v
                    res += hashMapEntryToString(k, v) + "\n"
                }
            }

            file.appendText(res)
        }

        /**
         * Register storage
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
        private fun readMap(): HashMap<Int, StorageRecord> {
            val file = this.getFile()
            val map: HashMap<Int, StorageRecord> = HashMap()

            file.forEachLine {
                val elements = it.split(" ")
                map[elements[0].toInt()] = Triple(Move.ofStorageEntry(elements[1]), elements[2].toFloat(), elements[3].toInt())
            }

            return map
        }

        /**
         * Read StorageRecordKeys from storage .txt file
         *
         * @return HashSet of RecordKeys
         */
        fun readStorageRecordKeysAsHashSet(): HashSet<Int> {
            val file = this.getFile()
            val set: HashSet<Int> = HashSet()

            file.forEachLine { set.add(it.split(" ")[0].toInt()) }

            return set
        }

        private fun initMap() {
            this.map = this.readMap()
            this.mapInitialized = true
        }

        companion object {
            private const val numberOfTranspositionTables = 14
            private const val treeDepthTranspositionTables = 8
            private const val transpositionTablesPath = "src/main/resources/transposition_tables"
            private var allStoragesRegistered: Boolean = false
            private val storages: Array<Storage?> = Array(numberOfTranspositionTables) { null }

            /**
             * Convert HashMap value to string.
             * This method is mainly used to format the HashMap values for the storage .txt file
             *
             * @param [key] HashMap key
             * @param [entry] HashMap entry
             * @return String to write
             */
            fun hashMapEntryToString(key: Int, entry: StorageRecord): String =
                    "$key ${entry.first} ${entry.second} ${entry.third}"

            /**
             * Get storage index based on played moves.
             * Range of 3 per file
             *
             * @param [playedMoves] played moves
             * @return storage index
             */
            fun getStorageIndexFromPlayedMoves(playedMoves: Int): Int = ceil((playedMoves.toDouble() / 3)).toInt() - 1

            /**
             * Register all storages.
             * You might want to do this on app start
             */
            fun registerStorages() {
                for (i in storages.indices) Storage(i).register()
                allStoragesRegistered = true
            }

            /**
             * Get storage based on storage index
             * Always use this method to access a storage
             *
             * @param [index] storage index
             * @return Storage
             */
            fun doStorageLookup(index: Int): Storage {
                assert(index in 0 until numberOfTranspositionTables)
                Storage(index).register()
                return storages[index]!!
            }

            /**
             * Feed transposition tables amount-times with boards of movesPlayed-moves
             *
             * @param [amount] number of data records
             * @param [movesPlayed] number of played moves
             * */
            fun feedByMovesPlayed(amount: Int, movesPlayed: Int) {
                val storage = doStorageLookup(getStorageIndexFromPlayedMoves(movesPlayed))
                val startTime = System.currentTimeMillis()

                println("Start feeding storage #${storage.index}...")

                val newHashMap: HashMap<Int, StorageRecord> = HashMap()
                var count = 0

                outer@
                for (i in 1..amount) {
                    val game = ConnectFour.playRandomMoves(movesPlayed)
                    assert(game.getNumberOfRemainingMoves() > 0)

                    val storageRecordKeys = game.getStorageRecordKeys()

                    // Check if board is already stored
                    for (storageRecordKey in storageRecordKeys)
                        if (storage.map.contains(storageRecordKey.first) || newHashMap.containsKey(storageRecordKey.first))
                            continue@outer

                    count++
                    val move = game.minimax(currentDepth = treeDepthTranspositionTables)
                    newHashMap[storageRecordKeys[0].first] = Triple(move.first!!, move.second, move.third)
                }

                storage.appendMapToFile(newHashMap)

                val duration = (System.currentTimeMillis() - startTime) / 1000
                println("${storage.filename}: Added $count / $amount new data records in ${duration}s.")
            }

            /**
             * Get table file name based on storage index
             *
             * @param [storageIndex] storage index
             * @return file name
             */
            fun getTableFilename(storageIndex: Int): String {
                assert(storageIndex < numberOfTranspositionTables)
                val id = if (storageIndex < 10) "0$storageIndex" else "$storageIndex"
                val from = storageIndex * 3 + 1
                val to = from + 2
                return "${id}_table_${from}_${to}.txt"
            }
        }
    }

    companion object {
        /**
         * Best possible score for a board evaluation
         * In this case one player should have won and the game ended
         */
        const val maxBoardEvaluationScore: Float = 200F
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
     * Evaluate game state for current player.
     *  For player 1 (maximizer) a higher value is better.
     *  For player -1 (minimizer) a lower value is better.
     *
     * @return Positive or negative integer
     */
    fun evaluate(depth: Int): Float

    /**
     * Get list of all possible moves
     *
     * @return List of possible moves
     */
    fun getPossibleMoves(): List<Move>

    /**
     * Get maximum number of remaining moves until game is finished
     *
     * @return max number of remaining moves
     */
    fun getNumberOfRemainingMoves(): Int

    /**
     * Check if a player has one
     *
     * @return is game over
     */
    fun hasWinner(): Boolean

    /**
     * Check if no more moves are possible or a player has one
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
     * Pick random move from possible moves list
     *
     * @param [possibleMoves] possible moves to pick random move from
     * @return a random move
     */
    fun getRandomMove(possibleMoves: List<Move> = this.getPossibleMoves()): Move = possibleMoves.random()

    /**
     * Get index in storage based on played moves
     *
     * @return storage index
     */
    fun getStorageIndex(): Int

    /**
     * Get base key of current board for storage record.
     * Boards positions are saved under this key.
     *
     * @return base key
     */
    fun getBaseStorageRecordKey(): Int

    /**
     * Get all possible storage keys for the current board with according methods to transform the associated move.
     * This might include symmetries or inverted boards for example.
     * Index 0 should be baseStorageRecordKey.
     *
     * @return storage keys the board might be saved under
     */
    fun getStorageRecordKeys(): List<Pair<Int, (move: connect.four.Move) -> connect.four.Move>>

    /**
     * Minimax algorithm that finds best move
     *
     * @param [game] game to apply minimax on
     * @param [startingDepth] tree depth on start
     * @param [currentDepth] maximal tree depth (maximal number of moves to anticipate)
     * @param [maximize] maximize or minimize
     * @return triple of (Move, Score, CurrentPlayer)
     */
    fun minimax(
            game: Minimax<Board, Move> = this,
            startingDepth: Int = this.difficulty,
            currentDepth: Int = this.difficulty,
            maximize: Boolean = game.currentPlayer == 1
    ): StorageRecord {

        // Recursion anchor -> Evaluate board
        if (currentDepth == 0 || game.isGameOver()) return StorageRecord(null, game.evaluate(currentDepth), game.currentPlayer)

        // Check if board exists in storage
        val storageIndex = game.getStorageIndex()
        if (storageIndex >= 0) {
            val storage = Storage.doStorageLookup(storageIndex)
            this.getStorageRecordKeys().forEach { storageRecordKey ->
                if (storage.map.containsKey(storageRecordKey.first)) {
                    val storedBoard = storage.map[storageRecordKey.first]!! // Load from storage
                    val newScore = if (storedBoard.third == game.currentPlayer) storedBoard.second else storedBoard.second * storedBoard.third * game.currentPlayer
                    val newMove = storageRecordKey.second(storedBoard.first!!) // Transform move for given storageRecordKey
                    return StorageRecord(newMove, newScore, game.currentPlayer)
                }
            }
        }

        val possibleMoves = game.getPossibleMoves()

        // If there's a move which results in a win for the current player we return this move.
        // This way we don't have to evaluate other possible moves.
        possibleMoves.forEach { move ->
            if (game.move(move).hasWinner())
                return Triple(move, game.currentPlayer * maxBoardEvaluationScore, game.currentPlayer) as StorageRecord
        }

        // List of moves and their evaluations
        val evaluations = mutableListOf<Pair<Move?, Float>>()

        // Call recursively from here on for each move to find best one
        var minOrMax: Pair<Move?, Float> = Pair(null, if (maximize) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY)
        for (move in possibleMoves) {
            val newGame = game.move(move)
            val moveScore = this.minimax(newGame, startingDepth, currentDepth - 1, !maximize).second

            val evaluation = Pair(move, moveScore)
            evaluations.add(evaluation)

            // Check for maximum or minimum
            if ((maximize && moveScore > minOrMax.second) || (!maximize && moveScore < minOrMax.second))
                minOrMax = evaluation
        }

        // If all possible moves have the same evaluation we return a random one
        // We only do this if the move to return is the final one that is returned to the user
        if (currentDepth == startingDepth && evaluations.stream().allMatch() { it.second == evaluations.first().second }) {
            val randomMove = evaluations.random()
            return Triple(randomMove.first, randomMove.second, game.currentPlayer) as StorageRecord
        }

        return Triple(minOrMax.first, minOrMax.second, game.currentPlayer) as StorageRecord
    }
}