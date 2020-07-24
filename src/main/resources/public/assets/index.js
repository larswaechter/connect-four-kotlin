const $content = document.querySelector("#game-wrapper");
const $startBtn = document.querySelector("#start-game");
const $players = document.querySelector("#players");
const $startPlayer = document.querySelector("#start-player")
const $difficulty = document.querySelector("#difficulty");
const $difficultyForm = document.querySelector("#difficulty-form");

let game = null;

/**
 *
 * TODO: Fix undo AI move
 *
 *
 */

class Game {
    constructor(id, players, difficulty, starter) {
        this.id = id;
        this.players = players;
        this.difficulty = difficulty;
        this.starter = starter;
        this.isMovePending = false;
        this.isGameOver = false;
    }

    start = async () => {
        const res = await fetch("/start/" + this.id + "?difficulty=" + this.difficulty + "&starter=" + this.starter);
        $content.innerHTML = await res.text();

        // AI starts after 1s
        if (this.players === 1 && this.starter === 2)
            setTimeout(async () => {
                await this.aiMove()
            }, 1000)
    }

    restart = async () => {
        this.id = Game.createRandomString();
        this.isGameOver = false;
        await this.start();
    }

    move = async (column) => {
        if (!this.isMovePending && !this.isGameOver) {
            this.isMovePending = true;
            const res = await fetch(this.id + "/move/" + column);
            $content.innerHTML = await res.text();
            this.isMovePending = false;

            // Check if game is over
            this.isGameOver = document.querySelector(".metadata").classList.contains("finished");

            // Play AI move
            if (this.players === 1 && !this.isGameOver) await this.aiMove();
        }
    }

    aiMove = async () => {
        if (!this.isMovePending && !this.isGameOver) {
            const $spinner = document.querySelector('.spinner-border');

            this.isMovePending = true;
            $spinner.classList.remove("d-none");

            const res = await fetch(this.id + "/ai-move");
            $content.innerHTML = await res.text();

            this.isMovePending = false;
            $spinner.classList.add("d-none");

            // Check if game is over
            this.isGameOver = document.querySelector(".metadata").classList.contains("finished");
        }
    }

    undoMove = async () => {
        if (!this.isMovePending) {
            this.isGameOver = false;
            this.isMovePending = true;
            const res = await fetch(this.id + "/undo");
            $content.innerHTML = await res.text();
            this.isMovePending = false;
        }
    }

    static createRandomString = () => {
        let randomNumber = "";
        do randomNumber += String.fromCharCode(Math.floor(Math.random() * (122 - 97 + 1)) + 97);
        while (randomNumber.length < 16);
        return randomNumber;
    }
}

const createLocalGame = async () => {
    game = new Game(Game.createRandomString(), parseInt($players.value), parseInt($difficulty.value), parseInt($startPlayer.value));
    await game.start();
}

const runTests = async () => {
    await fetch("/tests")
}

$startBtn.addEventListener("click", async () => {
    $("#setup-modal").modal("hide");
    $("#welcome").hide();
    await createLocalGame();
});

$players.addEventListener("change", function () {
    if (this.value == 2) $difficultyForm.classList.add("d-none");
    else $difficultyForm.classList.remove("d-none");
});