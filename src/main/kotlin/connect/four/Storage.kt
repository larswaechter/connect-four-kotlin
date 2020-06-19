package connect.four

import java.io.File
import kotlin.math.ceil

typealias StorageRecord = Triple<Move, Float, Boolean>

class Storage(private val index: Int) {
    private val filename: String
    val map: HashMap<Int, StorageRecord>

    init {
        this.filename = getTableFilename(this.index)
        this.map = this.loadHashMap()
    }

    /**
     * Write current map instance to storage .txt file
     *
     * @param [updateMap] update current map instance
     */
    fun syncFile(updateMap: Boolean = true) {
        val file = this.getFile()
        var res = ""

        this.map.clear()

        this.map.forEach { (k, v) ->
            if (updateMap) this.map[k] = v
            res += hashMapEntryToString(k, v) + "\n"
        }

        file.writeText(res)
    }

    /**
     * Append new HashMap to storage .txt file
     *
     * @param [map] new HashMap to append
     * @param [updateMap] update current map instance
     */
    fun appendFile(map: HashMap<Int, StorageRecord>, updateMap: Boolean = true) {
        val file = this.getFile()
        var res = ""

        map.forEach { (k, v) ->
            if (!this.map.containsKey(k)) {
                if (updateMap) this.map[k] = v
                res += hashMapEntryToString(k, v) + "\n"
            }
        }

        file.appendText(res)
    }

    /**
     * Get storage .txt file
     *
     * @return file
     */
    private fun getFile(): File = File("$transpositionTablesPath/${this.filename}")

    /**
     * Load HashMap from storage .txt file
     *
     * @return HashMap for played moves
     */
    private fun loadHashMap(): HashMap<Int, StorageRecord> {
        val file = this.getFile()
        val map: HashMap<Int, StorageRecord> = HashMap()

        file.forEachLine {
            val elements = it.split(" ")
            map[elements[0].toInt()] = Triple(Move(elements[1].toInt()), elements[2].toFloat(), elements[3].toBoolean())
        }

        return map
    }

    companion object {
        private const val numberOfTranspositionTables = 14
        private const val transpositionTablesPath = "src/main/resources/transposition_tables"
        private var storagesRegistered: Boolean = false
        private val storages: Array<Storage?> = Array(numberOfTranspositionTables) { null }

        /**
         * Convert HashMap value to string
         * This method is mainly used to format the HashMap values for the storage .txt file
         *
         * @param [key] HashMap key
         * @param [entry] HashMap entry
         * @return String to write
         */
        fun hashMapEntryToString(key: Int, entry: StorageRecord): String =
                "$key ${entry.first.column} ${entry.second} ${entry.third}"

        /**
         * Get storage index based on played moves
         *
         * @param [playedMoves] played moves
         * @return storage index
         */
        fun getStorageIndexFromPlayedMoves(playedMoves: Int): Int = ceil((playedMoves.toDouble() / 3)).toInt() - 1

        /**
         * Register all storages
         */
        fun registerStorages() {
            for (i in storages.indices) {
                println("Register storage #$i...")
                storages[i] = Storage(i)
            }
            storagesRegistered = true
        }

        /**
         * Get storage based on storage index
         *
         * @param [index] storage index
         * @return Storage
         */
        fun doStorageLookup(index: Int): Storage {
            if (!storagesRegistered) registerStorages()
            return storages[index]!!
        }

        /**
         * Feed transposition tables amount-times with boards of movesPlayed-moves
         *
         * @param [amount] number of data records
         * @param [movesPlayed] number of played moves
         * */
        fun feedByMovesPlayed(amount: Int, movesPlayed: Int) {
            val storage = Storage(getStorageIndexFromPlayedMoves(movesPlayed))
            val newHashMap: HashMap<Int, StorageRecord> = HashMap()
            var count = 0

            for (i in 1..amount) {
                val game = ConnectFour().playRandomMoves(movesPlayed)
                val hashCode = game.board.contentHashCode()

                // TODO: Call minimax

                // Board is not stored yet
                if (!storage.map.containsKey(hashCode) && !newHashMap.containsKey(hashCode)) {
                    count++
                    newHashMap[hashCode] = Triple(Move(2), 10F, true)
                }
            }

            storage.appendFile(newHashMap, false)
            println("${storage.filename}: Added $count / $amount new data records.")
        }

        /**
         * Get table file name based on storage index
         *
         * @param [storageIndex] storage index
         * @return file name
         */
        fun getTableFilename(storageIndex: Int): String {
            assert(storageIndex < numberOfTranspositionTables)
            val id = if (storageIndex < 10) "0$storageIndex" else "$storageIndex"
            val from = storageIndex * 3 + 1
            val to = from + 2
            return "${id}_table_${from}_${to}.txt"
        }
    }
}