package connect.four

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.*

/**
 * Class that represents a connect-four game
 *
 * @param [board] array of 2 bitboards
 * @param [currentPlayer] current player
 * @param [difficulty] AI strength
 * @param [storageRecordPrimaryKey] key under which the board will be saved to storage
 * @param [numberOfPlayedMoves] number of already played moves
 * @param [heights] current height of board columns
 * @param [history] history of boards
 * @param [moveDuration] duration of bestMove calculation
 */
class ConnectFour(
        override val board: LongArray = longArrayOf(
                0b0_0000000_0000000___0000000_0000000_0000000_0000000_0000000_0000000_0000000, // X = player 1 (red)
                0b0_0000000_0000000___0000000_0000000_0000000_0000000_0000000_0000000_0000000  // O = player -1 (yellow)
        ),
        override val currentPlayer: Int = 1,
        override val difficulty: Int = 5,
        override val storageRecordPrimaryKey: Long = calcZobristHash(board),
        val numberOfPlayedMoves: Int = calcNumberOfPlayedMoves(board),
        private val heights: IntArray = calcBoardHeights(board),
        private val history: List<LongArray> = listOf(),
        private val moveDuration: Int = 0) : Minimax<LongArray, Move> {

    // Storage index based on number of played moves -> In steps of six
    override val storageIndex = floor((this.numberOfPlayedMoves.toDouble() / 6)).toInt()

    companion object {
        /**
         * Most top column cells in bitboards
         */
        const val top = 0b1000000_1000000_1000000_1000000_1000000_1000000_1000000L

        /**
         * Play exactly n given random moves with ending up in a draw position.
         *
         * @param [n] number of moves to play
         * @return game with n played moves
         */
        fun playRandomMoves(n: Int): ConnectFour {
            var game = ConnectFour()
            for (i in 1..n) {
                game = game.move(game.getRandomMove())
                if (game.hasWinner(-game.currentPlayer)) return playRandomMoves(n)

            }
            return game
        }

        /**
         * Calculate zobrist-hash for a given board
         *
         * @param [board] board to hash
         * @return hash for given board
         */
        fun calcZobristHash(board: LongArray): Long {
            var hash = 0L

            for (player in intArrayOf(1, -1)) {
                val tmpBoard: Long = if (player == 1) board[0] else board[1]
                for (cell in 0..47) {
                    if (1L shl cell and tmpBoard != 0L)
                        hash = hash xor Minimax.Storage.getZobristHash(cell, player)
                }
            }

            return hash
        }

        /**
         * Calculate board heights based on already played moves
         *
         * @param [board] board to calculate heights for
         * @return array of heights for given board
         */
        fun calcBoardHeights(board: LongArray): IntArray {
            assert(isValidBoard(board)) { "Cannot calculate board heights! The given board is invalid." }
            val heights = IntArray(7) { 0 }

            for ((index, column) in (0..42 step 7).withIndex()) {
                // set bottom as default
                heights[index] = column

                for (row in column..column + 5)
                // Check if either player 1 or player -1 has set a chip
                    if ((1L shl row and board[0] != 0L) || 1L shl row and board[1] != 0L)
                        heights[index] = row + 1
            }

            return heights
        }

        /**
         * Get number of played moves of both players
         *
         * @return number of played moves
         */
        fun calcNumberOfPlayedMoves(board: LongArray): Int {
            var count = 0
            for (i in 0..47) {
                if ((1L shl i and board[0] != 0L) || (1L shl i and board[1] != 0L)) count++
            }

            return count
        }

        /**
         * Mirror player board on center y-axis
         *
         * @param [board] player board as bit representation
         * @return mirrored board as bit representation
         */
        fun mirrorPlayerBoard(board: Long): Long {
            var mirror = board and 0b1111111 shl 42
            mirror = mirror or (board and 0b1111111_0000000 shl 28)
            mirror = mirror or (board and 0b1111111_0000000_0000000 shl 14)
            mirror = mirror or (board and 0b1111111_0000000_0000000_0000000)
            mirror = mirror or (board and 0b1111111_0000000_0000000_0000000_0000000 shr 14)
            mirror = mirror or (board and 0b1111111_0000000_0000000_0000000_0000000_0000000 shr 28)
            mirror = mirror or (board and 0b1111111_0000000_0000000_0000000_0000000_0000000_0000000 shr 42)

            return mirror
        }

        /**
         * Check if the given board is a valid one.
         * There can't be two chips at the same cell.
         *
         * @param [board] board to validate
         * @return is valid board
         */
        private fun isValidBoard(board: LongArray): Boolean = board[0] and board[1] == 0L
    }

    override fun move(move: Move, duration: Int): ConnectFour {
        assert(this.isValidMove(move)) { "Invalid move!" }

        // Update column height
        val newHeights = this.heights.clone()
        newHeights[move.column] += 1

        // Shift move to the according cell
        val shiftedMove = 1L shl this.heights[move.column]

        // Update current player's board
        val newPlayerBoard = this.getPlayerBoard() xor shiftedMove
        val newBoards = when (this.currentPlayer) {
            1 -> longArrayOf(newPlayerBoard, this.board[1])
            else -> longArrayOf(this.board[0], newPlayerBoard)
        }

        // Calculate new zobrist-hash
        val cellPlacedAt = this.heights[move.column]
        val newZobristHash = this.storageRecordPrimaryKey xor Minimax.Storage.getZobristHash(cellPlacedAt, this.currentPlayer)

        return ConnectFour(
                board = newBoards,
                currentPlayer = -this.currentPlayer,
                difficulty = this.difficulty,
                storageRecordPrimaryKey = newZobristHash,
                numberOfPlayedMoves = this.numberOfPlayedMoves + 1,
                heights = newHeights,
                history = this.history.plusElement(this.board),
                moveDuration = duration
        )
    }

    override fun undoMove(number: Int): ConnectFour {
        assert(number <= this.history.size) { "You cannot undo more moves than moves were played!" }

        val nextPlayer: Int = if (number % 2 == 0) this.currentPlayer else -this.currentPlayer

        return ConnectFour(
                board = if (number > 0) this.history[this.history.size - number] else this.board,
                currentPlayer = nextPlayer,
                difficulty = this.difficulty,
                numberOfPlayedMoves = this.numberOfPlayedMoves - number,
                history = this.history.subList(0, this.history.size - number)
        )
    }

    override fun searchBestMoveInStorage(): Minimax.Storage.Record<Move>? {
        if (this.storageIndex >= 0) {
            val storage = Minimax.Storage.doStorageLookup<Move>(this.storageIndex) // Load storage

            this.getStorageRecordKeys().forEach { storageRecordKey ->
                val key = storageRecordKey() // Calculate key

                // Check if hash exists in storage
                if (storage.map.containsKey(key.first)) {
                    val storageRecord = storage.map[key.first]!! // Load from storage

                    // Call "Processing-Method" => create new storageRecord based on key
                    val newStorageRecord = key.second(storageRecord)
                    if (newStorageRecord != null) return newStorageRecord
                }
            }
        }

        // no match in storage
        return null
    }

    override fun getStorageRecordKeys(): List<() -> Pair<Long, (record: Minimax.Storage.Record<Move>) -> Minimax.Storage.Record<Move>?>> {

        /**
         * ##### Symmetries #####
         *
         * We encapsulate the keys in functions to avoid unnecessary key calculations.
         * The keys gets calculated when we call the according method.
         *
         * - Inverse board: -1 to 1 and vice versa
         * - Mirror board on center y-Axis
         * - Mirror board and inverse
         */

        // storageRecordPrimaryKey (base) - exact match
        val key1 = fun(): Pair<Long, (record: Minimax.Storage.Record<Move>) -> Minimax.Storage.Record<Move>?> =
                Pair(this.storageRecordPrimaryKey, { storageRecord ->
                    if (this.currentPlayer == storageRecord.player)
                        storageRecord
                    else null
                })

        // mirror
        val key2 = fun(): Pair<Long, (record: Minimax.Storage.Record<Move>) -> Minimax.Storage.Record<Move>?> =
                Pair(calcZobristHash(this.mirrorBoard()), { storageRecord ->
                    if (this.currentPlayer == storageRecord.player) {
                        val newMove = storageRecord.move!!.mirrorYAxis()
                        Minimax.Storage.Record(this.storageRecordPrimaryKey, newMove, storageRecord.score, storageRecord.player)
                    } else null
                })

        // inverse
        val key3 = fun(): Pair<Long, (record: Minimax.Storage.Record<Move>) -> Minimax.Storage.Record<Move>?> =
                Pair(calcZobristHash(longArrayOf(this.board[1], this.board[0])), { storageRecord ->
                    if (this.currentPlayer != storageRecord.player)
                        Minimax.Storage.Record(this.storageRecordPrimaryKey, storageRecord.move!!, -storageRecord.score, -storageRecord.player)
                    else null
                })

        // mirror and inverse
        val key4 = fun(): Pair<Long, (record: Minimax.Storage.Record<Move>) -> Minimax.Storage.Record<Move>?> =
                Pair(calcZobristHash(longArrayOf(mirrorPlayerBoard(this.board[1]), mirrorPlayerBoard(this.board[0]))), { storageRecord ->
                    if (this.currentPlayer != storageRecord.player) {
                        val newMove = storageRecord.move!!.mirrorYAxis()
                        Minimax.Storage.Record(this.storageRecordPrimaryKey, newMove, -storageRecord.score, -storageRecord.player)
                    } else null
                })

        return listOf(key1, key2, key3, key4)
    }

    /**
     * Check if [numberOfPlayedMoves] is 42 or [hasWinner] is true
     *
     * @param [player] set to 1 or -1 to check only for the given player's win in [hasWinner]
     * @return is game over
     */
    override fun isGameOver(player: Int): Boolean = this.hasWinner(player) || this.numberOfPlayedMoves == 42

    override fun hasWinner(player: Int): Boolean = when (player) {
        1 -> this.fourInARow(this.board[0])
        -1 -> this.fourInARow(this.board[1])
        else -> this.fourInARow(this.board[0]) || this.fourInARow(this.board[1])
    }

    override fun evaluate(depth: Int): Float = this.mcm()

    override fun getPossibleMoves(shuffle: Boolean): List<Move> {
        val possibleMoves = mutableListOf<Move>()
        val top = 0b1000000_1000000_1000000_1000000_1000000_1000000_1000000L

        for (col in 0..6)
            if (top and (1L shl this.heights[col]) == 0L)
                possibleMoves.add(Move(col))

        return if (shuffle) possibleMoves.shuffled() else possibleMoves.toList()
    }

    override fun getNumberOfRemainingMoves(): Int = 42 - this.numberOfPlayedMoves

    override fun toString(): String {
        var res = ""

        for (i in 5 downTo 0) {
            for (k in i..(42 + i) step 7) {
                val player = if (1L shl k and board[0] != 0L) "X" else if (1L shl k and board[1] != 0L) "O" else "."
                res += "$player "
            }
            res += "\n"
        }

        return res
    }

    /**
     * Check if given move is valid
     *
     * @param [move] move to check
     * @return is move valid
     */
    fun isValidMove(move: Move): Boolean = move.column in 0..6 && top and (1L shl this.heights[move.column]) == 0L

    /**
     * Perform best possible move for current player
     *
     * @return game with applied best move
     */
    fun bestMove(): ConnectFour {
        val startTime = System.currentTimeMillis()
        val bestMove = this.minimax().move!!
        val endTime = (System.currentTimeMillis() - startTime).toInt()
        return this.move(bestMove, endTime)
    }


    /**
     * Get winner of the game
     *
     * @return player 1 / -1 or 0 if draw
     */
    fun getWinner(): Int {
        assert(this.isGameOver()) { "The game has not ended yet! There is no winner." }
        return if (this.hasWinner()) -this.currentPlayer else 0
    }

    fun getPlayerSymbol(player: Int = this.currentPlayer): String = when (player) {
        1 -> "X"
        else -> "O"
    }

    fun toHtml(): String = this.metadataToHtml() + this.boardToHtml()

    /**
     * Check if four chips of the same player are in one row.
     * Vertically, horizontally and diagonal.
     *
     * @return if four in a row
     */
    private fun fourInARow(bitboard: Long): Boolean {
        val directions = intArrayOf(1, 7, 6, 8)
        var bb: Long

        for (direction in directions) {
            bb = bitboard and (bitboard shr direction)
            if (bb and (bb shr 2 * direction) != 0L) return true
        }

        return false
    }

    /**
     * Monte Carlo method for board evaluation.
     * Simulate a number of random games and evaluate based on the number of wins
     *
     * @param [numberOfSimulations] how many games to play
     * @return evaluation score
     */
    private fun mcm(numberOfSimulations: Int = 200): Float {
        val remainingMoves = this.getNumberOfRemainingMoves()
        val score = AtomicInteger()

        // Simulate games
        runBlocking {
            withContext(Dispatchers.Default) {
                repeat(numberOfSimulations) {
                    launch {
                        // Apply only the essential properties
                        var game = ConnectFour(
                                board = longArrayOf(board[0], board[1]),
                                currentPlayer = currentPlayer,
                                heights = heights.clone(),
                                storageRecordPrimaryKey = 1, // Prevent to calculate zobrist-hash
                                numberOfPlayedMoves = numberOfPlayedMoves
                        )

                        // Play random moves until game is over
                        // Factor: the earlier the game has ended, the better is the move
                        var factor = remainingMoves
                        while (!game.isGameOver(-game.currentPlayer)) {
                            game = game.move(game.getRandomMove())
                            factor--
                        }

                        // Update stats based on winner
                        when (game.getWinner()) {
                            currentPlayer -> score.addAndGet(1 + 5 * factor) // win
                            -currentPlayer -> score.addAndGet(-(1 + 5 * factor)) // defeat
                        }
                    }
                }
            }
        }

        return this.currentPlayer * score.toFloat()
    }

    /**
     * Mirror board on center y-axis
     *
     * @return mirrored board
     */
    private fun mirrorBoard(): LongArray = longArrayOf(mirrorPlayerBoard(this.board[0]), mirrorPlayerBoard(this.board[1]))

    /**
     * Get board of the according player.
     * - Player 1: index 0 in [board]
     * - Player -1: index 1 in [board]
     *
     * @param [player]
     * @return player board
     */
    private fun getPlayerBoard(player: Int = this.currentPlayer) = when (player) {
        1 -> this.board[0]
        else -> this.board[1]
    }

    private fun metadataToHtml(): String {
        fun running(): String {
            val currentPlayer = when (this.currentPlayer) {
                1 -> "<span class='color-red'>Rot</span>"
                else -> "<span class='color-yellow'>Gelb</span>"
            }

            var content = "<div class='col-auto'><h3>Aktueller Spieler: $currentPlayer</h3></div>"

            content += "<div class='col text-center duration'>" +
                    "<div class='spinner-border' role='status'><span class='sr-only'>Loading...</span></div>" +
                    "<span>${this.moveDuration}ms</span>" +
                    "</div>"

            content += "<div class='col text-right'>" +
                    "<button class='btn btn-primary mr-2' onclick='game.aiMove()'>KI Zug</button>" +
                    "<button class='btn btn-secondary' onclick='game.undoMove()'>Rückgängig</button></div>"

            return content
        }

        fun finish(): String {
            var content = "<div class='col-auto'><h3 class='text-center'>" + when (this.getWinner()) {
                1 -> "Spieler <span class='color-red'>Rot</span> gewinnt!"
                -1 -> "Spieler <span class='color-yellow'>Gelb</span> gewinnt!"
                else -> "Unentschieden!"
            } + "</h3></div>"

            content += "<div class='col text-right'>" +
                    "<button class='btn btn-primary mr-2' onclick='game.restart()'>Neustart</button>" +
                    "<button class='btn btn-secondary' onclick='game.undoMove()'>Rückgängig</button></div>"

            return content
        }

        var res: String

        if (this.isGameOver()) {
            res = "<div class='metadata finished row d-flex align-items-center'>"
            res += finish()
        } else {
            res = "<div class='metadata row d-flex align-items-center'>"
            res += running()
        }

        res += "</div><hr class='featurette-divider'>"

        return res
    }

    private fun boardToHtml(): String {
        var res = "<div class='board row mx-auto shadow-lg'>"

        for (i in 47 downTo 5 step 7) {
            res += "<div class='column col-auto' onclick='game.move(${i / 7})'>"
            for (k in i downTo i - 5) {
                val playerColor = if (1L shl k and board[0] != 0L) "red" else if (1L shl k and board[1] != 0L) "yellow" else ""
                res += "<div class='stone $playerColor'></div>"
            }
            res += "</div>"
        }

        res += "</div>"

        return res
    }
}