package connect.four

class Tests {
    init {
        this.runTest1()
        this.runTest2()
        this.runTest3()
        this.runTest4()
        this.runTest5()
    }

    // Die Spiel-Engine kann im nächsten Zug gewinnen (Sichttiefe 1)
    private fun runTest1() {
        println("Running test #1...")

        val board: Array<IntArray> = Array(7) { IntArray(6) }
        board[0][5] = 1
        board[0][4] = 1
        board[0][3] = 1

        board[1][5] = -1
        board[2][5] = -1
        board[3][5] = -1

        var game = ConnectFour(board)

        // player 1 should play win-move
        game = game.bestMove()

        assert(game.getWinner() == 1)
    }

    // Die Spiel-Engine kann im übernächsten Zug gewinnen (Sichttiefe 3)
    private fun runTest2() {
        println("Running test #2...")

        val board: Array<IntArray> = Array(7) { IntArray(6) }
        board[2][5] = 1
        board[3][5] = 1
        board[6][5] = -1
        board[6][4] = -1

        var game = ConnectFour(board)

        // player 1 plays move
        game = game.bestMove()

        // player -1 plays move
        game = game.bestMove()

        // player 1 should play win-move
        game = game.bestMove()

        assert(game.getWinner() == 1)
    }

    // Die Spiel-Engine kann im überübernächsten Zug gewinnen (Sichttiefe 5)
    private fun runTest3() {
        println("Running test #3...")
    }

    // Die Spiel-Engine vereitelt eine unmittelbare Gewinnbedrohung des Gegners (Sichttiefe 2)
    private fun runTest4() {
        println("Running test #4...")

        val board: Array<IntArray> = Array(7) { IntArray(6) }
        board[0][5] = 1
        board[0][4] = 1
        board[0][3] = 1

        board[1][5] = -1
        board[2][5] = -1

        var game = ConnectFour(board = board, currentPlayer = -1)

        // player -1 plays move
        game = game.bestMove()

        assert(!game.hasWinner())
    }

    // Die Spiel-Engine vereitelt ein Drohung, die den Gegner im übernächsten Zug ansonsten einen Gewinn umsetzen lässt (Sichttiefe 4)
    private fun runTest5() {
        println("Running test #5...")

        val board: Array<IntArray> = Array(7) { IntArray(6) }
        board[2][5] = 1
        board[3][5] = 1
        board[6][5] = -1

        var game = ConnectFour(board, currentPlayer = -1)

        // player -1 plays move
        game = game.bestMove()

        // player 1 plays move
        game = game.bestMove()

        // player -1 plays move
        game = game.bestMove()

        // player 1 plays move
        game = game.bestMove()

        assert(!game.hasWinner())
    }
}