/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package connect.four

fun main() {


    val game1 = ConnectFour.playRandomMoves(15)

    val game2 = ConnectFour(
            longArrayOf(
                    ConnectFour.mirrorPlayerBoard(game1.board[0]),
                    ConnectFour.mirrorPlayerBoard(game1.board[1])
            )
    )

    val game3 = ConnectFour(
            longArrayOf(
                    game1.board[0],
                    game1.board[1]
            )
    )

    println(game1.storageRecordPrimaryKey)
    println(game3.storageRecordPrimaryKey)

    println()

    println(game1.board[0].toString())
    println(game1.board[1].toString())
    println(game3.board[0].toString())
    println(game3.board[1].toString())

    println()

    println(game1)

    /*

    println(game1)

    var game2 = ConnectFour()
    game2 = game2.move(Move(0))
    game2 = game2.move(Move(1))
    game2 = game2.move(Move(0))
    game2 = game2.move(Move(1))
    game2 = game2.move(Move(1))


    println(game2)

    println(game1.storageRecordPrimaryKey)
    println(game2.storageRecordPrimaryKey)

     */


    /*

    var game = ConnectFour()
    game = game.move(Move(0))
    game = game.move(Move(1))

    return

    Minimax.Storage.seedByMovesPlayed<Move>(2, 40)

    return

    Tests()

     */

    /*

    val game1 = C4(
            longArrayOf(
                    0b0_0000000_0000000___0000000_0000000_0000000_0000000_0000000_0000000_0000001, // X
                    0b0_0000000_0000000___0000000_0000000_0000000_0000000_0000000_0000001_0000000 // O
            )
    )

    println(game1.storageRecordPrimaryKey)

    var game = C4()
    game = game.move(0)
    game = game.move(1)

    println(game.storageRecordPrimaryKey)

     */


    /*

    var game = C4()
    game = game.move(6)
    game = game.move(6)
    game = game.move(6)
    game = game.move(6)
    game = game.move(6)
    game = game.move(6)

    println(game)

    println(C4.calcZobristHash(game.board))


    var game2 = C4(board = longArrayOf(game.mirror(game.board[0]), game.mirror(game.board[1])))
    println(game2)

     */


    /*

    val board00 = Array(7) { IntArray(6) }
    board00[0][1] = 1

    val board11 = Array(7) { IntArray(6) }
    board11[0][1] = 1

    println(board00.contentHashCode())
    println(board11.contentHashCode())

    return

     */


    /*

    val board1 = Array(7) { IntArray(6) }
    board1[4][5] = -1
    board1[6][5] = -1
    board1[6][4] = -1
    println(board1.contentDeepHashCode())
    println(board1.contentDeepToString().hashCode())
    println(ConnectFour(board1))

    val board2 = Array(7) { IntArray(6) }
    board2[6][5] = -1
    board2[6][4] = -1
    board2[6][3] = -1
    println(board2.contentDeepHashCode())
    println(board2.contentDeepToString().hashCode())
    println(ConnectFour(board2))

    return

     */


    /*

    return

    Server()

    return

    var game = ConnectFour()

    game = game.move(Move(0))
    game = game.move(Move(0))
    game = game.move(Move(0))
    game = game.move(Move(0))

    game = game.undoMove(0)
    game = game.undoMove(2)

    println(game)

    println(game.getNumberOfPlayedMoves())

    // Minimax.Storage.seedByMovesPlayed<Move>(5000, 40)

     */

    /*

    val game = ConnectFour.playRandomMoves(28)

    println(game.currentPlayer)
    println(game)

    println(game.minimax())

     */

    // Minimax.Storage.seedByMovesPlayed(5000, 40)

    // val bestMove = ConnectFour.playRandomMoves(40).mcm()


    // Server()

    // train()


    // Minimax.Storage.seedByMovesPlayed(5, 40)
    // Minimax.Storage.seedByMovesPlayed(2000, 40)


    // println(ConnectFour(difficulty = 42).bestMove())

    /*

    val start = System.currentTimeMillis()

    val game = ConnectFour.playRandomMoves(1)
    println(game.bestMove())
    println((System.currentTimeMillis() - start) / 1000)

     */


}

fun train() {
    for (i in 3 downTo 1) {
        for (k in 1..3) Minimax.Storage.seedByMovesPlayed<Move>(2500, i)
    }
}

fun playGame() {
    var game = ConnectFour(difficulty = 4)

    while (!game.isGameOver()) {
        println(game)
        print("Enter move: ")

        val move = Move(readLine()!!.toInt())
        game = game.move(move)

        if (game.isGameOver()) break
        game = game.bestMove()
    }

    println(game)
}