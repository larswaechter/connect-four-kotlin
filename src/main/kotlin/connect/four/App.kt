/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package connect.four


fun main() {
    Minimax.Storage.feedByMovesPlayed(5000, 41)

    // Storage.feedByMovesPlayed(10000, 15)
    // println(Storage.doStorageLookup(4).map.size)

    // Minimax.Storage.feedByMovesPlayed(10000, 15)
    // println(Minimax.Storage.doStorageLookup(4).map.size)

    // val game = ConnectFour().playRandomMoves(15)

    /*

    val game = ConnectFour.playRandomMoves(10)
    val invGame = ConnectFour(board = game.board.inverseMatrix())

    println(game)
    game.getStorageRecordKeys().forEach { println(it) }
    println()
    println(invGame)
    invGame.getStorageRecordKeys().forEach { println(it) }

     */
}
