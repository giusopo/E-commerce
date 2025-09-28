
let indirizzo = document.getElementById('indirizzo');
let extraFields = document.getElementById('extraFields');
let dropdowns = document.querySelectorAll('.dropdownExtra');
let inputs = document.getElementById('extraFields').querySelectorAll('input');
let error = document.getElementById('errorExtraFields');

let regione = [];
let provincia = [];
let comune = [];
let cap = [];

let jsons = {};

let nextInputMap = {
    "regione": "provincia",
    "provincia": "comune",
    "comune": "cap",
    "cap": "finish"
    // Non c'è un input successivo dopo "cap"
};

let previousInputMap = {
	// Non c'è un input precedente a "regione"
	"regione": "regione",
    "provincia": "regione",
    "comune": "provincia",
    "cap": "comune"
};

function capitalize(string) {
	
	if(string == "") {
		console.log("siamo vuoti");
		return string;
	} else {
    	return string.charAt(0).toUpperCase() + string.slice(1);
    }
}

Promise.all([
    fetch('../json/regione.json').then(response => response.json()),
    fetch('../json/province.json').then(response => response.json()),
    fetch('../json/comune.json').then(response => response.json()),
    fetch('../json/cap.json').then(response => response.json())
]).then(data => {
    regione = data[0].Foglio1.map(item => ({regione: item.regione, id: item.id_regione}));
    provincia = data[1].Foglio1.map(item => ({provincia: item.provincia, idRegione: item.id_regione, sigla: item.sigla}));
    comune = data[2].Foglio1.map(item => ({comune: item.comune, sigla: item.provincia, istat: item.istat}));
    cap = data[3].Foglio1.map(item => ({cap: item.cap, istat: item.istat}));

    jsons = {
        regione, 		// nome regione, id regione
        provincia,		// nome provincia, id regione appartenenza, sigla
        comune,			// nome comune, sigla provincia appartenenza, istat
        cap				// valore cap, istat comune a cui si riferisce
    };
}).catch(error => {
    console.error('Si è verificato un errore:', error);
});



// quando la casella di input viene lasciata controlla se il suo
// valore è valido, se non è così forza l'utente a restare sull'input
// mostrando un messaggio di errore
inputs.forEach(function(input) {
	
	// dropdown relativo all'input corrente
	let dropdown = Array.from(dropdowns).find(dropdown => dropdown.classList[1] == input.name);

    input.addEventListener('blur', function() {

		let nextInputName = nextInputMap[input.name];
		let inputNames = ["regione", "provincia", "comune", "cap"];
		
		// per assicurarsi che l'evento onblur
		// sia eseguito dopo onchange
		setTimeout(() => {
			
			if(input.value != "") {
				
				input.value = capitalize(input.value);
				
				// se il valore nell'input corrente esiste negli array regione, provincia, ...
				if(!jsons[input.name].some(item => item[input.name] == input.value)) {
				
					// se no allora il campo è errato
					input.focus();
					error.innerHTML = "Campo errato: " + input.name;
					error.style.display = "block";
					
					let index = inputNames.findIndex(name => name == input.name);
					index++;
					
					// azzera tutti gli input successivi
					for(let i = index; i < 4; i++) {
						
						document.getElementById(inputNames[i]).value = "";
						document.getElementById(inputNames[i]).disabled = true;
					}
					
				} else {
					// se si tutto ok
					error.innerHTML = "";
					error.style.display = "none";
					dropdown.style.display = "none";
					
					if(!controlloCoerenza("altro")) {
						// anche se singolarmente il campo è valido
						// i campi fra loro potrebbero essere incoerenti
						
						error.innerHTML = "incoerenza nei campi inseriti";
						error.style.display = "block";
						
						let index = inputNames.findIndex(name => name == input.name);
						index++;
						
						// azzera tutti gli input successivi
						for(let i = index; i < 4; i++) {
							
							document.getElementById(inputNames[i]).value = "";
							document.getElementById(inputNames[i]).disabled = true;
						}
					}
					
					if(nextInputName != "finish") {
						document.getElementById(nextInputName).disabled = false;
					}
				}
			} else {
				// se il campo è vuoto tutto ok
				error.innerHTML = "";
				error.style.display = "none";
				dropdown.style.display = "none";
				
				let index = inputNames.findIndex(name => name == input.name);
				index++;
					
				// azzera tutti gli input successivi
				for(let i = index; i < 4; i++) {
					
					document.getElementById(inputNames[i]).value = "";
					document.getElementById(inputNames[i]).disabled = true;
				}
			}
		}, 200);
    });
});

// controllo se i campi sono coerenti fra loro
export function controlloCoerenza(str) {
	
	console.log(str);
	
	let Reg = regione;
	let Prov = provincia;
	let Com = comune;
	let Ca = cap;
	
	let regInput = document.getElementById("regione");
	let provInput = document.getElementById("provincia");
	let comInput = document.getElementById("comune");
	let capInput = document.getElementById("cap");
	
	let regObj = Reg.find(reg => reg.regione == regInput.value);
	let provObj = {};
	let comObj = {};
	let capObj = {};
	
	if(provInput.value != "") {

		provObj = Prov.find(prov => prov.provincia == provInput.value);
		if(provObj.idRegione != regObj.id) {
			console.log("1"); 
			return false;
		}
	}
	
	if(comInput.value != "") {

		comObj = Com.find(com => com.comune == comInput.value);
		if(comObj.sigla != provObj.sigla) {
			console.log("2"); 
			return false;
		}
	}
	
	if(capInput.value != "") {

		capObj = Ca.find(ca => ca.istat == comObj.istat);
		let caps = Ca.filter(ca => ca.cap == capInput.value);
		if(!caps.includes(capObj)) {
			console.log("3"); 
			return false;
		}
	}
	return true;
}

// passa al prossimo input quando l'utente seleziona 
// un elemento dal dropdown e riempie l'input corrente 
// con il valore dell'elemento del dropdown
dropdowns.forEach(function(dropdown) {
    dropdown.addEventListener('mousedown', function(event) {
		
		// prende la seconda classe del dropdown che mi dice a che input si riferisce
        let type = dropdown.classList[1];
        // trova l'input a cui si riferisce il dropdown 
        let input = Array.from(inputs).find(input => input.name == type);
        
        if (input) {
			// trova il nome del prossimo input
            let nextInputName = nextInputMap[input.name];
            // trova il prossimo input nell'array di input
            let nextInput = Array.from(inputs).find(input => input.name == nextInputName);
            
            // se il prossimo input esiste attiva il focus su esso
            if (nextInput) {
                setTimeout(() => {
                    nextInput.focus();
                }, 300);
            }
            // assegna il valore del testo dell'evento generato 
            // da 'mousedown' al valore dell'input corrente
            input.value = event.target.textContent;
        }
    });
});

// ad ogni caratte immesso nell'input cerca valori che iniziano
// per il valore dell'input
inputs.forEach(function(input) {
	input.addEventListener('input', function() {
		
		// dropdown relativo all'input corrente
		let dropdown = Array.from(dropdowns).find(dropdown => dropdown.classList[1] == input.name);
	
		dropdown.innerHTML = '';

	    // in matches saranno contenuti i valori che iniziano con
	    // quanto scritto nell'input
	    if (input.value.length > 0) {
			
			let previousInputName = previousInputMap[input.name];
			let actualArray = filterBy(previousInputName, input.name);
			
	        let matches = actualArray.filter(function(item) {
			    return item[input.name].toLowerCase().startsWith(input.value.toLowerCase());
			});
	        
	        // nessun riscontro
	        if(matches.length == 0) {
				
		        dropdown.style.display = 'none';
				return null;
			} else {
		        // per ogni match genero un div che conterrà il testo che matcha
		        matches.forEach(function(match) {
					
		            let option = document.createElement('div');
		            option.classList.add("options");
		            option.textContent = match[input.name];
		            
		            // se un option viene cliccato scambio il valore dell'input
		            // con il testo dell'option desiderato
		            option.addEventListener('click', function() {
						
		                input.value = this.textContent;
		                dropdown.style.display = 'none';
		            });
		            // si aggiunge l'option alla lista
		            dropdown.appendChild(option);
		        });
		        
		        dropdown.style.display = 'block';
	        }
	    } else {
	        dropdown.style.display = 'none';
	    }
	});
});

function filterBy(previousInputName, currentInputName) {
    let filteredArray = [];
    let previousVal = document.getElementById(previousInputName).value;
    
    switch(currentInputName) {
        case "provincia":
            
            // Trova la regione
            let regioneMatch = regione.find(reg => reg.regione == previousVal);
            
            if (regioneMatch) {
                // Filtra le province che appartengono alla regione
                filteredArray = provincia.filter(prov => prov.idRegione == regioneMatch.id);
            }
            break;
        case "comune":

			// Trova la provincia
			let provinciaMatch = provincia.find(prov => prov.provincia == previousVal);

			if (provinciaMatch) {
				
				// Filtra i comuni che appartengono alla provincia
				filteredArray = comune.filter(com => com.sigla == provinciaMatch.sigla);
			}
            break;
        case "cap":

			// trova il comune            
            let comuneMatch = comune.find(com => com.comune == previousVal);
            
			if (comuneMatch) {
				
				// Filtra il cap del comune
				filteredArray = cap.filter(ca => ca.istat == comuneMatch.istat);
			}
            break;
            
        default:
            filteredArray = regione;
    }
    return filteredArray;
}


// gestiscono la visibilità del form di input
indirizzo.addEventListener('mouseover', function() {
    extraFields.style.display = 'flex';
});

indirizzo.addEventListener('mouseout', function(event) {
    if (!event.relatedTarget || !extraFields.contains(event.relatedTarget)) {
        extraFields.style.display = 'none';
    }
});

extraFields.addEventListener('mouseout', function(event) {
    if (!event.relatedTarget || !extraFields.contains(event.relatedTarget)) {
        extraFields.style.display = 'none';
    }
	indirizzo.placeholder = "";

	inputs.forEach((inp) => {

		if(inp.value != "") {
			indirizzo.placeholder += inp.value;
			indirizzo.placeholder += ", ";
		}
	});
	
	indirizzo.placeholder = indirizzo.placeholder.slice(0,-2);
});
////////////////////////////////////////////


