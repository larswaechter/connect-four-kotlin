package connect.four

import kotlin.math.*

/**
 * Helper method to deeply copy matrix
 *
 * @return deeply copied matrix
 */
fun Array<IntArray>.copyMatrix(): Array<IntArray> = Array(this.size) { get(it).clone() }

/**
 * Helper method to mirror matrix on center Y axis
 *
 * @return mirrored matrix
 */
fun Array<IntArray>.mirrorYAxis(): Array<IntArray> {
    val newArr = this.copyMatrix()
    for (col in 0 until floor(newArr.size.toDouble() / 2).toInt()) {
        for (row in newArr[col].indices) {
            val tmp = newArr[col][row]
            newArr[col][row] = newArr[newArr.size - 1 - col][row]
            newArr[newArr.size - 1 - col][row] = tmp
        }
    }
    return newArr
}

/**
 * Helper method to inverse matrix
 *
 * @return inversed matrix
 */
fun Array<IntArray>.inverseMatrix(): Array<IntArray> = Array(this.size) { get(it).clone().map { n -> -n }.toIntArray() }

/**
 * Class that represents a connect-four game
 *
 * @param [board] board 7x6 matrix
 * @param [currentPlayer] current player
 * @param [difficulty] AI strength
 * @param [history] history of boards
 * @param [multiplayer] playing vs ai or another human player
 */
class ConnectFour(
        override val board: Array<IntArray> = Array(7) { IntArray(6) },
        override val currentPlayer: Int = 1,
        override val difficulty: Int = 10,
        private val history: List<Array<IntArray>> = listOf(),
        val multiplayer: Boolean = false) : Minimax<Array<IntArray>, Move> {

    // Storage index based on number of played moves -> In steps of three
    override val storageIndex = ceil((this.getNumberOfPlayedMoves().toDouble() / 3)).toInt() - 1
    override val storageRecordPrimaryKey: Int = this.board.contentDeepHashCode()

    companion object {
        /**
         * Play exactly n given random moves with ending up in a draw position.
         * This method is mainly used to create different random game positions for the transposition tables.
         *
         * @param [n] number of moves to play
         * @param [startingPlayer] starting player
         * @return game with n played moves
         */
        fun playRandomMoves(n: Int): ConnectFour {
            var game = ConnectFour()
            for (i in 1..n) {
                game = game.move(game.getRandomMove())
                if (game.fourInARow()) return playRandomMoves(n)
            }
            return game
        }
    }

    override fun move(move: Move): ConnectFour {
        assert(move.column in 0..6)
        assert(!this.isColFull(move.column))

        val newBoard: Array<IntArray> = this.board.copyMatrix()
        val row = this.board[move.column].indexOfLast { n -> n == 0 }
        newBoard[move.column][row] = this.currentPlayer

        return ConnectFour(
                newBoard,
                -currentPlayer,
                this.difficulty,
                this.history.plusElement(this.board),
                this.multiplayer
        )
    }

    override fun undoMove(number: Int): ConnectFour {
        assert(number <= this.history.size)

        val nextPlayer: Int = if (number % 2 == 0) this.currentPlayer else -this.currentPlayer

        return ConnectFour(
                if(number > 0) this.history[this.history.size - number] else this.board,
                nextPlayer,
                this.difficulty,
                this.history.subList(0, this.history.size - number),
                this.multiplayer
        )
    }

    override fun getStorageRecordKeys(): List<Pair<Int, (record: Minimax.Storage.Record<Move>) -> Minimax.Storage.Record<Move>?>> {

        /**
         * Applying the following actions to the board do not change its evaluation
         * but we might have to modify the StorageRecord entry which we return -> second pair value
         *
         * - Inverse board: -1 to 1 and vice versa
         * - Mirror board on center y-Axis
         * - Mirror board and inverse
         */

        // PrimaryRecordKey
        val key1: Pair<Int, (record: Minimax.Storage.Record<Move>) -> Minimax.Storage.Record<Move>?> =
                Pair(this.storageRecordPrimaryKey, { storageRecord ->
                    if (this.currentPlayer == storageRecord.player) storageRecord
                    else null
                })

        val boardMirrored = this.board.mirrorYAxis()

        // Mirror -> We also have to mirror the move
        val key2: Pair<Int, (record: Minimax.Storage.Record<Move>) -> Minimax.Storage.Record<Move>?> =
                Pair(boardMirrored.contentDeepHashCode(), { storageRecord ->
                    if (this.currentPlayer == storageRecord.player) {
                        val newMove = storageRecord.move!!.mirrorYAxis()
                        Minimax.Storage.Record(storageRecord.key, newMove, storageRecord.score, storageRecord.player)
                    } else null
                })

        // Inverse -> We have to inverse the score and player
        val key3: Pair<Int, (record: Minimax.Storage.Record<Move>) -> Minimax.Storage.Record<Move>?> =
                Pair(this.board.inverseMatrix().contentDeepHashCode(), { storageRecord ->
                    if (this.currentPlayer != storageRecord.player)
                        Minimax.Storage.Record(storageRecord.key, storageRecord.move!!, -storageRecord.score, -storageRecord.player)
                    else null
                })

        // Mirror and Inverse -> We also have to mirror the move and inverse the score and player
        val key4: Pair<Int, (record: Minimax.Storage.Record<Move>) -> Minimax.Storage.Record<Move>?> =
                Pair(boardMirrored.inverseMatrix().contentDeepHashCode(), { storageRecord ->
                    if (this.currentPlayer != storageRecord.player) {
                        val newMove = storageRecord.move!!.mirrorYAxis()
                        Minimax.Storage.Record(storageRecord.key, newMove, -storageRecord.score, -storageRecord.player)
                    } else null
                })

        return listOf(key1, key2, key3, key4)
    }

    override fun isGameOver(): Boolean = this.fourInARow() || this.getNumberOfPlayedMoves() == 42

    override fun hasWinner(): Boolean = this.fourInARow()

    override fun evaluate(depth: Int): Float = this.mcm()

    override fun getPossibleMoves(shuffle: Boolean): List<Move> {
        val possibleMoves: MutableList<Move> = mutableListOf()
        for (col in this.board.indices) if (!this.isColFull(col)) possibleMoves.add(Move(col))
        return if (shuffle) possibleMoves.shuffled() else possibleMoves.toList()
    }

    override fun getNumberOfRemainingMoves(): Int = 42 - this.getNumberOfPlayedMoves()

    override fun toString(): String {
        var res = ""

        for (i in 0..5) {
            for (j in 0..6) res += this.board[j][i].toString() + "\t"
            res += "\n"
        }

        return res
    }

    fun getNumberOfPlayedMoves() = this.history.size

    fun toHTML(): String {
        var res = "<div class='board row mx-auto shadow-lg'>"

        for (i in 0..6) {
            res += "<div class='column col-auto' data-column='$i'>"
            for (j in 0..5) {
                val playerColor = when (this.board[i][j]) {
                    1 -> "red"
                    -1 -> "yellow"
                    else -> ""
                }
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
     * Check if four chips of the same player are in one row.
     * Vertically, horizontally and diagonal.
     *
     * @return if four in a row
     */
    fun fourInARow(): Boolean {
        if (this.getNumberOfPlayedMoves() < 7) return false

        // Check vertically
        for (row in this.board.indices) {
            for (col in 0 until this.board[row].size - 3) {
                val sum = abs(this.board[row][col] + this.board[row][col + 1] + this.board[row][col + 2] + this.board[row][col + 3])
                if (sum == 4) return true
            }
        }

        // Check horizontally
        for (col in this.board[0].indices) {
            for (row in 0 until this.board.size - 3) {
                val sum = abs(this.board[row][col] + this.board[row + 1][col] + this.board[row + 2][col] + this.board[row + 3][col])
                if (sum == 4) return true
            }
        }

        // Check diagonal top-right to bottom-left
        for (col in 3 until this.board.size) {
            for (row in 0 until this.board[col].size - 3) {
                val sum = abs(this.board[col][row] + this.board[col - 1][row + 1] + this.board[col - 2][row + 2] + this.board[col - 3][row + 3])
                if (sum == 4) return true
            }
        }

        // Check diagonal top-left to bottom-right
        for (col in 3 until this.board.size) {
            for (row in 3 until this.board[col].size) {
                val sum = abs(this.board[col][row] + this.board[col - 1][row - 1] + this.board[col - 2][row - 2] + this.board[col - 3][row - 3])
                if (sum == 4) return true
            }
        }

        return false
    }

    /**
     * Monte Carlo method for board evaluation.
     * Simulate a number of random games and evaluate based on the number of wins
     *
     * @param [numberOfSimulations] how many games to play
     * @return number of wins
     */
    fun mcm(numberOfSimulations: Int = 200): Float {
        // Defeats - Draws - Wins for current player
        var stats = Triple(0, 0, 0)

        // Simulate 100 games
        for (i in 1..numberOfSimulations) {
            var game = ConnectFour(
                    board = this.board.copyMatrix(),
                    currentPlayer = this.currentPlayer,
                    difficulty = this.difficulty,
                    multiplayer = this.multiplayer
            )

            // Play random moves until game is over
            while (!game.isGameOver()) game = game.move(game.getRandomMove())

            // Update stats based on winner
            when (game.getWinner()) {
                -this.currentPlayer -> stats = Triple(stats.first + 1, stats.second, stats.third)
                0 -> stats = Triple(stats.first, stats.second + 1, stats.third)
                this.currentPlayer -> stats = Triple(stats.first, stats.second, stats.third + 1)
            }
        }

        // println(stats.third)

        // Return number of wins for current player
        return (this.currentPlayer * stats.third).toFloat()
    }

    /**
     * Get winner of the game
     *
     * @return player 1 / -1 or 0 if draw
     */
    private fun getWinner(): Int {
        assert(this.isGameOver())
        return if (this.fourInARow()) return -this.currentPlayer else 0
    }

    /**
     * Check if the given column is full
     *
     * @param [col] index of column in matrix
     * @return if column is ful
     */
    private fun isColFull(col: Int): Boolean = !this.board[col].contains(0)
}