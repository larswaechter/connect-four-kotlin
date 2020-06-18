package connect.four

class ConnectFour(
        val board: Array<IntArray> = Array(7) { IntArray(6) },
        val currentPlayer: Int = 1) {

    fun move(move: Move): ConnectFour {
        assert(move.column in 0..6)
        assert(!this.isColFull(move.column))

        val newBoard = this.board.clone()
        val row = this.board[move.column].indexOfLast { n -> n == 0 }
        newBoard[move.column][row] = this.currentPlayer

        return ConnectFour(newBoard, -currentPlayer)
    }

    fun getRandomMove(): Move = this.getPossibleMoves().random()

    fun getPossibleMoves(): List<Move> {
        val possibleMoves: MutableList<Move> = mutableListOf()
        for (col in this.board.indices) if (!this.isColFull(col)) possibleMoves.add(Move(col))
        return possibleMoves.toList()
    }

    fun isColFull(col: Int): Boolean = !this.board[col].contains(0)

    fun isGameOver(): Boolean = !this.board.any { n -> n.any { m -> m == 0 } }

    override fun toString(): String {
        var res = ""

        for (i in 0..5) {
            for (j in 0..6) res += this.board[j][i].toString() + "\t"
            res += "\n"
        }

        return res;
    }
}