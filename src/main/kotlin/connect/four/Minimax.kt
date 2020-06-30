package connect.four

import java.io.File
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Interface for implementing Minimax algorithm in two-player zero-sum games
 * including storage for already evaluated board positions
 *
 * @param [Board] the type of the game board
 * @param [Move] the type of a move
 */
interface Minimax<Board, Move> {
    class Storage(val index: Int) {
        private val filename: String
        private var mapInitialized: Boolean = false
        var map: HashMap<Int, TableRecord<Move>> = HashMap()

        init {
            this.filename = getTableFilename(this.index)
        }

        /**
         * Append new HashMap to storage .txt file
         *
         * @param [map] new HashMap to append
         * @param [updateMap] update current map instance
         */
        fun appendMapToFile(map: HashMap<Int, TableRecord<Move>>, updateMap: Boolean = true) {
            val file = this.getFile()
            var res = ""

            map.forEach { (k, v) ->
                if (!this.map.containsKey(k)) {
                    if (updateMap) this.map[k] = v
                    res += v.toString() + "\n"
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
        private fun readMap(): HashMap<Int, TableRecord<Move>> {
            val file = this.getFile()
            val map: HashMap<Int, TableRecord<Move>> = HashMap()

            file.forEachLine {
                val elements = it.split(" ")
                map[elements[0].toInt()] = TableRecord(
                        key = elements[0].toInt(),
                        depth = elements[1].toInt(),
                        flag = Flag.valueOf(elements[2]),
                        move = Move.ofStorageEntry(elements[3]),
                        score = elements[4].toFloat())
            }

            return map
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

                val newHashMap: HashMap<Int, TableRecord<Move>> = HashMap()
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
                    val move = game.negamax(depth = treeDepthTranspositionTables)
                    newHashMap[move.key!!] = move
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

    enum class Flag {
        EXACT, LOWERBOUND, UPPERBOUND
    }

    class TableRecord<Move>(val key: Int?, val depth: Int, val flag: Flag?, val move: Move?, val score: Float) {
        override fun toString(): String {
            return "$key $depth $flag $move $score"
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
     * Negamax algorithm that finds best move
     * including Alpha / Beta pruning and transposition tables
     *
     * @param [game] game to apply minimax on
     * @return triple of (Move, Score, CurrentPlayer)
     */
    fun negamax(
            game: Minimax<Board, Move> = this,
            depth: Int = this.difficulty,
            alpha: Float = Float.NEGATIVE_INFINITY,
            beta: Float = Float.POSITIVE_INFINITY,
            storedBoards: HashMap<Int, TableRecord<Move>> = HashMap()
    ): TableRecord<Move> {

        // Recursion anchor -> Evaluate board
        if (depth == 0 || game.isGameOver())
            return TableRecord(null, depth = depth, flag = null, move = null, score = game.evaluate(depth))

        // Alpha / Beta Window
        var wAlpha = alpha
        var wBeta = beta

        // Check if board exists in storage for any possible storage key
        // We might have to adjust the Alpha-Beta window
        val storageIndex = game.getStorageIndex()
        if (storageIndex >= 0 || true) {
          //  val storage = Storage.doStorageLookup(storageIndex)
            for (storageRecordKey in game.getStorageRecordKeys()) {
                // Check if board exists in storage
                if (storedBoards.containsKey(storageRecordKey.first)) {
                    val storedBoard = storedBoards[storageRecordKey.first]!! // Load from storage

                    // val storedBoard = storage.map[storageRecordKey.first]!! as TableRecord<Move> // Load from storage
                    val newMove = storageRecordKey.second(storedBoard.move as connect.four.Move) // Transform move for given storageRecordKey

                    if (storedBoard.depth >= depth) {
                        val newBoard = TableRecord(storedBoard.key, storedBoard.depth, storedBoard.flag, newMove, storedBoard.score) as TableRecord<Move>
                        when (storedBoard.flag) {
                            Flag.EXACT -> return newBoard
                            Flag.LOWERBOUND -> wAlpha = max(alpha, storedBoard.score)
                            Flag.UPPERBOUND -> wBeta = min(beta, storedBoard.score)
                        }

                        if (wAlpha >= wBeta) return newBoard
                        break
                    }
                }
            }
        }

        val possibleMoves = game.getPossibleMoves()

        // If there's a move which results in a win for the current player we return this move.
        // This way we don't have to evaluate other possible moves.
        possibleMoves.forEach { move ->
            val tmpGame = game.move(move)
            if (tmpGame.hasWinner()) {
                val flag: Flag
                if (maxBoardEvaluationScore <= alpha) {
                    flag = Flag.UPPERBOUND
                } else if (maxBoardEvaluationScore >= wBeta) {
                    flag = Flag.LOWERBOUND
                } else {
                    flag = Flag.EXACT
                }

                return TableRecord(tmpGame.getBaseStorageRecordKey(), depth, flag, move, maxBoardEvaluationScore)
            }
        }

        // List of moves and their evaluations
        val evaluations = mutableListOf<Pair<Move?, Float>>()

        // Call recursively from here on for each move to find best one
        var maxScore: Pair<Move?, Float> = Pair(null, wAlpha)
        for (move in possibleMoves) {
            // Apply move - We have to cast here since NimGame prescribes the return type NimGame
            val newGame = game.move(move)
            val moveScore = -this.negamax(game = newGame, depth = depth - 1, alpha = -wBeta, beta = -maxScore.second, storedBoards = storedBoards).score

            // Check for maximum or minimum
            if (moveScore > maxScore.second) {
                maxScore = Pair(move, moveScore)
                if (moveScore >= wBeta) break
            }
        }

        val flag: Flag
        if (maxScore.second <= alpha) {
            flag = Flag.UPPERBOUND
        } else if (maxScore.second >= wBeta) {
            flag = Flag.LOWERBOUND
        } else {
            flag = Flag.EXACT
        }

        // If all possible moves have the same evaluation we return a random one
        // We only do this if the move to return is the final one that is returned to the user
        if (false && evaluations.stream().allMatch() { it.second == evaluations.first().second }) {
            val randomMove = evaluations.random()
            return TableRecord(game.getBaseStorageRecordKey(), depth, flag, randomMove.first, randomMove.second)
        }

        val tableRecord = TableRecord(game.getBaseStorageRecordKey(), depth, flag, maxScore.first, maxScore.second)

        if(maxScore.first != null) storedBoards[game.getBaseStorageRecordKey()] = tableRecord

        return tableRecord
    }
}