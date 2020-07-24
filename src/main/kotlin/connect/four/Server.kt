package connect.four

import io.javalin.Javalin

class Server {
    private val localGames: HashMap<String, ConnectFour> = hashMapOf()

    /**
     * Bootstrap alert classes
     */
    enum class Alert {
        warning, danger
    }

    companion object {
        fun createHtmlAlert(type: Alert, text: String): String = "<div class='alert alert-$type'>$text</div>"
    }

    init {
        val app = Javalin.create { config ->
            config.addStaticFiles("/public")
        }.start(7070)

        Minimax.Storage.registerStorages<Move>()

        /**
         * Run tests
         */
        app.get("/tests") { ctx -> Tests() }

        /**
         * Start new game
         */
        app.get("/start/:id") { ctx ->
            val id = ctx.pathParam("id")
            val paramDifficulty = ctx.queryParam("difficulty", "0")!!
            val paramStarter = ctx.queryParam("starter", "0")!!

            // Validate parameters
            if (this.isValidSessionID(id) && !this.localGames.containsKey(id) && paramDifficulty.matches(Regex("^[0-5]$")) && paramStarter.matches(Regex("^[1-2]$"))) {
                // Create new game and store in HashMap
                val newGame = ConnectFour(difficulty = paramDifficulty.toInt(), currentPlayer = if (paramStarter.toInt() == 1) 1 else -1)
                this.localGames[id] = newGame
                ctx.html(newGame.toHtml())
            } else ctx.html(createHtmlAlert(Alert.danger, "Invalid request!"))
        }

        /**
         * Play move
         */
        app.get("/:id/move/:column") { ctx ->
            val paramID = ctx.pathParam("id")
            val paramColumn = ctx.pathParam("column")

            // Validate parameters
            if (this.localGames.containsKey(paramID) && paramColumn.matches(Regex("^[0-6]\$"))) {
                var game = this.localGames[paramID]!!
                val move = Move(paramColumn.toInt())

                if (game.isValidMove(move)) {
                    game = game.move(move)
                    this.localGames[paramID] = game
                }

                ctx.html(game.toHtml())

            } else ctx.html(createHtmlAlert(Alert.danger, "Invalid request!"))
        }

        /**
         * Let AI play move
         */
        app.get("/:id/ai-move") { ctx ->
            val paramID = ctx.pathParam("id")

            // Validate parameters
            if (this.localGames.containsKey(paramID)) {
                var game = this.localGames[paramID]!!
                game = game.bestMove()

                this.localGames[paramID] = game

                ctx.html(game.toHtml())

            } else ctx.html(createHtmlAlert(Alert.danger, "Invalid request!"))
        }

        /**
         * Undo move
         */
        app.get("/:id/undo") { ctx ->
            val paramID = ctx.pathParam("id")

            // Validate parameters
            if (this.localGames.containsKey(paramID)) {
                var game = this.localGames[paramID]!!

                // Check if there was at least one move played
                if (game.numberOfPlayedMoves > 0) {
                    game = game.undoMove(1)
                    this.localGames[paramID] = game
                }

                ctx.html(game.toHtml())

            } else ctx.html(createHtmlAlert(Alert.danger, "Invalid request!"))
        }
    }

    /**
     * Validate session id
     *
     * @param [id] session id to validate
     * @return is session id valid
     */
    private fun isValidSessionID(id: String): Boolean = id.matches(Regex("[a-z]{16}"))
}