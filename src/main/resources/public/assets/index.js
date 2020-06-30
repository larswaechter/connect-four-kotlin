window.onload = () => {
    const $content = document.querySelector("#board-wrapper")

    const fetchGame = async () => {
        const res = await fetch("/game")
        const content = await res.text()
        $content.innerHTML = content
        setEventListeners()
    }

    const move = async (col) => {
        const res = await fetch("/move/" + col)
        const content = await res.text()
        $content.innerHTML = content
        setEventListeners()
    }

    const setEventListeners = () => {
        Array.from(document.querySelectorAll('.column')).forEach((element) => {
            element.addEventListener('click', (e) => {
                move(element.dataset.column)
            });
        });
    }

    fetchGame()
}

