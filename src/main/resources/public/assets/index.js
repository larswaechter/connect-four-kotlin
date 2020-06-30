window.onload = () => {
    let id = "";
    const $content = document.querySelector("#board-wrapper")
    const $startBtn = document.querySelector("#start-game")

    const startGame = async () => {
        id = createRandomString()
        const res = await fetch("/start/" + id)
        const content = await res.text()
        $content.innerHTML = content
        setEventListeners()
    }

    const move = async (col) => {
        const res = await fetch(id + "/move/" + col)
        const content = await res.text()
        $content.innerHTML = content
        setEventListeners()
    }

    const createRandomString = () => Math.random().toString(36).substring(7);

    const setEventListeners = () => {
        Array.from(document.querySelectorAll('.column')).forEach((element) => {
            element.addEventListener('click', (e) => {
                move(element.dataset.column)
            });
        });
    }

    $startBtn.addEventListener("click", function () {
        $("#setup-modal").modal("hide")
        $("#welcome").hide()
        startGame()
    })
}

