package connect.four

import io.javalin.websocket.WsConnectContext

/**
 * Lobby class used to wrap websocket connections and a game instance.
 * A lobby consists of two players and one game instance.
 *
 * @param [playerOneSocket] ws connection of the first player
 * @param [playerTwoSocket] ws connection of the second player
 * @param [currentPlayerSessionID] ws session id of the current player
 * @param [game] game instance
 */
class Lobby(
        var playerOneSocket: WsConnectContext?,
        var playerTwoSocket: WsConnectContext?,
        var currentPlayerSessionID: String,
        var game: ConnectFour = ConnectFour()
) {
}