package connect.four

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

// Helper method to deeply copy matrix
fun Array<IntArray>.copyMatrix() = Array(size) { get(it).clone() }

class ConnectFour(
        override val board: Array<IntArray> = Array(7) { IntArray(6) },
        override val currentPlayer: Int = 1) : Minimax<Move> {

    override fun move(move: Move): ConnectFour {
        assert(move.column in 0..6)
        assert(!this.isColFull(move.column))

        val newBoard: Array<IntArray> = this.board.copyMatrix()
        val row = this.board[move.column].indexOfLast { n -> n == 0 }
        newBoard[move.column][row] = this.currentPlayer

        return ConnectFour(newBoard, -currentPlayer)
    }

    fun bestMove(): ConnectFour {
        val bestMove = this.minimax()
        return this.move(bestMove.first!!)
    }

    /**
     * Play exactly n given random moves with ending up in a draw position
     * This method is mainly used to create different random game positions for the transposition tables
     *
     * @param [n] number of moves to play
     * @return game with n played moves
     */
    fun playRandomMoves(n: Int): ConnectFour {
        outer@
        while (true) {
            var game = ConnectFour(board = this.board.copyMatrix(), currentPlayer = this.currentPlayer)
            for (i in 1..n) {
                val beforeMove = game
                val possibleMoves: List<Move> = beforeMove.getPossibleMoves()
                do game = beforeMove.move(beforeMove.getRandomMove(possibleMoves))
                while (possibleMoves.size > 1 && game.fourInARow())
            }
            return game
        }
    }

    /**
     * Check if no more moves are possible or a player has one
     *
     * @return is game over
     */
    override fun isGameOver(): Boolean = !this.board.any { n -> n.any { m -> m == 0 } }

    /**
     * Get index in storage based on played moves
     *
     * @return storage index
     */
    override fun getStorageIndex(): Int = Storage.getStorageIndexFromPlayedMoves(this.getNumberOfPlayedMoves())

    override fun evaluate(depth: Int): Float {
        var bestScore = 0F

        // Check vertically
        for (row in this.board.indices) {
            for (col in 0 until this.board[row].size - 3) {
                val sum = this.board[row][col] + this.board[row][col + 1] + this.board[row][col + 2] + this.board[row][col + 3]
                val sumAbs = abs(sum)
                if (sumAbs == 4)
                    return (if (sum == sumAbs * this.currentPlayer) this.currentPlayer else -this.currentPlayer) * Float.POSITIVE_INFINITY
                bestScore = this.calcScore(sum, bestScore)
            }
        }

        // Check horizontally
        for (col in this.board[0].indices) {
            for (row in 0 until this.board.size - 3) {
                val sum = this.board[row][col] + this.board[row + 1][col] + this.board[row + 2][col] + this.board[row + 3][col]
                val sumAbs = abs(sum)
                if (sumAbs == 4)
                    return (if (sum == sumAbs * this.currentPlayer) this.currentPlayer else -this.currentPlayer) * Float.POSITIVE_INFINITY
                bestScore = this.calcScore(sum, bestScore)
            }
        }

        return this.currentPlayer * bestScore
    }

    private fun fourInARow(): Boolean {
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

    override fun getPossibleMoves(): List<Move> {
        val possibleMoves: MutableList<Move> = mutableListOf()
        for (col in this.board.indices) if (!this.isColFull(col)) possibleMoves.add(Move(col))
        return possibleMoves.toList()
    }

    override fun getNumberOfRemainingMoves(): Int = this.board.sumBy { col -> col.count { cell -> cell == 0 } }

    private fun getNumberOfPlayedMoves(): Int = this.board.sumBy { col -> col.count { cell -> cell != 0 } }

    private fun isColFull(col: Int): Boolean = !this.board[col].contains(0)

    override fun toString(): String {
        var res = ""

        for (i in 0..5) {
            for (j in 0..6) res += this.board[j][i].toString() + "\t"
            res += "\n"
        }

        return res;
    }
}