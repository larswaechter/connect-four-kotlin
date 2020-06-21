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
        fun isRegistered(): Boolean = storages[this.index] != null

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
                    val move = game.minimax()
                    newHashMap[storageRecordKeys[0].first] = Triple(move.first!!, move.second, move.third)
                }

                storage.appendFile(newHashMap)
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
    val board: Board

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

    /**
     * Get maximum number of remaining moves until game is finished
     *
     * @return max number of remaining moves
     */
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
     * Get base key of current board for storage record
     *
     * @return
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
     * @param [game]
     * @param [depth] maximal tree depth (maximal number of moves to anticipate)
     * @param [maximize] maximize or minimize
     * @return triple of (Move, Score, CurrentPlayer)
     */
    fun minimax(
            game: Minimax<Board, Move> = this,
            depth: Int = 10,
            maximize: Boolean = game.currentPlayer == 1
    ): StorageRecord {

        // Recursion anchor -> Evaluate board
        if (depth == 0 || game.isGameOver()) return StorageRecord(null, game.evaluate(depth), game.currentPlayer)

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

        // Call recursively from here on for each move to find best one
        var minOrMax: Pair<Move?, Float> = Pair(null, if (maximize) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY)
        for (move in game.getPossibleMoves()) {
            val moveScore = this.minimax(game.move(move), depth - 1, !maximize).second

            // Check for maximum or minimum
            if ((maximize && moveScore > minOrMax.second) || (!maximize && moveScore < minOrMax.second))
                minOrMax = Pair(move, moveScore)
        }

        return Triple(minOrMax.first, minOrMax.second, game.currentPlayer) as StorageRecord
    }
}