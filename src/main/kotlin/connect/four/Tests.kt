package connect.four

class Tests {
    init {
        this.runTest1()
        this.runTest2()
        this.runTest3()
        this.runTest4()
        this.runTest5()
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

        var game = ConnectFour(
                board = board,
                heights = intArrayOf(3, 7, 14, 21, 29, 36, 43)
        )

        // player 1 should play win-move
        game = game.bestMove()

        assert(game.getWinner() == 1)
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

        var game = ConnectFour(
                board = board,
                heights = intArrayOf(0, 7, 15, 22, 28, 35, 44)
        )

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

        var game = ConnectFour(
                board = board,
                heights = intArrayOf(3, 8, 15, 21, 28, 35, 42),
                currentPlayer = -1
        )

        // player -1 plays move -> prevent player 1 from getting 4-in-a-row
        game = game.bestMove()

        // player 1 cannot win any more with this move
        game = game.bestMove()

        assert(!game.hasWinner())
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

        var game = ConnectFour(
                board = board,
                heights = intArrayOf(0, 7, 15, 22, 28, 35, 43),
                currentPlayer = -1
        )

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