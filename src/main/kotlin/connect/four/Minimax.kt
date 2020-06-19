package connect.four

/**
 * Interface for implementing Minimax algorithm in two-player zero-sum games
 *
 * @param [Move] the type of a move
 */
interface Minimax<Move> {
    /**
     * Game board
     */
    val board: Array<IntArray>

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
    fun evaluate(depth: Int): Float

    /**
     * Get list of all possible moves
     *
     * @return List of possible moves
     */
    fun getPossibleMoves(): List<Move>

    fun getNumberOfRemainingMoves(): Int

    /**
     * Check if no more moves are possible or a player has one
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
    fun move(move: Move): Minimax<Move>

    /**
     * Pick random move from possible moves list
     *
     * @param [possibleMoves] possible moves to pick random move from
     * @return a random move
     */
    fun getRandomMove(possibleMoves: List<Move> = this.getPossibleMoves()): Move = possibleMoves.random()

    /**
     * Get index in storage based on played moves
     *
     * @return storage index
     */
    fun getStorageIndex(): Int

    /**
     * Minimax algorithm that finds best move
     *
     * @param [game]
     * @param [depth] maximal tree depth
     * @param [maximize] maximize or minimize
     * @return triple of (Move, Score, CurrentPlayer)
     */
    fun minimax(
            game: Minimax<Move> = this,
            depth: Int = this.getNumberOfRemainingMoves(),
            maximize: Boolean = game.currentPlayer == 1
    ): Triple<Move?, Float, Int> {

        // Recursion anchor -> Evaluate board
        if (depth == 0 || game.isGameOver()) return Triple(null, game.evaluate(depth), game.currentPlayer)

        val storageIndex = game.getStorageIndex()
        if (storageIndex >= 0) {
            val storage = Storage.doStorageLookup(storageIndex)
            val storageKey = 123123

            // Check if board exists in storage
            if (storage.map.containsKey(storageKey)) {
                val storedBoard = storage.map[storageKey]!! as Triple<Move?, Float, Int>
                val newScore = if (storedBoard.third == game.currentPlayer) storedBoard.second else storedBoard.second * storedBoard.third * game.currentPlayer
                return Triple(storedBoard.first, newScore, game.currentPlayer)
            }
        }

        // Call recursively from here on for each move to find best one
        var minOrMax: Pair<Move?, Float> = Pair(null, if (maximize) Float.NEGATIVE_INFINITY else Float.POSITIVE_INFINITY)

        for (move in game.getPossibleMoves()) {
            val moveScore = this.minimax(game.move(move), depth - 1, !maximize).second

            // Check for maximum or minimum
            if ((maximize && moveScore > minOrMax.second) || (!maximize && moveScore < minOrMax.second))
                minOrMax = Pair(move, moveScore)
        }

        return Triple(minOrMax.first, minOrMax.second, game.currentPlayer)
    }
}