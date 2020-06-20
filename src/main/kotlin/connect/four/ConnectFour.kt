package connect.four

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

// Helper method to deeply copy matrix
fun Array<IntArray>.copyMatrix() = Array(size) { get(it).clone() }

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

    override fun getStorageRecordKey(): Int = this.board.contentDeepHashCode()

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