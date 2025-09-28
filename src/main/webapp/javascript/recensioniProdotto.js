document.addEventListener('DOMContentLoaded', () => {
	const thumbnails = document.querySelectorAll('.thumbnail-images img');
	const mainImage = document.getElementById('main-image');
	
	thumbnails.forEach(thumbnail => {
	    thumbnail.addEventListener('click', () => {
	        mainImage.src = thumbnail.src;
	    });
	});
});

// Ottieni tutte le stelle
let stars = document.querySelectorAll('#str svg');

// Variabile per tenere traccia del voto
let rating = 0;

if(document.getElementById("voto") != null) 
	rating = document.forms["recensione"]["voto"].value;

console.log("rating: " + rating);
// messaggio di errore
let msgErr = document.getElementById("errRec");

// Aggiungi event listener per ogni stella
stars.forEach((star, index) => {
    // Al passaggio del mouse
    star.addEventListener('mouseover', () => {
		
		for(let i = 0; i < stars.length; i++) {
            stars[i].firstElementChild.setAttribute('fill', 'white');
        }
		
        // Rendi attive tutte le stelle fino a quella corrente
        for(let i = 0; i <= index; i++) {
            stars[i].firstElementChild.setAttribute('fill', 'black');
        }
    });

    // Quando il mouse esce
    star.addEventListener('mouseout', () => {
        // Se la stella non è stata cliccata, rendila non attiva
        for(let i = 0; i < stars.length; i++) {
            if(i >= rating) {
                stars[i].firstElementChild.setAttribute('fill', 'white');
            }
        }
        
        for(let i = 0; i < rating; i++) {
            stars[i].firstElementChild.setAttribute('fill', 'black');
        }
    });

    // Al click
    star.addEventListener('click', () => {
        // Imposta il voto e rendi attive tutte le stelle fino a quella corrente
        rating = index + 1;
        for(let i = 0; i <= index; i++) {
            stars[i].firstElementChild.setAttribute('fill', 'black');
        }
    });
});

function punteggioValido(value) {
    let num = parseInt(value);
    return !isNaN(num) && num >= 1 && num <= 5;
}

function commentoValido(txt) {
	
    let tempDiv = document.createElement('div');
    
    // Assegna l'input all'attributo 'innerText' dell'elemento
    // Questo converte automaticamente tutti i caratteri speciali in entità HTML
    tempDiv.innerHTML = txt;
    
    // Ottieni il testo sanitizzato
    let sanitizedInput = tempDiv.innerHTML;
    tempDiv.remove();
    
    return sanitizedInput;
}

function validateRecensione(event) {
	
	event.preventDefault();
	
	let punteggio = document.forms["recensione"]["voto"].value;
	let commento = document.forms["recensione"]["commento"].value;
	let productID = document.forms["recensione"]["ProductID"].value;
	let recensioneID = document.forms["recensione"]["recensioneID"].value;
	let action = "recensione";
	
	commento = commentoValido(commento);
	punteggio = rating;
	console.log(punteggio);

	if(!punteggioValido(punteggio)) {
		
		console.log("errore recensione");
		msgErr.style.display = "flex";
		
		setTimeout(() => {
        	msgErr.style.display = "none";
        }, 3000);
		
		return;
	}
	
	console.log(recensioneID);
	
	// se un recensioneID è settato allora esiste una recensione
	// quindi va trovata e modificata
	if(recensioneID != -1) {
		console.log("recensione ID: " + recensioneID);
		action = "modificaRecensione";
	}
	
	// se i dati sono validi, invia una richiesta AJAX al server
    let xhr = new XMLHttpRequest();
    xhr.open("POST", "/", true);
    xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    xhr.onreadystatechange = function(){
		
		// se il server restituisce un messaggio di errore, visualizzalo nel tag <p>
        if (this.readyState === XMLHttpRequest.DONE) {
			
		    if (this.status === 200) {
				
			    let val = JSON.parse(this.responseText);

			    if(val.val == 1) {
					console.log("si recensione");
					
					// pulisco tutto
			    	// document.forms["recensione"].reset();
					
					msgErr.innerHTML = "Qualcosa è andato storto";
					msgErr.style.display = "none";
					
				} else {
					console.log("no recensione");
					
					msgErr.innerHTML = "Il server ha restituito un errore";
					msgErr.style.display = "flex";
					
					setTimeout(function() {
						msgErr.style.display = "none";
					}, 3000); 
					return;
				}
		    } else if (this.status === 400 || this.status === 500) {
				// mappato con XML
		    }
		}
	}
	
	// invia i dati del form al server
    xhr.send("punteggio=" + punteggio + "&commento=" + commento + 
    "&productID=" + productID + "&recensioneID=" + recensioneID + 
    "&action=" + action);
	
	return false;
}
