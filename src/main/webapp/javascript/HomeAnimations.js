
document.addEventListener("DOMContentLoaded", function() {
    SetStars();
});

function SetStars() {
	
	fetch('./StarControl')
    .then(response => response.json())  // Trasforma la risposta in JSON
    .then(products => {					// 'products' è ora un array di oggetti JavaScript
    
    	//indice della card
    	let j = 0;		
        
        for (let product of products) {
			
			// Assicurati che la valutazione sia un numero
			let votazione = Number(product.votazione); 
			
			let DataInserimento = new Date(product.DataInserimento);
			
			let oggi = new Date();
			
			// Calcola la differenza in millisecondi
			var trascorso = oggi - DataInserimento;
			
			// Calcola il numero di millisecondi in 30 giorni
			var TrentaGiorni = 30 * 24 * 60 * 60 * 1000;
			
			let intero = 0;
			let DopoVirgola = 0;
			
			// indice delle stelle per ogni card
			let i = 1;		
			
			// elementi html
			let lastSVG;
			let polygon;
			let polygons;
			
			// prendo la parte intera della votazione
			intero = Math.floor(votazione);
			
			// se vero allora il prodotto avrà il banner NEW;
			// semplicemente aggiungo una classe ai div già esistenti (ma invisibili)
			// per mostrarli			
			if(trascorso <= TrentaGiorni) {
				
				var elements = document.querySelectorAll("#c" + j + " .new-banner, #c" + j + " .BackBanner");
				
			    elements.forEach(function(element) {
			        element.classList.add("activeBanner");
			    });		
			    
			    // il prodotto può essere nuovo e avere comunque tante recensioni
			    // quindi non ha senso togliere le stelline solo perchè è nuovo
			    // oppure ha poche recensioni
			    		
				//j++;
				//continue;
			}
            
            // il for parte da 1 perchè nella pseudo classe ":nth-child()"
            // non è possibile partire da 0
			for(i = 1; i <= intero; i++) {

				// Seleziona l'i-esima stella della j-esima card e aggiungi la classe "active"
    			document.querySelector('#s' + j + ' :nth-child(' + i + ') polygon').classList.add('active');
			}
			
			
			// se abbiamo la parte dopo la virgola allora sicuramente bisognerà aggiungere 
			// un nuovo poligono per generare l'effetto di una frazione di stella.
			// l'effetto è dato sovrapponendo una stella piena e una vuota,
			// mostrando solo una frazione di quella piena
			if(!Number.isInteger(votazione)) {
				
				// mi assicuro di prendere solo un numero dopo la virgola
				DopoVirgola = Math.floor((votazione - intero) * 10);
				
				// svg in cui bisogna aggiungere un altra stella per implementare la mezza stella
				lastSVG = document.querySelector('#s' + j + ' :nth-child(' + i + ')');
				
				// Crea un nuovo elemento polygon
			    polygon = document.createElementNS("http://www.w3.org/2000/svg", "polygon");
			    
			    // Imposta gli attributi del polygon
			    polygon.setAttribute("points", "25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18");
			    polygon.setAttribute("fill", "black");
			    polygon.setAttribute("stroke", "black");
			
			    // Aggiungi il polygon al SVG
			    lastSVG.appendChild(polygon);
			  
			    // Seleziona i figli (elementi polygon) dell'SVG
				polygons = lastSVG.children;
			}
			
			// verifico la parte dopo la virgola per capire che tipo di stella aggiungere
			
			if(DopoVirgola >= 4 && DopoVirgola <= 6) { // questa è una mezza stella
				
				if (polygons[0]) {
				    polygons[0].classList.add('full');
				}
				
				if (polygons[1]) {
				    polygons[1].classList.add('empty');
				}
				
			} else if(DopoVirgola > 0 && DopoVirgola <= 3) { // questo è un quarto di stella
				
				if (polygons[0]) {
				    polygons[0].classList.add('full');
				}
				
				if (polygons[1]) {
				    polygons[1].classList.add('UnQuarto');
				}
				
			} else if(DopoVirgola >= 7 && DopoVirgola <= 9) { // questi sono tre quarti di stella
				
				if (polygons[0]) {
				    polygons[0].classList.add('full');
				}
				
				if (polygons[1]) {
				    polygons[1].classList.add('TreQuarti');
				}
			}
			
			j++; // passo alla card successiva
        }
    })
    .catch(error => console.error('Si è verificato un errore:', error));
    
}

