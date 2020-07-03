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

        app.get("/start/:id") { ctx ->
            val id = ctx.pathParam("id")
            val paramPlayers = ctx.queryParam("players", "0")!!
            val paramDifficulty = ctx.queryParam("difficulty", "0")!!

            // Validate parameters
            if(this.isValidSessionID(id) && paramPlayers.matches(Regex("^[1-2]$")) && paramDifficulty.matches(Regex("^[1-2]$"))) {
                // Create new game and store in HashMap
                val newGame = ConnectFour(difficulty = paramDifficulty.toInt(), multiplayer = paramPlayers.toInt() == 2)
                this.localGames[id] = newGame
                ctx.html(newGame.toHTML())
            } else ctx.html(createHtmlAlert(Alert.danger, "Invalid request!"))
        }

        app.get("/:id/move/:column") { ctx ->
            val paramID = ctx.pathParam("id")
            val paramColumn = ctx.pathParam("column")

            // Validate parameters
            if (this.localGames.containsKey(paramID) && paramColumn.matches(Regex("^[0-6]\$"))) {
                // Play move
                var game = this.localGames[paramID]!!
                val column = paramColumn.toInt()
                game = game.move(Move(column))

                // AI plays the move if there's no multiplayer
                if (!game.multiplayer) game = game.move(game.getRandomMove())

                // Update in HashMap
                this.localGames[paramID] = game

                // Check game status
                if (game.hasWinner()) {
                    this.localGames.remove(paramID)
                    ctx.html(game.toHTML())
                } else if (game.getNumberOfRemainingMoves() == 0) {
                    this.localGames.remove(paramID)
                    ctx.html(game.toHTML())
                } else {
                    ctx.html(game.toHTML())
                }
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
                    onlineGame.playerOneSocket!!.send(onlineGame.game.toHTML())
                    onlineGame.playerTwoSocket!!.send(onlineGame.game.toHTML())
                } else {
                    ctx.send(createHtmlAlert(Alert.danger, "Lobby not found!"))
                }
            }

            ws.onMessage() { ctx -> this.handleWebsocketMessage(ctx) }
            ws.onClose() { ctx -> this.handleWebsocketDisconnect(ctx) }
        }
    }

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
            lobby.playerOneSocket!!.send(lobby.game.toHTML())
            lobby.playerTwoSocket!!.send(lobby.game.toHTML())
        } else {
            ctx.send(createHtmlAlert(Alert.danger, "Invalid request!"))
        }
    }

    private fun isValidSessionID(id: String): Boolean = id.matches(Regex("[a-z]{16}"))

    private fun doesLobbyExists(id: String): Boolean = this.isValidSessionID(id) && this.onlineGames.containsKey(id)
}