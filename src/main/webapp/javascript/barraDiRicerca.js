// I risultati si costruiscono con il DOM: il nome del prodotto va in textContent, quindi un nome che
// contiene caratteri HTML viene mostrato com'è e non interpretato come codice.
function riempiRisultati(prodotti) {
    let risultati = document.getElementById("risultatiRicerca");
    risultati.replaceChildren();

    for (let i = 0; i < prodotti.length; i++) {
        let id = encodeURIComponent(prodotti[i].Id);

        let link = document.createElement("a");
        link.id = "singolo-risultato";
        link.href = "/?action=read&ProductID=" + id;

        let immagine = document.createElement("img");
        immagine.src = "/getPicture?IDpic=" + id + "&ruolo=principale";
        immagine.onerror = function () { this.onerror = null; this.src = "/img/beauty.png"; };

        let nome = document.createElement("span");
        nome.textContent = prodotti[i].nome;

        link.append(immagine, nome);
        risultati.appendChild(link);
    }

    risultati.style.display = "block";
}

function attendiECerca() {
    setTimeout(getSpecificheProdotti, 200);
}

function getSpecificheProdotti() {
    let request = new XMLHttpRequest();
    let input = document.getElementById("barraRicerca").value;

    if (input === "") {
        nascondiRisultatiRicerca();
        return;
    }

    request.onreadystatechange = function() {
        console.log("READYSTATE: " + request.readyState);

        if (request.readyState === XMLHttpRequest.DONE) {
            if (request.status !== 200) {
                console.error("Errore nella richiesta: " + request.status);
                return;
            }

            try {
                let prodotti = JSON.parse(request.responseText);
                riempiRisultati(prodotti);
            } catch (e) {
                console.error("Errore nel parsing della risposta JSON: " + e);
            }
        }
    };

    request.open("GET", "../?action=dettagliBarraRicercaJSON&input=" + encodeURIComponent(input), true);
    request.send();
}

function nascondiRisultatiRicerca() {
    setTimeout(() => {
        document.getElementById("risultatiRicerca").innerHTML = "";
        document.getElementById("risultatiRicerca").style.display = "none";
    }, 200);
}
