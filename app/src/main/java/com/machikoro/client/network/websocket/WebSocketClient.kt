interface WebSocketClient {
    val connectionStatus: StateFlow<ConnectionStatus>
    val gamePhase: StateFlow<GamePhase>
    val players: StateFlow<List<PlayerCoinState>>
    val lobbyCode: StateFlow<String?>
    val diceResult: StateFlow<List<Int>?>
    val activePlayerId: StateFlow<Int?>

    fun connect()
    fun disconnect()
    fun sendCreateLobby()
    fun clearLobbyCode()
    fun sendGameStart()
    fun rollDice(diceCount: Int = 1)
}