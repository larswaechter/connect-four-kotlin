package connect.four

import kotlin.math.*

/**
 * Class that represents a connect-four game
 *
 * @param [board] array of 2 bitboards
 * @param [currentPlayer] current player
 * @param [difficulty] AI strength
 * @param [storageRecordPrimaryKey] key under which the board will be saved to storage
 * @param [heights] current height of board columns
 * @param [history] history of boards
 * @param [multiplayer] playing vs ai or another human player
 */
class ConnectFour(
        override val board: LongArray = longArrayOf(
                0b0_0000000_0000000___0000000_0000000_0000000_0000000_0000000_0000000_0000000, // X = player 1 (red)
                0b0_0000000_0000000___0000000_0000000_0000000_0000000_0000000_0000000_0000000 // O = player -1 (yellow)
        ),
        override val currentPlayer: Int = 1,
        override val difficulty: Int = 5,
        override val storageRecordPrimaryKey: Long = calcZobristHash(board),
        private val heights: IntArray = calcBoardHeights(board),
        private val history: List<LongArray> = listOf(),
        val multiplayer: Boolean = false) : Minimax<LongArray, Move> {

    // Storage index based on number of played moves -> In steps of three
    override val storageIndex = ceil((this.getNumberOfPlayedMoves().toDouble() / 3)).toInt() - 1

    companion object {
        /**
         * Most top column cells in bitboards
         */
        const val top = 0b1000000_1000000_1000000_1000000_1000000_1000000_1000000L

        /**
         * Play exactly n given random moves with ending up in a draw position.
         * This method is mainly used to create different random game positions for the transposition tables.
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
                for (cell in 0..47)
                    if (1L shl cell and tmpBoard != 0L)
                        hash = hash xor Minimax.Storage.getZobristHash(cell, player)
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

            // loop columns
            for ((index, column) in (0..42 step 7).withIndex()) {
                // set bottom as default
                heights[index] = column

                // loop rows down -> up
                for (row in column..column + 5)
                // Check if either player 1 or player -1 has set a chip
                    if ((1L shl row and board[0] != 0L) || 1L shl row and board[1] != 0L)
                        heights[index] = row + 1
            }

            return heights
        }

        /**
         * Mirror player board on center y-axis
         *
         * @param [board] player board as bit representation
         * @return mirrored board as bit representation
         */
        fun mirrorPlayerBoard(board: Long): Long {
            var mirror = board and 0b111111 shl 42
            mirror = mirror or (board and 0b111111_0000000 shl 28)
            mirror = mirror or (board and 0b111111_0000000_0000000 shl 14)
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

    override fun move(move: Move): ConnectFour {
        assert(move.column in 0..6) { "Invalid move! Move should be in range of 0-6. Found: ${move.column}}" }
        assert(top and (1L shl this.heights[move.column]) == 0L) { "Invalid move! No more space in given column." }

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
        val insertedAt = this.heights[move.column]
        val newZobristHash = this.storageRecordPrimaryKey xor Minimax.Storage.getZobristHash(insertedAt, this.currentPlayer)

        return ConnectFour(
                board = newBoards,
                currentPlayer = -this.currentPlayer,
                difficulty = this.difficulty,
                storageRecordPrimaryKey = newZobristHash,
                heights = newHeights,
                history = this.history.plusElement(this.board),
                multiplayer = this.multiplayer
        )
    }

    override fun undoMove(number: Int): ConnectFour {
        assert(number <= this.history.size) { "You cannot undo more moves than moves were played!" }

        val nextPlayer: Int = if (number % 2 == 0) this.currentPlayer else -this.currentPlayer

        return ConnectFour(
                board = if (number > 0) this.history[this.history.size - number] else this.board,
                currentPlayer = nextPlayer,
                difficulty = this.difficulty,
                history = this.history.subList(0, this.history.size - number),
                multiplayer = this.multiplayer
        )
    }

    override fun searchInStorage(): Minimax.Storage.Record<Move>? {
        if (this.storageIndex >= 0) {
            val storage = Minimax.Storage.doStorageLookup<Move>(this.storageIndex) // Load storage

            this.getStorageRecordKeys().forEach { storageRecordKey ->
                val key = storageRecordKey()

                if (storage.map.containsKey(key.first)) {
                    val storageRecord = storage.map[key.first]!! // Load from storage

                    // Create new storageRecord based on key
                    val newStorageRecord = key.second(storageRecord)
                    if (newStorageRecord != null) return newStorageRecord
                }
            }
        }

        return null
    }

    override fun getStorageRecordKeys(): List<() -> Pair<Long, (record: Minimax.Storage.Record<Move>) -> Minimax.Storage.Record<Move>?>> {

        /**
         * ##### Symmetries #####
         *
         * Applying the following actions to the board do not change its evaluation
         * but we might have to modify the StorageRecord entry which we return -> second pair value.
         *
         * We encapsulate the keys in functions to avoid unnecessary key calculations.
         * The keys gets calculated when we call the according method.
         *
         * - Inverse board: -1 to 1 and vice versa
         * - Mirror board on center y-Axis
         * - Mirror board and inverse
         */

        // storageRecordPrimaryKey (base)
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
                        Minimax.Storage.Record(this.storageRecordPrimaryKey, storageRecord.move!!, -storageRecord.score, this.currentPlayer)
                    else null
                })

        // mirror and inverse
        val key4 = fun(): Pair<Long, (record: Minimax.Storage.Record<Move>) -> Minimax.Storage.Record<Move>?> =
                Pair(calcZobristHash(longArrayOf(mirrorPlayerBoard(this.board[1]), mirrorPlayerBoard(this.board[0]))), { storageRecord ->
                    if (this.currentPlayer != storageRecord.player) {
                        val newMove = storageRecord.move!!.mirrorYAxis()
                        Minimax.Storage.Record(this.storageRecordPrimaryKey, newMove, -storageRecord.score, this.currentPlayer)
                    } else null
                })

        return listOf(key1, key2, key3, key4)
    }

    /**
     * Check if [getNumberOfPlayedMoves] is 42 or [hasWinner] is true
     *
     * @param [player] set to 1 or -1 to check only for the given player's win in [hasWinner]
     * @return is game over
     */
    override fun isGameOver(player: Int): Boolean = this.hasWinner(player) || this.getNumberOfPlayedMoves() == 42

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

    override fun getNumberOfRemainingMoves(): Int = 42 - this.getNumberOfPlayedMoves()

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


    fun toHTML(): String {

        /**
         * TODO:
         *  - if fourInARow() -> output getWinner()
         *  - output current player (color)
         */

        var res = "<div class='board row mx-auto shadow-lg'>"

        for (i in 47 downTo 5 step 7) {
            res += "<div class='column col-auto' data-column='${i / 7}'>"
            for (k in i downTo i - 5) {
                val playerColor = if (1L shl k and board[0] != 0L) "red" else if (1L shl k and board[1] != 0L) "yellow" else ""
                res += "<div class='stone $playerColor'></div>"
            }
            res += "</div>"
        }

        res += "</div>"

        return res
    }

    /**
     * Perform best possible move for current player
     *
     * @return game with applied best move
     */
    fun bestMove(): ConnectFour = this.move(if (this.difficulty == 0) this.getRandomMove() else this.minimax().move!!)


    /**
     * Get winner of the game
     *
     * @return player 1 / -1 or 0 if draw
     */
    fun getWinner(): Int {
        assert(this.isGameOver()) { "The game has not ended yet! There is no winner." }
        return if (this.hasWinner()) return -this.currentPlayer else 0
    }

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
        // Defeats - Draws - Wins for current player
        var stats = Triple(0, 0, 0)
        val remainingMoves = this.getNumberOfRemainingMoves()

        // Simulate games
        //    runBlocking {
        //        repeat(numberOfSimulations) {
        //            launch {
        for (i in 1..numberOfSimulations) {
            // Apply only the essential properties
            var game = ConnectFour(
                    board = board.clone(),
                    currentPlayer = currentPlayer,
                    heights = heights.clone(),
                    storageRecordPrimaryKey = 1 // Prevent to calculate zobrist-hash
            )

            // Play random moves until game is over
            // Factor: the earlier the game has ended, the better is the move
            var factor = remainingMoves
            while (!game.isGameOver()) {
                game = game.move(game.getRandomMove())
                factor--
            }

            // Update stats based on winner
            when (game.getWinner()) {
                -currentPlayer -> stats = Triple(stats.first + 1 + 5 * factor, stats.second, stats.third)
                0 -> stats = Triple(stats.first, stats.second + 1, stats.third)
                currentPlayer -> stats = Triple(stats.first, stats.second, stats.third + 1 + 5 * factor)
            }
            //         }
            //     }
        }

        return (this.currentPlayer * (stats.third - stats.first)).toFloat()
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

    /**
     * Get number of played moves of both players
     *
     * @return number of played moves
     */
    private fun getNumberOfPlayedMoves(): Int {
        var count = 0
        for (i in 0..47)
            if ((1L shl i and this.board[0] != 0L) || (1L shl i and this.board[1] != 0L))
                count++
        return count
    }
}