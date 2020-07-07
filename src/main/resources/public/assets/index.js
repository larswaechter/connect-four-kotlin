const $content = document.querySelector("#game-wrapper")

const $startBtn = document.querySelector("#start-game")
const $joinBtn = document.querySelector("#join-game")

const $players = document.querySelector("#players")
const $playersMode = document.querySelector("#players-mode")
const $playersModeForm = document.querySelector("#players-mode-form")

const $sessionID = document.querySelector("#sessionID")

const $difficulty = document.querySelector("#difficulty")
const $difficultyForm = document.querySelector("#difficulty-form")

let game = null;

class Game {
    constructor(id) {
        this.id = id || this.createRandomString()
        this.isMovePending = false
        this.isGameOver = false
    }

    createRandomString = () => {
        let randomNumber = ""
        do randomNumber += String.fromCharCode(Math.floor(Math.random() * (122 - 97 + 1)) + 97);
        while (randomNumber.length < 16)

        return randomNumber
    }
}

class LocalGame extends Game {
    constructor(players, difficulty, multiplayer) {
        super();
        this.players = players
        this.difficulty = difficulty
    }

    start = async () => {
        const res = await fetch("/start/" + this.id + "?players=" + this.players + "&difficulty=" + this.difficulty)
        const content = await res.text()
        $content.innerHTML = content
    }

    move = async (column) => {
        if (!this.isMovePending && !this.isGameOver) {
            this.isMovePending = true
            const res = await fetch(this.id + "/move/" + column)
            $content.innerHTML = await res.text()
            this.isMovePending = false

            if (document.querySelector(".metadata").classList.contains("finished")) this.isGameOver = true
            if (this.players == 1 && !this.isGameOver) await this.aiMove()
        }
    }

    aiMove = async () => {
        if (!this.isMovePending && !this.isGameOver) {
            this.isMovePending = true
            const res = await fetch(this.id + "/ai-move")
            $content.innerHTML = await res.text()
            this.isMovePending = false

            if (document.querySelector(".metadata").classList.contains("finished")) this.isGameOver = true
        }
    }

    undoMove = async (column) => {
        if (!this.isMovePending) {
            this.isMovePending = true
            const res = await fetch(this.id + "/undo")
            const content = await res.text()
            $content.innerHTML = content
            this.isMovePending = false
        }
    }
}

class OnlineGame extends Game {
    constructor(id) {
        super(id);
    }

    create = () => {
        this.ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws/create/" + this.id);
        this.ws.onopen = () => {
            alert("Your Session-ID is: " + this.id)
        }

        this.ws.onmessage = msg => {
            $content.innerHTML = msg.data
        }
    }

    join = () => {
        this.ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws/join/" + this.id);
        this.ws.onmessage = msg => {
            $content.innerHTML = msg.data
        }
    }

    move = (column) => {
        if (!this.isMovePending) {
            this.isMovePending = true
            this.ws.send(column)
            this.isMovePending = false
        }
    }

    aiMove = () => {
    }
}

const createGame = async () => {
    const players = $players.value
    const playersMode = $playersMode.value

    if (players === "1" || (players === "2" && playersMode === "1")) createLocalGame()
    else createOnlineGame()
}

const createLocalGame = async () => {
    const players = $players.value
    const difficulty = $difficulty.value

    game = new LocalGame(players, difficulty)
    await game.start()
}

const createOnlineGame = () => {
    game = new OnlineGame()
    game.create()
}

const joinOnlineGame = () => {
    const sessionID = $sessionID.value
    game = new OnlineGame(sessionID)
    game.join()
}

$startBtn.addEventListener("click", function () {
    $("#setup-modal").modal("hide")
    $("#welcome").hide()
    createGame()
})

$joinBtn.addEventListener("click", function () {
    if ($sessionID.value.length === 16) {
        $sessionID.classList.remove("is-invalid")
        $("#join-modal").modal("hide")
        $("#welcome").hide()
        joinOnlineGame()
    } else $sessionID.classList.add("is-invalid")
})

$players.addEventListener("change", function () {
    if (this.value == 2) {
        $difficultyForm.classList.add("d-none")
        $playersModeForm.classList.remove("d-none")
    } else {
        $playersModeForm.classList.add("d-none")
        $difficultyForm.classList.remove("d-none")
    }
})