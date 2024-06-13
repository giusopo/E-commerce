/**
 * 
 */

let errorMsg = document.getElementById("errorMessage");
let errorTxt = document.getElementById("errorTxt");
let requirements = document.getElementById("passRequirements");
let loginBtn = document.getElementById("defaultOpen");


 function isAllLetter(inputtxt) {
	  
  var letters = /^[A-Za-z]+$/;
  
  if(inputtxt.match(letters) || inputtxt == "") {
	  
  	// Per nascondere l'elemento
	errorMsg.classList.remove("visibleError");
	errorMsg.classList.add("hiddenError");
	
	// Svuoto il messaggio di errore
	errorTxt.innerHTML = "";

  	return true;
  	
  } else {
	    
 	// Per mostrare l'elemento
	errorMsg.classList.remove("hiddenError");
	errorMsg.classList.add("visibleError");
	
	// Aggiunge il messagigo di errore
	errorTxt.innerHTML = "solo lettere";
	return false;
  }
}

function isAlphanumeric(inputtxt) {
	
	var letters = /^[0-9a-zA-Z]+$/;
	if(inputtxt.match(letters) || inputtxt == "") {
		
		// Per nascondere l'elemento
		errorMsg.classList.remove("visibleError");
		errorMsg.classList.add("hiddenError");
		
		// Svuoto il messaggio di errore
		errorTxt.innerHTML = "";
		
	    return true;
	}
	
	// Per mostrare l'elemento
	errorMsg.classList.remove("hiddenError");
	errorMsg.classList.add("visibleError");
	
	// Aggiunge il messagigo di errore
	errorTxt.innerHTML = "solo lettere e numeri";
	return false;
}

function emailIsValid(inputtxt) {
    var mailformat = /^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/;

    if(inputtxt.match(mailformat) || inputtxt == "") {
		
		// Per nascondere l'elemento
		errorMsg.classList.remove("visibleError");
		errorMsg.classList.add("hiddenError");
		
		// Svuoto il messaggio di errore
		errorTxt.innerHTML = "";
		
        return true;
    } else {
		
		// Per mostrare l'elemento
		errorMsg.classList.remove("hiddenError");
		errorMsg.classList.add("visibleError");
		
		// Aggiunge il messagigo di errore
		errorTxt.innerHTML = "E-mail invalida";
		return false;
	}
}

function phoneNumberIsValid(inputtxt) {
	
	let phoneno = /^\+?([0-9]{2})\)?[-. ]?([0-9]{4})[-. ]?([0-9]{4})$/;

	if(inputtxt.match(phoneno) || inputtxt == "") {
		
		// Per nascondere l'elemento
		errorMsg.classList.remove("visibleError");
		errorMsg.classList.add("hiddenError");
		
		// Svuoto il messaggio di errore
		errorTxt.innerHTML = "";
		
		return true;
	}
	
	// Per mostrare l'elemento
	errorMsg.classList.remove("hiddenError");
	errorMsg.classList.add("visibleError");
	
	// Aggiunge il messagigo di errore
	errorTxt.innerHTML = "Numero di telefono invalido";
	
	return false;
}

function birthDateIsValid(birthDate) {
	// Verifica se la data inserita: 100AnniFa < birthDate < oggi
	
    // Converte la data di nascita in un oggetto Date
    let dateParts = birthDate.split("-");
    let dateObject = new Date(+dateParts[0], dateParts[1] - 1, +dateParts[2]);

    // Ottiene la data odierna e la data di 100 anni fa
    let today = new Date();
    let hundredYearsAgo = new Date();
    hundredYearsAgo.setFullYear(today.getFullYear() - 100);

    // Controlla se la data di nascita rientra nell'intervallo di tempo ragionevole
    if (dateObject > today || dateObject < hundredYearsAgo) {
		
		// Per mostrare l'elemento
		errorMsg.classList.remove("hiddenError");
		errorMsg.classList.add("visibleError");
		
		// Aggiunge il messagigo di errore
		errorTxt.innerHTML = "Data invalida";
  		return false;
    }

    // Controlla se la data di nascita è una data valida
    if (isNaN(dateObject.getTime())) {
		
		
		// Per mostrare l'elemento
		errorMsg.classList.remove("hiddenError");
		errorMsg.classList.add("visibleError");
		
		// Aggiunge il messagigo di errore
		errorTxt.innerHTML = "Data invalida";
  		return false;
    }
    
    // Per nascondere l'elemento
	errorMsg.classList.remove("visibleError");
	errorMsg.classList.add("hiddenError");
	
	// Svuoto il messaggio di errore
	errorTxt.innerHTML = "";
	return true;
}

function hideRequirements() {
	
	requirements.classList.remove("showRequirements");
}

function passwordIsValid(password) {
	
	requirements.classList.add("showRequirements");
	
    if (password.length < 8) {
		requirements.children[0].classList.remove("liGreen");
        requirements.children[0].classList.add("liRed");
    } else {
		requirements.children[0].classList.remove("liRed");
		requirements.children[0].classList.add("liGreen");
	}
	
    if (password.length > 20 || password == "") {
        requirements.children[1].classList.remove("liGreen");
        requirements.children[1].classList.add("liRed");
    } else {
		requirements.children[1].classList.remove("liRed");
		requirements.children[1].classList.add("liGreen");
	}
	
    if (!/[a-z]/.test(password)) {
        requirements.children[2].classList.remove("liGreen");
        requirements.children[2].classList.add("liRed");
    } else {
		requirements.children[2].classList.remove("liRed");
		requirements.children[2].classList.add("liGreen");
	}
	
    if (!/[A-Z]/.test(password)) {
        requirements.children[3].classList.remove("liGreen");
        requirements.children[3].classList.add("liRed");
    } else {
		requirements.children[3].classList.remove("liRed");
		requirements.children[3].classList.add("liGreen");
	}
	
    if (!/[0-9]/.test(password)) {
        requirements.children[4].classList.remove("liGreen");
        requirements.children[4].classList.add("liRed");
    } else {
		requirements.children[4].classList.remove("liRed");
		requirements.children[4].classList.add("liGreen");
	}
	
    if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]+/.test(password)) {
        requirements.children[5].classList.remove("liGreen");
        requirements.children[5].classList.add("liRed");
    } else {
		requirements.children[5].classList.remove("liRed");
		requirements.children[5].classList.add("liGreen");
	}
	
	if (password.length >= 8 && password.length <= 20 && /[a-z]/.test(password) && 
	/[A-Z]/.test(password) && /[0-9]/.test(password) && 
	/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]+/.test(password)) {
        
        return true;
        
    } else return false;
    
}




function isEmpty(form) {
	
    // Controlla il numero di proprietà nell'oggetto form
    var numProperties = Object.keys(form).length;

    // Se il form è il form di registrazione
    if (numProperties > 3) {
		
		console.log(form.nome);
		
        if (form.nome == null || form.nome == "" || form.cognome == null || form.cognome == "" || 
        form.username == null || form.username == "" || form.phoneNumber == null || form.phoneNumber == "" || 
        form.dataNascita == null || form.dataNascita == "" || form.email == null || form.email == "" || 
        form.password == null || form.password == "" || form.indirizzo == null || form.indirizzo == "") {
			
            return true;
        }
    } 
    // Se il form è il form di login
    else {
        if (form.username == null || form.username == "" || form.password == null || form.password == "") {
            return true;
        }
    }
    
    return false;
}


function validateRegister(event) {
	
	event.preventDefault();
	
    let nome = document.forms["register"]["nome"].value;
    let cognome = document.forms["register"]["cognome"].value;
    let username = document.forms["register"]["new_username"].value;
    let phoneNumber = document.forms["register"]["phoneNumber"].value;
    let dataNascita = document.forms["register"]["dataNascita"].value;
    let email = document.forms["register"]["email"].value;
    let indirizzo = document.forms["register"]["indirizzo"].value;
    let password = document.forms["register"]["new_password"].value;
    let action = "register";
    
    let form = {
		
		nome,
		cognome,
		username,
		phoneNumber,
		dataNascita,
		email,
		indirizzo,
		password,
		action
	}
	
	if(isEmpty(form) || !isAllLetter(form.nome) || !isAllLetter(form.cognome) || 
	!isAlphanumeric(form.username) || !birthDateIsValid(form.dataNascita) || 
	!emailIsValid(form.email) || !passwordIsValid(form.password) || !phoneNumberIsValid(form.phoneNumber)) {
		
		// Per mostrare l'elemento
		errorMsg.classList.remove("hiddenError");
		errorMsg.classList.add("visibleError");
		
		// Aggiunge il messagigo di errore
		errorTxt.innerHTML = "controlla meglio i dati inseriti";
		return;
	}
	
    // se i dati sono validi, invia una richiesta AJAX al server
    var xhr = new XMLHttpRequest();
    xhr.open("POST", "User", true);
    xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    xhr.onreadystatechange = function() {
		
        // se il server restituisce un messaggio di errore, visualizzalo nel tag <p>
        if (this.readyState === XMLHttpRequest.DONE) {
			
		    if (this.status === 200) {
				
				// sembra che questa riga in alcune circostanze scriva
				// il codice della home page nel paragrafo per gli errori :(
		        errorTxt.innerHTML = this.responseText;
		        
		        loginBtn.click();
		        
		        // se il form è inoltrato ckrrettamente
		        // pulisco tutto
		        
			    document.forms["register"].reset();
			    hideRequirements();
			    
			    // Per nascondere l'elemento
				errorMsg.classList.remove("visibleError");
				errorMsg.classList.add("hiddenError");
				
				// Svuoto il messaggio di errore
				errorTxt.innerHTML = "";
		        		        
		    } else if (this.status === 400 || this.status === 500) {

		        errorTxt.innerHTML = "Si è verificato un errore durante l'invio del form. Riprova più tardi.";
		    }
		}
    }
    
	// invia i dati del form al server
    xhr.send("nome=" + form.nome + "&cognome=" + form.cognome + 
    "&new_username=" + form.username + "&new_username=" + form.username + "&dataNascita=" + form.dataNascita + 
    "&email=" + form.email + "&new_password=" + form.password + 
    "&action=" + form.action);
    
	// previene la sottomissione del form
    return false; 
}


