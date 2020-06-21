package connect.four

import kotlin.math.abs

class Move(val column: Int) {
    companion object {
        /**
         * Instantiate based on move-value in storage entry
         * Required to read move from storage .txt file
         *
         * @param [move]
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