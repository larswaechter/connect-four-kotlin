package connect.four

class Move(val column: Int) {
    override fun toString(): String {
        return "$column"
    }
}