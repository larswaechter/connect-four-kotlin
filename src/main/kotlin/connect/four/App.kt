/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package connect.four

fun main() {

    Tests()

    return


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


    Tests()

    return

    var game = ConnectFour()
    println(game.storageRecordPrimaryKey)

    game = game.move(Move(0))
    game = game.undoMove(1)

    println(game.storageRecordPrimaryKey)

    return

    Tests()

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