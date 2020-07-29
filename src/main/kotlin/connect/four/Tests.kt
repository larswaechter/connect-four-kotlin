package connect.four

class Tests {
    private var successCounter: Int = 0

    init {
        println("\n----- Starting tests -----")
        this.runTest1()
        this.runTest2()
        this.runTest3()
        this.runTest4()
        this.runTest5()
        println("Tests completed! Finished ${this.successCounter} / 5 successfully.")
    }

    private fun printMove(player: String, move: Move) {
        println("$player is playing: $move")
    }

    /**
     *      . . . . . . .
     *      . . . . . . .
     *      . . . . . . .
     *      X . . . . . .
     *      X . . . . . .
     *      X . . . O O O
     *
     */
    private fun runTest1() {
        println("\nRunning test #1...")

        val board = longArrayOf(
                0b0_0000000_0000000___0000000_0000000_0000000_0000000_0000000_0000000_0000111, // X
                0b0_0000000_0000000___0000001_0000001_0000001_0000000_0000000_0000000_0000000 // O
        )

        var game = ConnectFour(board = board)
        println(game)

        // player X should play win-move
        val bestMove = game.minimax().move!!
        printMove(game.getPlayerSymbol(), bestMove)
        game = game.move(bestMove)
        println(game)

        if (game.getWinner() == 1) {
            this.successCounter++
            println("Success: Player X has won!")
        } else println("Error: Player X should have won!")

        println("----------------------------------------------")
    }

    /**
     *      . . . . . . .
     *      . . . . . . .
     *      . . . . . . .
     *      . . . . . . .
     *      . . . . . . O
     *      . . X X . . O
     *
     */
    private fun runTest2() {
        println("\nRunning test #2...")

        val board = longArrayOf(
                0b0_0000000_0000000___0000000_0000000_0000000_0000001_0000001_0000000_0000000, // X
                0b0_0000000_0000000___0000011_0000000_0000000_0000000_0000000_0000000_0000000 // O
        )

        var game = ConnectFour(board = board)
        println(game)

        // player X plays move
        val move1 = game.minimax().move!!
        printMove(game.getPlayerSymbol(), move1)
        game = game.move(move1)
        println(game)

        // player O plays move
        val move2 = game.minimax().move!!
        printMove(game.getPlayerSymbol(), move2)
        game = game.move(move2)
        println(game)

        // player X should play win-move
        val move3 = game.minimax().move!!
        printMove(game.getPlayerSymbol(), move3)
        game = game.move(move3)
        println(game)

        if (game.getWinner() == 1) {
            this.successCounter++
            println("Success: Player X has won!")
        } else println("Error: Player X should have won!")

        println("----------------------------------------------")
    }

    private fun runTest3() {
        println("Running test #3...")
        println("empty")
        println("----------------------------------------------")
    }

    /**
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
        println(game)

        // player O plays move -> prevent player X from win-move
        val move1 = game.minimax().move!!
        printMove(game.getPlayerSymbol(), move1)
        game = game.move(move1)
        println(game)

        // player X cannot play win-move
        val move2 = game.minimax().move!!
        printMove(game.getPlayerSymbol(), move2)
        game = game.move(move2)
        println(game)

        if (!game.hasWinner(1)) {
            this.successCounter++
            println("Success: Player X has not won!")
        } else println("Error: Player X should not have won!")

        println("----------------------------------------------")
    }


    /**
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
        println(game)

        // player O plays move -> player X cannot play win-move in his next-next move
        val move1 = game.minimax().move!!
        printMove(game.getPlayerSymbol(), move1)
        game = game.move(move1)
        println(game)

        // player X plays move
        val move2 = game.minimax().move!!
        printMove(game.getPlayerSymbol(), move2)
        game = game.move(move2)
        println(game)

        // player O plays move
        val move3 = game.minimax().move!!
        printMove(game.getPlayerSymbol(), move3)
        game = game.move(move3)
        println(game)

        // player X plays move
        val move4 = game.minimax().move!!
        printMove(game.getPlayerSymbol(), move4)
        game = game.move(move4)
        println(game)

        if (!game.hasWinner(1)) {
            this.successCounter++
            println("Success: Player X has not won!")
        } else println("Error: Player X should not have won!")

        println("----------------------------------------------")
    }
}