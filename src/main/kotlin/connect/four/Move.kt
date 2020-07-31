package connect.four

import kotlin.math.abs

/**
 * Class that represents a move
 *
 * @param [column] the column where to place the move at
 */
class Move(val column: Int) {
    companion object {
        /**
         * Instantiate based on move-value in storage entry
         * Required to read move from storage .txt file
         *
         * @param [move] as in string representation
         * @return Move
         */
        fun ofStorageEntry(move: String) = Move(move.toInt())
    }

    /**
     * Mirror move on y-Axis
     *
     * @return mirrored move
     */
    fun mirrorYAxis(): Move {
        return Move(abs(6 - this.column))
    }

    override fun toString(): String {
        return "$column"
    }
}