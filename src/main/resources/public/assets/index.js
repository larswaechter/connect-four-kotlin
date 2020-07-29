let game;

class Game {
    constructor(id, players, starter) {
        this.id = id;
        this.players = players;
        this.starter = starter;
        this.isRequestPending = false;
        this.isGameOver = false;
    }

    start = async () => {
        document.querySelector("#game-wrapper").innerHTML = await (await fetch("/start/" + this.id + "?starter=" + this.starter)).text();
        if (this.players === 1 && this.starter === 2) await this.aiMove()
    }

    restart = async () => {
        this.id = Game.createRandomString();
        this.isGameOver = false;
        await this.start();
    }

    move = async (column) => {
        if (!this.isRequestPending && !this.isGameOver) {
            this.isRequestPending = true;
            document.querySelector("#game-wrapper").innerHTML = await (await fetch(this.id + "/move/" + column)).text();
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

            document.querySelector("#game-wrapper").innerHTML = await (await fetch(this.id + "/ai-move")).text();

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
            document.querySelector("#game-wrapper").innerHTML = await (await fetch(this.id + "/undo")).text();
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
    game = new Game(Game.createRandomString(), parseInt(document.querySelector("#players").value), parseInt(document.querySelector("#start-player").value));
    await game.start();
}

const runTests = async () => await fetch("/tests")

document.querySelector("#start-game").addEventListener("click", async () => {
    $("#setup-modal").modal("hide");
    $("#welcome").hide();
    await createLocalGame();
});