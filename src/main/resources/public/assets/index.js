const $content = document.querySelector("#board-wrapper")

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
    }

    createRandomString = () => {
        let randomNumber = ""

        // Char code [a-z]
        const from = 97
        const to = 122

        do {
            randomNumber += String.fromCharCode(Math.floor(Math.random() * (to - from + 1)) + from);
        } while (randomNumber.length < 16)

        return randomNumber
    }
}

class LocalGame extends Game {
    constructor(players, difficulty) {
        super();
        this.players = players
        this.difficulty = difficulty
    }

    start = async () => {
        const res = await fetch("/start/" + this.id + "?players=" + this.players + "&difficulty=" + this.difficulty)
        const content = await res.text()
        $content.innerHTML = content
        setEventListeners()
    }

    move = async (column) => {
        if (!this.isMovePending) {
            this.isMovePending = true
            const res = await fetch(this.id + "/move/" + column)
            const content = await res.text()
            $content.innerHTML = content
            setEventListeners()
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
            setEventListeners()
        }
    }

    join = () => {
        this.ws = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws/join/" + this.id);
        this.ws.onmessage = msg => {
            $content.innerHTML = msg.data
            setEventListeners()
        }
    }

    move = (column) => {
        if (!this.isMovePending) {
            this.isMovePending = true
            this.ws.send(column)
            this.isMovePending = false
        }
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

const setEventListeners = () => {
    Array.from(document.querySelectorAll('.column')).forEach((element) => {
        element.addEventListener('click', (e) => {
            game.move(element.dataset.column)
        });
    });
}

$startBtn.addEventListener("click", function () {
    $("#setup-modal").modal("hide")
    $("#welcome").hide()
    createGame()
})

$joinBtn.addEventListener("click", function () {
    if ($sessionID.value.length == 16) {
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