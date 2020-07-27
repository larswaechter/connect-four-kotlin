const $content = document.querySelector("#game-wrapper");
const $startBtn = document.querySelector("#start-game");
const $players = document.querySelector("#players");
const $startPlayer = document.querySelector("#start-player")

let game;

/**
 *
 * TODO: Fix undo AI move
 *
 *
 */

class Game {
    constructor(id, players, starter) {
        this.id = id;
        this.players = players;
        this.starter = starter;
        this.isRequestPending = false;
        this.isGameOver = false;
    }

    start = async () => {
        $content.innerHTML = await (await fetch("/start/" + this.id + "?starter=" + this.starter)).text();

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
        if (!this.isRequestPending && !this.isGameOver) {
            this.isRequestPending = true;
            $content.innerHTML = await (await fetch(this.id + "/move/" + column)).text();
            this.isRequestPending = false;

            // Check if game is over
            this.isGameOver = document.querySelector(".metadata").classList.contains("finished");

            // Play AI move
            if (this.players === 1 && !this.isGameOver) await this.aiMove();
        }
    }

    aiMove = async () => {
        if (!this.isRequestPending && !this.isGameOver) {
            const $duration = document.querySelector('.metadata .duration');

            this.isRequestPending = true;
            $duration.classList.add("fetching");

            $content.innerHTML = await (await fetch(this.id + "/ai-move")).text();

            this.isRequestPending = false;
            $duration.classList.remove("fetching");

            // Check if game is over
            this.isGameOver = document.querySelector(".metadata").classList.contains("finished");
        }
    }

    undoMove = async () => {
        if (!this.isRequestPending) {
            this.isGameOver = false;
            this.isRequestPending = true;
            $content.innerHTML = await (await fetch(this.id + "/undo")).text();
            this.isRequestPending = false;
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
    game = new Game(Game.createRandomString(), parseInt($players.value), parseInt($startPlayer.value));
    await game.start();
}

const runTests = async () => await fetch("/tests")

$startBtn.addEventListener("click", async () => {
    $("#setup-modal").modal("hide");
    $("#welcome").hide();
    await createLocalGame();
});