package connect.four

import kotlin.math.abs
import kotlin.math.ceil

/**
 * Interface for implementing Minimax algorithm in two-player zero-sum games
 *
 * @param [Game] the type of a game
 * @param [Move] the type of a move
 */
interface Minimax<Game, Move> {
    /**
     * Game board
     */
    val board: IntArray

    /**
     * Current player
     */
    val currentPlayer: Int

    /**
     * Evaluate game state for current player.
     *  For player +1, a higher value is better. (maximizer)
     *  For player -1, a lower value is better. (minimizer)
     *
     * @return Positive or negative integer
     */
    fun evaluate(depth: Int): Int

    /**
     * Get list of all possible moves
     *
     * @return List of possible moves
     */
    fun getPossibleMoves(): List<Move>

    /**
     * Check if no more moves are possible
     *
     * @return is game over
     */
    fun isGameOver(): Boolean

    /**
     * Do move and return new game
     *
     * @param [move] move to perform
     * @return new game with applied move
     */
    fun move(move: Move): Game

    /**
     * Undo a number of moves
     *
     * @param [number] number of moves to undo
     * @return new game with undone moves
     */
    fun undoMove(number: Int): Game

    /**
     * Pick random move from possible moves list
     *
     * @return a random move
     */
    fun getRandomMove(): Move = this.getPossibleMoves().random()


    /**
     * Minimax algorithm that finds best move
     *
     * @param [game]
     * @param [depth] maximal tree depth
     * @param [maximize] maximize or minimize
     * @param [storedBoards] boards with evaluated best move & score
     * @return triple of (Move, Score, WasGoodMove)
     */
    fun minimax(
            game: Minimax<Game, Move>,
            depth: Int = this.getPossibleMoves().size,
            maximize: Boolean = game.currentPlayer == 1,
            storedBoards: HashMap<Int, Triple<Move?, Float, Boolean>> = HashMap()
    ): Triple<Move?, Float, Boolean> {

        // Recursion anchor -> Evaluate board
        if (depth == 0 || game.isGameOver()) return Triple(null, game.evaluate(depth).toFloat(), false)

       // val storage = Storage.doStorageLookup((game as ConnectFour).getNumberOfPlayedMoves())

        val boardSortedHash = game.board.sortedArray().contentHashCode()

        // Check if board exists in storage
        if (storedBoards.containsKey(boardSortedHash)) {
            val storedBoard = storedBoards[boardSortedHash]!!
            val scoreAbs = abs(storedBoard.second)
            val newScore: Float

            // Was move for storedBoard a good one or not? -> Transform score for current player
            if (storedBoard.third) newScore = if (maximize) scoreAbs else -scoreAbs
            else newScore = if (maximize) -scoreAbs else scoreAbs

            return Triple(storedBoard.first, newScore, storedBoard.third)
        }

        // Call recursively from here on for each move to find best one
        var minOrMax: Pair<Move?, Float> = Pair(null, if (maximize) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY)

        for (move in game.getPossibleMoves()) {
            // Apply move - We have to cast here since NimGame prescribes the return type NimGame
            val newGame = game.move(move) as Minimax<Game, Move>

            val moveScore = this.minimax(newGame, depth - 1, !maximize, storedBoards).second
            val score: Pair<Move?, Float> = Pair(move, moveScore)

            // Check for maximum or minimum
            if ((maximize && score.second > minOrMax.second) || (!maximize && score.second < minOrMax.second))
                minOrMax = score
        }

        // Add best move for current position to storage
        val wasGoodMove = (maximize && minOrMax.second > 0) || (!maximize && minOrMax.second < 0)
        val finalMove = Triple(minOrMax.first, minOrMax.second, wasGoodMove)

        // Store move
        storedBoards[boardSortedHash] = finalMove

        return finalMove
    }
}