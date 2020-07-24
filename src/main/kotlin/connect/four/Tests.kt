package connect.four

class Tests {
    init {
        println("\nStarting tests!")
        this.runTest1()
        this.runTest2()
        this.runTest3()
        this.runTest4()
        this.runTest5()
        println("Tests finished!")
    }

    /**
     * "Die Spiel-Engine kann im nächsten Zug gewinnen (Sichttiefe 1)"
     *
     * It's player X's turn:
     * He has to play in column 0 to win the game
     *
     *      . . . . . . .
     *      . . . . . . .
     *      . . . . . . .
     *      X . . . . . .
     *      X . . . . . .
     *      X . . . O O O
     *
     */
    private fun runTest1() {
        println("Running test #1...")

        val board = longArrayOf(
                0b0_0000000_0000000___0000000_0000000_0000000_0000000_0000000_0000000_0000111, // X
                0b0_0000000_0000000___0000001_0000001_0000001_0000000_0000000_0000000_0000000 // O
        )

        var game = ConnectFour(board = board)

        // player X should play win-move
        game = game.bestMove()

        assert(game.getWinner() == 1) {"Failed test #1: Player 1 (X) should have won!"}
    }

    /**
     * "Die Spiel-Engine kann im übernächsten Zug gewinnen (Sichttiefe 3)"
     *
     * It's player X's turn:
     * He has to play in column 1 or 4 to win his his next move
     *
     *      . . . . . . .
     *      . . . . . . .
     *      . . . . . . .
     *      . . . . . . .
     *      . . . . . . O
     *      . . X X . . O
     *
     */
    private fun runTest2() {
        println("Running test #2...")

        val board = longArrayOf(
                0b0_0000000_0000000___0000000_0000000_0000000_0000001_0000001_0000000_0000000, // X
                0b0_0000000_0000000___0000011_0000000_0000000_0000000_0000000_0000000_0000000 // O
        )

        var game = ConnectFour(board = board)

        // player X plays move
        game = game.bestMove()

        // player O plays move
        game = game.bestMove()

        // player X should play win-move
        game = game.bestMove()

        assert(game.getWinner() == 1) {"Failed test #2: Player 1 (X) should have won!"}
    }

    // Die Spiel-Engine kann im überübernächsten Zug gewinnen (Sichttiefe 5)
    private fun runTest3() {
        println("Running test #3...")
    }

    /**
     * "Die Spiel-Engine vereitelt eine unmittelbare Gewinnbedrohung des Gegners (Sichttiefe 2)"
     *
     * It's player O's turn:
     * He has to play in column 0 otherwise player X can win in his next move
     *
     *      . . . . . . .
     *      . . . . . . .
     *      . . . . . . .
     *      X . . . . . .
     *      X . . . . . .
     *      X O O . . . .
     *
     */
    private fun runTest4() {
        println("Running test #4...")

        val board = longArrayOf(
                0b0_0000000_0000000___0000000_0000000_0000000_0000000_0000000_0000000_0000111, // X
                0b0_0000000_0000000___0000000_0000000_0000000_0000000_0000001_0000001_0000000 // O
        )

        var game = ConnectFour(board = board, currentPlayer = -1)

        // player O plays move -> prevent player X from win-move
        game = game.bestMove()

        // player X cannot play win-move
        game = game.bestMove()

        assert(!game.hasWinner(1)) {"Failed test #1: Player 1 (X) should not have won!"}
    }


    /**
     * "Die Spiel-Engine vereitelt ein Drohung, die den Gegner im übernächsten Zug ansonsten einen Gewinn umsetzen lässt (Sichttiefe 4)"
     *
     * It's player O's turn:
     * He has to play in column 1 or 4 otherwise player X can definitely win
     *
     *      . . . . . . .
     *      . . . . . . .
     *      . . . . . . .
     *      . . . . . . .
     *      . . . . . . .
     *      . . X X . . O
     *
     */
    private fun runTest5() {
        println("Running test #5...")

        val board = longArrayOf(
                0b0_0000000_0000000___0000000_0000000_0000000_0000001_0000001_0000000_0000000, // X
                0b0_0000000_0000000___0000001_0000000_0000000_0000000_0000000_0000000_0000000 // O
        )

        var game = ConnectFour(board = board, currentPlayer = -1)

        // player O plays move -> player X cannot play win-move in his next-next move
        game = game.bestMove()

        // player X plays move
        game = game.bestMove()

        // player O plays move
        game = game.bestMove()

        // player X plays move
        game = game.bestMove()

        assert(!game.hasWinner(1)) {"Failed test #5: Player 1 (X) should not have won"}
    }
}