package connect.four

import io.javalin.websocket.WsConnectContext

class GameLobby(
        var playerOneSocket: WsConnectContext?,
        var playerTwoSocket: WsConnectContext?,
        var currentPlayerSessionID: String,
        var game: ConnectFour = ConnectFour(multiplayer = true)
) {
}