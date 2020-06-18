package connect.four

import java.io.File
import kotlin.math.ceil

typealias StorageEntry = Triple<Move, Float, Boolean>

class Storage(private val movesPlayed: Int) {
    private var filename: String
    var hashMap: HashMap<Int, StorageEntry>

    init {
        this.filename = getTableFilename(movesPlayed)
        this.hashMap = getHashMapFromStorage(movesPlayed)
    }

    fun overwriteFile() {
        val file = this.getFile()
        var res = ""

        this.hashMap.forEach { (k, v) ->
            this.hashMap[k] = v
            res += storageEntryToString(k, v) + "\n"
        }

        file.writeText(res)
    }

    fun appendFile(map: HashMap<Int, StorageEntry>, updateHashMap: Boolean = true) {
        val file = this.getFile()
        var res = ""

        map.forEach { (k, v) ->
            if (updateHashMap) this.hashMap[k] = v
            res += storageEntryToString(k, v) + "\n"
        }

        file.appendText(res)
    }

    private fun getFile(): File = getFile(this.filename)

    companion object {
        private var tablesRegistered: Boolean = false
        private const val numberOfTranspositionTables = 14
        private const val transpositionTablesPath = "src/main/resources/transposition_tables"
        private val storages: Array<Storage?> = Array(numberOfTranspositionTables) { null }

        fun storageEntryToString(key: Int, entry: StorageEntry): String =
                "$key ${entry.first.column} ${entry.second} ${entry.third}"

        /**
         * Register all storages
         */
        fun registerStorages() {
            for (i in storages.indices) {
                println("Register storage #$i...")
                storages[i] = Storage((i + 1) * 3)
            }
            tablesRegistered = true
        }

        /**
         * Get storage based on moves played
         *
         * @param [movesPlayed] number of played moves
         * @return Storage
         */
        fun doStorageLookup(movesPlayed: Int): Storage {
            if (!tablesRegistered) registerStorages()
            return this.storages[ceil((movesPlayed.toDouble() / 3)).toInt() - 1]!!
        }

        /**
         * Feed transposition tables amount-times with boards of movesPlayed-moves
         *
         * @param [amount] number of data records
         * @param [movesPlayed] number of played moves
         * */
        fun feedByMovesPlayed(amount: Int, movesPlayed: Int) {
            val storage = Storage(movesPlayed)
            val newHashMap: HashMap<Int, StorageEntry> = HashMap()
            var count = 0

            for (i in 1..amount) {
                val game = ConnectFour().playRandomMoves(movesPlayed)
                val hashCode = game.board.contentHashCode()

                // TODO: Call minimax

                // Board is not stored yet
                if (!storage.hashMap.containsKey(hashCode) && !newHashMap.containsKey(hashCode)) {
                    count++
                    newHashMap[hashCode] = Triple(Move(2), 10F, true)
                }
            }

            storage.appendFile(newHashMap, false)
            println("Added $count / $amount new data records.")
        }

        /**
         * Get storage file based on moves played
         *
         * @param [movesPlayed] number of played moves
         * @return file
         */
        fun getFile(movesPlayed: Int): File =
                File(transpositionTablesPath + this.getTableFilename(movesPlayed))

        /**
         * Get storage file based on filename
         *
         * @param [filename] transposition table file name
         * @return file
         */
        fun getFile(filename: String): File = File("$transpositionTablesPath/$filename")

        /**
         * Get table file name based on moves played
         *
         * @param [movesPlayed] number of played moves
         * @return file name
         */
        fun getTableFilename(movesPlayed: Int): String =
                when (movesPlayed) {
                    in 1..3 -> "00_table_1_3.txt"
                    in 4..6 -> "01_table_4_6.txt"
                    in 7..9 -> "02_table_7_9.txt"
                    in 10..12 -> "03_table_10_12.txt"
                    in 13..15 -> "04_table_13_15.txt"
                    in 16..18 -> "05_table_16_18.txt"
                    in 19..21 -> "06_table_19_21.txt"
                    in 22..24 -> "07_table_22_24.txt"
                    in 25..27 -> "08_table_25_27.txt"
                    in 28..30 -> "09_table_28_30.txt"
                    in 31..33 -> "10_table_31_33.txt"
                    in 34..36 -> "11_table_34_36.txt"
                    in 37..39 -> "12_table_37_39.txt"
                    in 40..42 -> "13_table_40_42.txt"
                    else -> ""
                }

        /**
         * Create HashMap from storage .txt file
         *
         * @param [movesPlayed] number of played moves
         * @return HashMap for played moves
         */
        fun getHashMapFromStorage(movesPlayed: Int): HashMap<Int, StorageEntry> {
            val file = this.getFile(movesPlayed)
            val map: HashMap<Int, StorageEntry> = HashMap()

            file.forEachLine {
                val elements = it.split(" ")
                map[elements[0].toInt()] = Triple(Move(elements[1].toInt()), elements[2].toFloat(), elements[3].toBoolean())
            }

            return map
        }
    }
}