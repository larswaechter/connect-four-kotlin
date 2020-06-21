package connect.four

import kotlin.math.*

// Helper method to deeply copy matrix
fun Array<IntArray>.copyMatrix(): Array<IntArray> = Array(size) { get(it).clone() }

// Helper method to mirror matrix on center Y axis
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

// Helper method to inverse matrix
fun Array<IntArray>.inverseMatrix(): Array<IntArray> = Array(size) { get(it).clone().map { n -> -n }.toIntArray() }

class ConnectFour(
        override val board: Array<IntArray> = Array(7) { IntArray(6) },
        override val currentPlayer: Int = 1) : Minimax<Array<IntArray>, Move> {

    companion object {
        /**
         * Play exactly n given random moves with ending up in a draw position
         * This method is mainly used to create different random game positions for the transposition tables
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

        return ConnectFour(newBoard, -currentPlayer)
    }

    override fun getStorageIndex(): Int = Minimax.Storage.getStorageIndexFromPlayedMoves(this.getNumberOfPlayedMoves())

    override fun getBaseStorageRecordKey(): Int = this.board.contentDeepHashCode()

    override fun getStorageRecordKeys(): List<Pair<Int, (move: Move) -> Move>> {

        /**
         * Applying the following actions to the board do not change the its evaluation
         * but the Move might change, so we also return a move-transform method
         *
         * - Inverse board: -1 to 1 and vice versa
         * - Mirror board on center y-Axis
         * - Mirror board and inverse
         */

        val key1: Pair<Int, (move: Move) -> Move> = Pair(this.getBaseStorageRecordKey(), {move -> move})
        val key2: Pair<Int, (move: Move) -> Move> = Pair(this.board.inverseMatrix().contentDeepHashCode(), {move -> move}) // Inverse

        val boardMirrored = this.board.mirrorYAxis()

        val key3: Pair<Int, (move: Move) -> Move> = Pair(boardMirrored.contentDeepHashCode(), {move -> move.mirrorYAxis()}) // Mirror
        val key4: Pair<Int, (move: Move) -> Move> = Pair(boardMirrored.inverseMatrix().contentDeepHashCode(), {move -> move.mirrorYAxis()}) // Mirror and Inverse

        return listOf(key1, key2, key3, key4)
    }

    override fun isGameOver(): Boolean = this.fourInARow() || this.getNumberOfRemainingMoves() == 0

    override fun evaluate(depth: Int): Float {
        var bestScore = 0F

        // Evaluate vertically
        for (row in this.board.indices) {
            for (col in 0 until this.board[row].size - 3) {
                val sum = this.board[row][col] + this.board[row][col + 1] + this.board[row][col + 2] + this.board[row][col + 3]
                val sumAbs = abs(sum)
                if (sumAbs == 4)
                    return (if (sum == sumAbs * this.currentPlayer) this.currentPlayer else -this.currentPlayer) * 200F
                bestScore = this.calcScore(sum, bestScore)
            }
        }

        // Evaluate horizontally
        for (col in this.board[0].indices) {
            for (row in 0 until this.board.size - 3) {
                val sum = this.board[row][col] + this.board[row + 1][col] + this.board[row + 2][col] + this.board[row + 3][col]
                val sumAbs = abs(sum)
                if (sumAbs == 4)
                    return (if (sum == sumAbs * this.currentPlayer) this.currentPlayer else -this.currentPlayer) * 200F
                bestScore = this.calcScore(sum, bestScore)
            }
        }

        // Evaluate diagonal top-right to bottom-left
        for (col in 3 until this.board.size) {
            for (row in 0 until this.board[col].size - 3) {
                val sum = this.board[col][row] + this.board[col - 1][row + 1] + this.board[col - 2][row + 2] + this.board[col - 3][row + 3]
                val sumAbs = abs(sum)
                if (sumAbs == 4)
                    return (if (sum == sumAbs * this.currentPlayer) this.currentPlayer else -this.currentPlayer) * 200F
                bestScore = this.calcScore(sum, bestScore)
            }
        }

        // Evaluate diagonal top-left to bottom-right
        for (col in 3 until this.board.size) {
            for (row in 3 until this.board[col].size) {
                val sum = this.board[col][row] + this.board[col - 1][row - 1] + this.board[col - 2][row - 2] + this.board[col - 3][row - 3]
                val sumAbs = abs(sum)
                if (sumAbs == 4)
                    return (if (sum == sumAbs * this.currentPlayer) this.currentPlayer else -this.currentPlayer) * 200F
                bestScore = this.calcScore(sum, bestScore)
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

    fun bestMove(): ConnectFour {
        val bestMove = this.minimax()
        return this.move(bestMove.first!!)
    }

    fun fourInARow(): Boolean {
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

    private fun calcScore(sum: Int, bestScore: Float): Float {
        val newScore = when (sum) {
            3 -> (if (sum == abs(sum) * this.currentPlayer) this.currentPlayer else -this.currentPlayer) * 100F
            2 -> (if (sum == abs(sum) * this.currentPlayer) this.currentPlayer else -this.currentPlayer) * 50F
            else -> return 0F
        }

        return if (this.currentPlayer == 1) max(newScore, bestScore) else min(newScore, bestScore)
    }

    private fun getNumberOfPlayedMoves(): Int = this.board.sumBy { col -> col.count { cell -> cell != 0 } }

    private fun isColFull(col: Int): Boolean = !this.board[col].contains(0)
}