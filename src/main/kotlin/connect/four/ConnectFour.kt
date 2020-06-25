package connect.four

import kotlin.math.*

/**
 * Helper method to deeply copy matrix
 *
 * @return deeply copied matrix
 */
fun Array<IntArray>.copyMatrix(): Array<IntArray> = Array(size) { get(it).clone() }

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
fun Array<IntArray>.inverseMatrix(): Array<IntArray> = Array(size) { get(it).clone().map { n -> -n }.toIntArray() }


class ConnectFour(
        override val board: Array<IntArray> = Array(7) { IntArray(6) },
        override val currentPlayer: Int = 1,
        override val difficulty: Int = 8,
        private val numberOfPlayedMoves: Int = 0) : Minimax<Array<IntArray>, Move> {

    override val storageIndex = ceil((this.numberOfPlayedMoves.toDouble() / 3)).toInt() - 1
    override val storageRecordPrimaryKey: Int = this.board.contentDeepHashCode()

    companion object {
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

        return ConnectFour(newBoard, -currentPlayer, this.difficulty, this.numberOfPlayedMoves + 1)
    }

    // override fun getStorageIndex(): Int = ceil((this.numberOfPlayedMoves.toDouble() / 3)).toInt() - 1

    override fun getStorageRecordKeys(): List<Pair<Int, (move: Move) -> Move>> {

        /**
         * Applying the following actions to the board do not change its evaluation
         * but the Move might change, so we also return a move-transform method:
         *
         * - Inverse board: -1 to 1 and vice versa
         * - Mirror board on center y-Axis
         * - Mirror board and inverse
         */

        // baseRecordKey
        val key1: Pair<Int, (move: Move) -> Move> = Pair(this.storageRecordPrimaryKey, { move -> move })

        // Inverse
        val key2: Pair<Int, (move: Move) -> Move> = Pair(this.board.inverseMatrix().contentDeepHashCode(), { move -> move })

        val boardMirrored = this.board.mirrorYAxis()

        // Mirror
        val key3: Pair<Int, (move: Move) -> Move> = Pair(boardMirrored.contentDeepHashCode(), { move -> move.mirrorYAxis() })

        // Mirror and Inverse -> We also have to mirror the move
        val key4: Pair<Int, (move: Move) -> Move> = Pair(boardMirrored.inverseMatrix().contentDeepHashCode(), { move -> move.mirrorYAxis() })

        return listOf(key1, key2, key3, key4)
    }

    override fun isGameOver(): Boolean = this.fourInARow() || this.getNumberOfRemainingMoves() == 0

    override fun hasWinner(): Boolean = this.fourInARow()

    override fun evaluate(depth: Int): Float {
        var bestScore = 0F

        /**
         * Calculate board evaluation score based on number of chips in a row and already existing best score
         * - 4 in a row: 200 (see below)
         * - 3 in a row: 100
         * - 2 in a row:  50
         *
         * @params [sum] number of chips in a row
         * @return score
         */
        fun calcScore(sum: Int): Float {
            val newScore = when (sum) {
                3 -> (if (sum == abs(sum) * this.currentPlayer) this.currentPlayer else -this.currentPlayer) * 100F
                2 -> (if (sum == abs(sum) * this.currentPlayer) this.currentPlayer else -this.currentPlayer) * 50F
                else -> 0F
            }

            return if (this.currentPlayer == 1) max(newScore, bestScore) else min(newScore, bestScore)
        }

        // Evaluate vertically
        for (row in this.board.indices) {
            for (col in 0 until this.board[row].size - 3) {
                val sum = this.board[row][col] + this.board[row][col + 1] + this.board[row][col + 2] + this.board[row][col + 3]
                val sumAbs = abs(sum)
                if (sumAbs == 4)
                    return (if (sum == sumAbs * this.currentPlayer) this.currentPlayer else -this.currentPlayer) * Minimax.maxBoardEvaluationScore
                bestScore = calcScore(sum)
            }
        }

        // Evaluate horizontally
        for (col in this.board[0].indices) {
            for (row in 0 until this.board.size - 3) {
                val sum = this.board[row][col] + this.board[row + 1][col] + this.board[row + 2][col] + this.board[row + 3][col]
                val sumAbs = abs(sum)
                if (sumAbs == 4)
                    return (if (sum == sumAbs * this.currentPlayer) this.currentPlayer else -this.currentPlayer) * Minimax.maxBoardEvaluationScore
                bestScore = calcScore(sum)
            }
        }

        // Evaluate diagonal top-right to bottom-left
        for (col in 3 until this.board.size) {
            for (row in 0 until this.board[col].size - 3) {
                val sum = this.board[col][row] + this.board[col - 1][row + 1] + this.board[col - 2][row + 2] + this.board[col - 3][row + 3]
                val sumAbs = abs(sum)
                if (sumAbs == 4)
                    return (if (sum == sumAbs * this.currentPlayer) this.currentPlayer else -this.currentPlayer) * Minimax.maxBoardEvaluationScore
                bestScore = calcScore(sum)
            }
        }

        // Evaluate diagonal top-left to bottom-right
        for (col in 3 until this.board.size) {
            for (row in 3 until this.board[col].size) {
                val sum = this.board[col][row] + this.board[col - 1][row - 1] + this.board[col - 2][row - 2] + this.board[col - 3][row - 3]
                val sumAbs = abs(sum)
                if (sumAbs == 4)
                    return (if (sum == sumAbs * this.currentPlayer) this.currentPlayer else -this.currentPlayer) * Minimax.maxBoardEvaluationScore
                bestScore = calcScore(sum)
            }
        }

        return bestScore
    }

    override fun getPossibleMoves(): List<Move> {
        val possibleMoves: MutableList<Move> = mutableListOf()
        for (col in this.board.indices) if (!this.isColFull(col)) possibleMoves.add(Move(col))
        return possibleMoves.toList()
    }

    override fun getNumberOfRemainingMoves(): Int = this.board.sumBy { col -> col.count { cell -> cell == 0 } }

    override fun toString(): String {
        var res = ""

        for (i in 0..5) {
            for (j in 0..6) res += this.board[j][i].toString() + "\t"
            res += "\n"
        }

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
        if(this.numberOfPlayedMoves < 4) return false

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
     * Check if the given column is full
     *
     * @param [col] index of column in matrix
     * @return if column is ful
     */
    private fun isColFull(col: Int): Boolean = !this.board[col].contains(0)
}