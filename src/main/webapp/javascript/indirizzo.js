
let indirizzo = document.getElementById('indirizzo');
let extraFields = document.getElementById('extraFields');
let dropdownR = document.getElementById('dropdownR');
let regioneInput = document.getElementById('regione');
let dropdowns = document.querySelectorAll('.dropdownExtra');
let inputs = document.getElementById('extraFields').querySelectorAll('input');
let error = document.getElementById('errorExtraFields');

let regione = [];
let provincia = [];
let comune = [];
let cap = [];

let jsons = {};

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
// valore è valito, se non è così forza l'utente a restare sull'input
// mostrando un messaggio di errore
inputs.forEach(function(input) {
	
	// dropdown relativo all'input corrente
	let dropdown = Array.from(dropdowns).find(dropdown => dropdown.classList[1] == input.name);

    input.addEventListener('blur', function() {

		let nextInputName = "";
		
		switch(input.name) {
		    case "regione":
		        nextInputName = "provincia";
		        break;
		    case "provincia":
		        nextInputName = "comune";
		        break;
		    case "comune":
		        nextInputName = "cap";
		        break;
		    case "cap":
		        nextInputName = "finish";
		        break;
		}
		
		// per assicurarsi che l'evento onblur
		// sia eseguito dopo onchange
		setTimeout(() => {
			
			if(input.value != "") {
				
				input.value = capitalize(input.value);
				
				if(!jsons[input.name].some(item => item[input.name] == input.value)) {
				
					console.log("errore nel blur");
					input.focus();
					error.innerHTML = "Campo errato: " + input.name;
					error.style.display = "block";
					document.getElementById(nextInputName).value = "";
					document.getElementById(nextInputName).disabled = true;
				} else {
					
					error.innerHTML = "";
					error.style.display = "none";
					dropdown.style.display = "none";
					document.getElementById(nextInputName).disabled = false;
				}
			} else {
				error.innerHTML = "";
				error.style.display = "none";
				dropdown.style.display = "none";
				document.getElementById(nextInputName).value = "";
				document.getElementById(nextInputName).disabled = true;
			}
		}, 200);
    });
});

// quando viene selezionato un elemento diverso della dropdown
// lo inserisce nel campo input

dropdowns.forEach(function(dropdown) {
	
	dropdown.addEventListener('change', function() {
    	
    	// mi dice a che input si riferisce 
    	// la dropdown (regione, provincia ...)
		let type = dropdown.classList[1];
		
		// trova l'input con lo stesso nome
		let input = Array.from(inputs).find(input => input.name == type);
		
		if (input) {
			input.value = this.value;
		}
    });
});

inputs.forEach(function(input) {
	input.addEventListener('input', function() {
		
		// dropdown relativo all'input corrente
		let dropdown = Array.from(dropdowns).find(dropdown => dropdown.classList[1] == input.name);
	
		dropdown.innerHTML = '';

	    // in matches saranno contenuti i valori che iniziano con
	    // quanto scritto nell'input
	    if (input.value.length > 0) {
			
	        let matches = jsons[input.name].filter(function(item) {
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
});
////////////////////////////////////////////


