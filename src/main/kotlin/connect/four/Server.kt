package connect.four

import io.javalin.Javalin
import io.javalin.websocket.WsCloseContext
import io.javalin.websocket.WsMessageContext

class Server {
    private val localGames: HashMap<String, ConnectFour> = hashMapOf()
    private val onlineGames: HashMap<String, Lobby> = hashMapOf()

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

        app.get("/tests") { ctx -> Tests() }

        app.get("/start/:id") { ctx ->
            val id = ctx.pathParam("id")
            val paramDifficulty = ctx.queryParam("difficulty", "0")!!

            // Validate parameters
            if (this.isValidSessionID(id) && paramDifficulty.matches(Regex("^[0-5]$"))) {
                // Create new game and store in HashMap
                val newGame = ConnectFour(difficulty = 5)
                this.localGames[id] = newGame
                ctx.html(newGame.toHtml())
            } else ctx.html(createHtmlAlert(Alert.danger, "Invalid request!"))
        }

        app.get("/:id/move/:column") { ctx ->
            val paramID = ctx.pathParam("id")
            val paramColumn = ctx.pathParam("column")

            // Validate parameters
            if (this.localGames.containsKey(paramID) && paramColumn.matches(Regex("^[0-6]\$"))) {
                var game = this.localGames[paramID]!!
                val column = paramColumn.toInt()
                game = game.move(Move(column))

                // Update in HashMap
                this.localGames[paramID] = game

                if (game.isGameOver()) this.localGames.remove(paramID)
                ctx.html(game.toHtml())

            } else ctx.html(createHtmlAlert(Alert.danger, "Invalid request!"))
        }

        app.get("/:id/ai-move") { ctx ->
            val paramID = ctx.pathParam("id")

            // Validate parameters
            if (this.localGames.containsKey(paramID)) {
                var game = this.localGames[paramID]!!
                game = game.bestMove()

                // Update in HashMap
                this.localGames[paramID] = game

                if (game.isGameOver()) this.localGames.remove(paramID)
                ctx.html(game.toHtml())

            } else ctx.html(createHtmlAlert(Alert.danger, "Invalid request!"))
        }

        app.get("/:id/undo") { ctx ->
            val paramID = ctx.pathParam("id")

            // Validate parameters
            if (this.localGames.containsKey(paramID)) {
                var game = this.localGames[paramID]!!

                // Check if there was at least one move played
                if (game.getNumberOfPlayedMoves() > 0) {
                    game = game.undoMove(1)
                    this.localGames[paramID] = game
                }

                ctx.html(game.toHtml())

            } else ctx.html(createHtmlAlert(Alert.danger, "Invalid request!"))
        }

        app.ws("/ws/create/:id") { ws ->
            ws.onConnect { ctx ->
                val id = ctx.pathParam("id")

                // Validate session-id
                if (this.isValidSessionID(id)) {
                    // Create new game lobby
                    val newGame = Lobby(ctx, null, ctx.sessionId)

                    // Update in HashMap
                    this.onlineGames[id] = newGame

                    // The lobby create has to wait until another player joins
                    ctx.send(createHtmlAlert(Alert.warning, "Waiting for another player..."))
                } else {
                    ctx.send(createHtmlAlert(Alert.danger, "Invalid session-id!"))
                }
            }
            ws.onMessage() { ctx -> this.handleWebsocketMessage(ctx) }
            ws.onClose() { ctx -> this.handleWebsocketDisconnect(ctx) }
        }

        app.ws("/ws/join/:id") { ws ->
            ws.onConnect { ctx ->
                val id = ctx.pathParam("id")

                // Check if lobby exists
                if (this.doesLobbyExists(id)) {

                    // Store ws connection in game instance
                    val onlineGame = this.onlineGames[id]!!
                    onlineGame.playerTwoSocket = ctx

                    // Update in HashMap
                    this.onlineGames[id] = onlineGame

                    // Game starts
                    onlineGame.playerOneSocket!!.send(onlineGame.game.toHtml())
                    onlineGame.playerTwoSocket!!.send(onlineGame.game.toHtml())
                } else {
                    ctx.send(createHtmlAlert(Alert.danger, "Lobby not found!"))
                }
            }

            ws.onMessage() { ctx -> this.handleWebsocketMessage(ctx) }
            ws.onClose() { ctx -> this.handleWebsocketDisconnect(ctx) }
        }
    }

    /**
     * Handle websocket disconnect.
     * Notify player that his opponent has left.
     *
     * @param [ctx] ws context of player who left the game
     */
    private fun handleWebsocketDisconnect(ctx: WsCloseContext) {
        val id = ctx.pathParam("id")

        // Check if lobby exists
        if (this.doesLobbyExists(id)) {
            val onlineGame = this.onlineGames[id]!!

            // Determine the player we have to notify that his opponent left
            val playerToNotify = when (ctx.sessionId) {
                onlineGame.playerOneSocket?.sessionId -> onlineGame.playerTwoSocket
                onlineGame.playerTwoSocket?.sessionId -> onlineGame.playerOneSocket
                else -> null
            }

            // Send notification if player exists
            playerToNotify?.send(createHtmlAlert(Alert.warning, "Your opponent has left the game. You won!"))

            // Delete lobby
            this.onlineGames.remove(id)
        }
    }

    /**
     * Handle websocket message (player move).
     *
     * @param [ctx] ws context of player making the request
     */
    private fun handleWebsocketMessage(ctx: WsMessageContext) {
        val id = ctx.pathParam("id")!!
        val column = ctx.message()

        // Check if lobby exists and validate move
        if (this.doesLobbyExists(id) && column.matches(Regex("^[0-6]\$"))) {
            val lobby = this.onlineGames[id]!!

            // Check if player is allowed to make move (his turn)
            if (ctx.sessionId == lobby.currentPlayerSessionID) {
                lobby.game = lobby.game.move(Move(column.toInt()))

                // Determine next player
                lobby.currentPlayerSessionID = when (lobby.currentPlayerSessionID) {
                    lobby.playerOneSocket!!.sessionId -> lobby.playerTwoSocket!!.sessionId
                    else -> lobby.playerOneSocket!!.sessionId
                }

                // Update in HashMap
                this.onlineGames[id] = lobby
            }

            // Send updated board to players
            lobby.playerOneSocket!!.send(lobby.game.toHtml())
            lobby.playerTwoSocket!!.send(lobby.game.toHtml())
        } else {
            ctx.send(createHtmlAlert(Alert.danger, "Invalid request!"))
        }
    }

    /**
     * Validate session id
     *
     * @param [id] session id to validate
     * @return is session id valid
     */
    private fun isValidSessionID(id: String): Boolean = id.matches(Regex("[a-z]{16}"))

    /**
     * Check if a lobby exist for the given session id
     *
     * @param [id] session id of lobby
     * @return does lobby exist
     */
    private fun doesLobbyExists(id: String): Boolean = this.isValidSessionID(id) && this.onlineGames.containsKey(id)
}