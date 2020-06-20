package connect.four

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

    override fun toString(): String {
        return "$column"
    }
}