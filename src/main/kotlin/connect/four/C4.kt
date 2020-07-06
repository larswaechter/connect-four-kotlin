package connect.four

import java.io.File
import kotlin.math.pow
import kotlin.random.Random

class C4(
        // LSB is column 0
        // MSB is column 6
        // left 15 bits are unused -> only used to check fourInARow
        val board: LongArray = longArrayOf(
                0b0_0000000_0000000___0000000_0000000_0000000_0000000_0000000_0000000_0000000, // X
                0b0_0000000_0000000___0000000_0000000_0000000_0000000_0000000_0000000_0000000 // O
        ),

        val heights: IntArray = intArrayOf(
                0, 7, 14, 21, 28, 35, 42
        ),

        val currentPlayer: Int = 1,

        val storageRecordPrimaryKey: Long = calcZobristHash(board)
) {

    companion object {
        private val zobristTable: Array<Array<Long>> = buildZobristTable()

        fun generateZobristHashes() {
            val file = File("src/main/resources/transposition_tables/zobrist_hashes.txt")
            var res = ""

            for (i in 0..47)
                for (k in 0..1)
                    res += "${Random.nextLong(2F.pow(64).toLong())}\n"

            file.writeText(res)
        }

        fun getZobristHash(field: Int, player: Int): Long = zobristTable[field][if (player == 1) 0 else 1]

        private fun buildZobristTable(): Array<Array<Long>> {
            val keys = readZobristHashes()
            val table = Array(48) { Array(2) { 0L } }

            var count = 0
            for (i in 0..47)
                for (k in 0..1)
                    table[i][k] = keys[count++]

            return table
        }

        private fun readZobristHashes(): Array<Long> {
            val file = File("src/main/resources/transposition_tables/zobrist_hashes.txt")
            val keys = Array<Long>(96) { 0 }

            if (file.readLines().isEmpty()) {
                println("No Zobrist hashes found. Creating...")
                generateZobristHashes()
                return readZobristHashes()
            }

            var count = 0
            file.forEachLine {
                keys[count++] = it.toLong()
            }

            return keys
        }

        fun calcZobristHash(board: LongArray): Long {
            var hash = 0L

            for (i in intArrayOf(1, -1)) {
                val tmpBoard: Long = if (i == 1) board[0] else board[1]
                for (k in 0..42) {
                    if (1L shl k and tmpBoard != 0L) {
                        hash = hash xor getZobristHash(k, i)
                    }
                }
            }

            return hash
        }
    }

    private fun getPlayerBoard(player: Int = this.currentPlayer) = when (player) {
        1 -> this.board[0]
        else -> this.board[1]
    }


    fun move(col: Int): C4 {
        val top = 0b1000000_1000000_1000000_1000000_1000000_1000000_1000000L
        assert(top and (1L shl this.heights[col]) == 0L)

        val move: Long = 1L shl heights[col]
        val insertedAt = heights[col]

        val newHeights = heights.clone()
        newHeights[col] += 1

        val newBoard = this.getPlayerBoard() xor move
        val newBoards = when (this.currentPlayer) {
            1 -> longArrayOf(newBoard, this.board[1])
            else -> longArrayOf(this.board[0], newBoard)
        }

        val newZobristHash = this.storageRecordPrimaryKey.xor(getZobristHash(insertedAt, this.currentPlayer))

        return C4(newBoards, newHeights, -this.currentPlayer, newZobristHash)
    }

    fun getPossibleMoves(): MutableList<Int> {
        val moves = mutableListOf<Int>()
        val top = 0b1000000_1000000_1000000_1000000_1000000_1000000_1000000L

        for (col in 0..6)
            if (top and (1L shl this.heights[col]) == 0L)
                moves.add(col)

        return moves
    }

    fun hasWinner(): Boolean = this.fourInARow(this.board[0]) || this.fourInARow(this.board[1])

    fun fourInARow(bitboard: Long): Boolean {
        val directions = intArrayOf(1, 7, 6, 8)
        var bb: Long

        for (direction in directions) {
            bb = bitboard and (bitboard shr direction)
            if (bb and (bb shr 2 * direction) != 0L) return true
        }

        return false
    }

    fun mirror(board: Long): Long {
        var mirror = board and 0b111111 shl 42
        mirror = mirror or (board and 0b111111_0000000 shl 28)
        mirror = mirror or (board and 0b111111_0000000_0000000 shl 14)
        mirror = mirror or (board and 0b1111111_0000000_0000000_0000000)
        mirror = mirror or (board and 0b1111111_0000000_0000000_0000000_0000000 shr 14)
        mirror = mirror or (board and 0b1111111_0000000_0000000_0000000_0000000_0000000 shr 28)
        mirror = mirror or (board and 0b1111111_0000000_0000000_0000000_0000000_0000000_0000000 shr 42)

        return mirror
    }

    fun getNumberOfRemainingMoves(): Int = 42 - this.getNumberOfPlayedMoves()

    fun getNumberOfPlayedMoves(): Int {
        var count = 0
        for (i in 0..47)
            if ((1L shl i and this.board[0] != 0L) || (1L shl i and this.board[1] != 0L))
                count++
        return count
    }


    override fun toString(): String {
        var res = ""

        for (i in 5 downTo 0) {
            for (k in i..(42 + i) step 7) {
                val player = if (1L shl k and board[0] != 0L) "X" else if (1L shl k and board[1] != 0L) "O" else "."
                res += "$player "
            }
            res += "\n"
        }

        return res
    }

}