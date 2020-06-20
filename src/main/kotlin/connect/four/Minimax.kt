package connect.four

import java.io.File
import kotlin.math.ceil

typealias StorageRecord = Triple<Move, Float, Int>

/**
 * Interface for implementing Minimax algorithm in two-player zero-sum games
 * including storage for already evaluated board positions
 *
 * @param [Move] the type of a move
 */
interface Minimax<Move> {
    class Storage(private val index: Int) {
        private val filename: String
        val map: HashMap<Int, StorageRecord>

        init {
            this.filename = getTableFilename(this.index)
            this.map = this.loadHashMap()
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
        fun appendFile(map: HashMap<Int, StorageRecord>, updateMap: Boolean = true) {
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
        fun register() = run { storages[this.index] = this }

        /**
         * Check if storage is registered
         *
         * @return whether storage is registered
         */
        fun isRegistered(): Boolean = storages[this.index] != null

        /**
         * Get storage .txt file
         *
         * @return file
         */
        private fun getFile(): File = File("$transpositionTablesPath/${this.filename}")

        /**
         * Load HashMap from storage .txt file
         *
         * @return HashMap for played moves
         */
        private fun loadHashMap(): HashMap<Int, StorageRecord> {
            val file = this.getFile()
            val map: HashMap<Int, StorageRecord> = HashMap()

            file.forEachLine {
                val elements = it.split(" ")
                map[elements[0].toInt()] = Triple(Move.ofStorageEntry(elements[1]), elements[2].toFloat(), elements[3].toInt())
            }

            return map
        }

        companion object {
            private const val numberOfTranspositionTables = 14
            private const val transpositionTablesPath = "src/main/resources/transposition_tables"
            private var storagesRegistered: Boolean = false
            private val storages: Array<Storage?> = Array(numberOfTranspositionTables) { null }

            /**
             * Convert HashMap value to string
             * This method is mainly used to format the HashMap values for the storage .txt file
             *
             * @param [key] HashMap key
             * @param [entry] HashMap entry
             * @return String to write
             */
            fun hashMapEntryToString(key: Int, entry: StorageRecord): String =
                    "$key ${entry.first} ${entry.second} ${entry.third}"

            /**
             * Get storage index based on played moves
             *
             * @param [playedMoves] played moves
             * @return storage index
             */
            fun getStorageIndexFromPlayedMoves(playedMoves: Int): Int = ceil((playedMoves.toDouble() / 3)).toInt() - 1

            /**
             * Register all storages
             */
            fun registerStorages() {
                for (i in storages.indices) {
                    println("Register storage #$i...")
                    Storage(i).register()
                }
                storagesRegistered = true
            }

            /**
             * Get storage based on storage index
             *
             * @param [index] storage index
             * @return Storage
             */
            fun doStorageLookup(index: Int): Storage {
                assert(index in 1 until numberOfTranspositionTables)
                if (!storagesRegistered) registerStorages()
                return storages[index]!!
            }

            /**
             * Feed transposition tables amount-times with boards of movesPlayed-moves
             *
             * @param [amount] number of data records
             * @param [movesPlayed] number of played moves
             * */
            fun feedByMovesPlayed(amount: Int, movesPlayed: Int) {
                val storage = Storage(getStorageIndexFromPlayedMoves(movesPlayed))
                val newHashMap: HashMap<Int, StorageRecord> = HashMap()
                var count = 0

                for (i in 1..amount) {
                    val game = ConnectFour().playRandomMoves(movesPlayed)
                    assert(game.getNumberOfRemainingMoves() > 0)

                    val hashCode = game.getStorageRecordKey()
                    val move = game.minimax()

                    // Board is not stored yet
                    if (!storage.map.containsKey(hashCode) && !newHashMap.containsKey(hashCode)) {
                        count++
                        newHashMap[hashCode] = Triple(move.first!!, move.second, move.third)
                    }
                }

                storage.appendFile(newHashMap, false)
                println("${storage.filename}: Added $count / $amount new data records.")
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

    /**
     * Game board
     */
    val board: Array<IntArray>

    /**
     * Current player
     */
    val currentPlayer: Int

    /**
     * Evaluate game state for current player.
     *  For player +1, a higher value is better. (maximizer)
     *  For player -1, a lower value is better. (minimizer)
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

    fun getNumberOfRemainingMoves(): Int

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
    fun move(move: Move): Minimax<Move>

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
     * Get key of current board for storage record
     */
    fun getStorageRecordKey(): Int

    /**
     * Minimax algorithm that finds best move
     *
     * @param [game]
     * @param [depth] maximal tree depth
     * @param [maximize] maximize or minimize
     * @return triple of (Move, Score, CurrentPlayer)
     */
    fun minimax(
            game: Minimax<Move> = this,
            depth: Int = this.getNumberOfRemainingMoves(),
            maximize: Boolean = game.currentPlayer == 1
    ): Triple<Move?, Float, Int> {

        // Recursion anchor -> Evaluate board
        if (depth == 0 || game.isGameOver()) return Triple(null, game.evaluate(depth), game.currentPlayer)

        val storageIndex = game.getStorageIndex()
        if (storageIndex >= 0) {
            val storage = Storage.doStorageLookup(storageIndex)
            val storageRecordKey = this.getStorageRecordKey()

            // Check if board exists in storage
            if (storage.map.containsKey(storageRecordKey)) {
                val storedBoard = storage.map[storageRecordKey]!! as Triple<Move?, Float, Int>
                val newScore = if (storedBoard.third == game.currentPlayer) storedBoard.second else storedBoard.second * storedBoard.third * game.currentPlayer
                return Triple(storedBoard.first, newScore, game.currentPlayer)
            }
        }

        // Call recursively from here on for each move to find best one
        var minOrMax: Pair<Move?, Float> = Pair(null, if (maximize) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY)
        for (move in game.getPossibleMoves()) {
            val moveScore = this.minimax(game.move(move), depth - 1, !maximize).second

            // Check for maximum or minimum
            if ((maximize && moveScore > minOrMax.second) || (!maximize && moveScore < minOrMax.second))
                minOrMax = Pair(move, moveScore)
        }

        return Triple(minOrMax.first, minOrMax.second, game.currentPlayer)
    }
}