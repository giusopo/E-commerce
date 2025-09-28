
import { controlloCoerenza } from "./indirizzo.js";

let errorMsg = document.getElementById("errorMessage");
let errorTxt = document.getElementById("errorTxt");
let errorMsg2 = document.getElementById("errorMessage2");
let errorTxt2 = document.getElementById("errorTxt2");
let requirements = document.getElementById("passRequirements");
let loginBtn = document.getElementById("defaultOpen");

let usernameLog = document.getElementById("username");
let passwordLog = document.getElementById("password");

let usernameReg = document.getElementById("new_username");

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

function isAlphanumeric(inputtxt, errMsg, errTxtMsg) {
	
	var letters = /^[0-9a-zA-Z]+$/;
		
	if(inputtxt.match(letters) || inputtxt == "") {
		
		// Per nascondere l'elemento
		errMsg.classList.remove("visibleError");
		errMsg.classList.add("hiddenError");
		
		// Svuoto il messaggio di errore
		errTxtMsg.innerHTML = "";
		
	    return true;
	}
	
	// Per mostrare l'elemento
	errMsg.classList.remove("hiddenError");
	errMsg.classList.add("visibleError");
	
	// Aggiunge il messagigo di errore
	errTxtMsg.innerHTML = "solo lettere e numeri";
	errTxtMsg.style.display = "block";
	
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

function passwordIsValidReg(password) {
	
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

function passwordIsValid(password, errTxtMsg) {
	
	
	if(password == "") {
		
		errTxtMsg.style.display = "none";
        errTxtMsg.parentNode.classList.remove("visibleError");
        errTxtMsg.parentNode.classList.add("hiddenError");
        
        	console.log("password");
        return false;
	}
	
	if (password.length < 8) {
		console.log("<8")
        errTxtMsg.textContent = "La password deve contenere almeno 8 caratteri.";
        errTxtMsg.style.display = "block";
        errTxtMsg.parentNode.classList.remove("hiddenError");
        errTxtMsg.parentNode.classList.add("visibleError");
        
    } else if (password.length > 20 || password == "") {
        errTxtMsg.textContent = "La password non deve superare i 20 caratteri o essere vuota.";
        errTxtMsg.style.display = "block";
        errTxtMsg.parentNode.classList.remove("hiddenError");
        errTxtMsg.parentNode.classList.add("visibleError");

    } else if (!/[a-z]/.test(password)) {
        errTxtMsg.textContent = "La password deve contenere almeno una lettera minuscola.";
        errTxtMsg.style.display = "block";
        errTxtMsg.parentNode.classList.remove("hiddenError");
        errTxtMsg.parentNode.classList.add("visibleError");
       
    } else if (!/[A-Z]/.test(password)) {
        errTxtMsg.textContent = "La password deve contenere almeno una lettera maiuscola.";
        errTxtMsg.style.display = "block";
        errTxtMsg.parentNode.classList.remove("hiddenError");
        errTxtMsg.parentNode.classList.add("visibleError");
        
    } else if (!/[0-9]/.test(password)) {
        errTxtMsg.textContent = "La password deve contenere almeno un numero.";
        errTxtMsg.style.display = "block";
        errTxtMsg.parentNode.classList.remove("hiddenError");
        errTxtMsg.parentNode.classList.add("visibleError");
       
    } else if (!/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]+/.test(password)) {
        errTxtMsg.textContent = "La password deve contenere almeno un carattere speciale.";
        errTxtMsg.style.display = "block";
        errTxtMsg.parentNode.classList.remove("hiddenError");
        errTxtMsg.parentNode.classList.add("visibleError");

    } else {
        errTxtMsg.style.display = "none";
        errTxtMsg.parentNode.classList.remove("visibleError");
        errTxtMsg.parentNode.classList.add("hiddenError");
	}
	
	if (password.length >= 8 && password.length <= 20 && /[a-z]/.test(password) && 
	/[A-Z]/.test(password) && /[0-9]/.test(password) && 
	/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]+/.test(password)) {
		
        return true;
    } else {
		
			console.log("passwordvalid");

		return false;
	}
}

function isEmpty(form) {
	
    // Controlla il numero di proprietà nell'oggetto form
    var numProperties = Object.keys(form).length;
    
    // Se il form è il form di registrazione
    if (numProperties > 3) {
		
        if (form.nome == null || form.nome == "" || form.cognome == null || form.cognome == "" || 
        form.username == null || form.username == "" || form.phoneNumber == null || form.phoneNumber == "" || 
        form.dataNascita == null || form.dataNascita == "" || form.email == null || form.email == "" || 
        form.password == null || form.password == "") {
			
            return true;
        }
    } 
    // Se il form è il form di login
    else {
        if (form.username == null || form.username == "" || form.password == null || form.password == "") {
            return true;
        }
    }
    
    console.log("non vuoto");
    return false;
}

usernameLog.addEventListener("input", () => {
	
	isAlphanumeric(usernameLog.value, errorMsg2, errorTxt2)
});

passwordLog.addEventListener("input", () => {

	passwordIsValid(passwordLog.value, errorTxt2);
});

function validateLogin(event) {
	
	event.preventDefault();
	
	let username = document.getElementById("username").value;
    let password = document.getElementById("password").value;
    let action = "login";
    
    let form = {
		
		username,
		password,
		action
	}
	
	if(isEmpty(form) || !isAlphanumeric(form.username, errorMsg2, errorTxt2) || 
	!passwordIsValid(form.password, errorTxt2)) {
		
		console.log("empty,!alpha, ecc "+errorMsg2)
		// Per mostrare l'elemento
		errorMsg2.classList.remove("hiddenError");
		errorMsg2.classList.add("visibleError");
		
		// Aggiunge il messagigo di errore
		errorTxt2.innerHTML = "controlla meglio i dati inseriti";
		return;
	}
	
	// se i dati sono validi, invia una richiesta AJAX al server
    let xhr = new XMLHttpRequest();
    xhr.open("POST", "User", true);
    xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    xhr.onreadystatechange = function() {
		
		// se il server restituisce un messaggio di errore, visualizzalo nel tag <p>
        if (this.readyState === XMLHttpRequest.DONE) {
			
		    if (this.status === 200) {

			    let val = JSON.parse(this.responseText);
			    
			    if(val.val == 1) {
					// portare all'area utente
					console.log("si log");
					
					// pulisco tutto
			    	document.forms["login"].reset();
					
					// Per nascondere l'elemento
					errorMsg2.classList.remove("visibleError");
					errorMsg2.classList.add("hiddenError");
					
					// Svuoto il messaggio di errore
					errorTxt2.innerHTML = "";
					errorTxt2.style.display = "none";
					
					window.location.href = "http://localhost:8080/User?action=DatiUtente";
				} else {
					// restare nel login
					console.log("no log");
					
					// per mostrare l'elemento
					errorMsg2.classList.remove("hiddenError");
					errorMsg2.classList.add("visibleError");
					
					// inserire errore
					errorTxt2.innerHTML = "Account non trovato";
					errorTxt2.style.display = "block";
					
					setTimeout(function() {
						// Per nascondere l'elemento
						errorMsg2.classList.remove("visibleError");
						errorMsg2.classList.add("hiddenError");
						
						// Svuoto il messaggio di errore
						errorTxt2.innerHTML = "";
					    errorTxt2.style.display = "none";
					}, 3000); 
					
					return;
				}
		    } else if (this.status === 400 || this.status === 500) {

				console.log("erroraccio");	
				return;    
			}
		}
	}
	
	// invia i dati del form al server
    xhr.send("username=" + form.username + "&password=" + form.password + 
    "&action=" + form.action);
}

usernameReg.addEventListener("input", () => {
	
	isAlphanumeric(usernameReg.value, errorMsg, errorTxt);;
})

function validateRegister(event) {
	
	event.preventDefault();
	
    let nome = document.forms["register"]["nome"].value;
    let cognome = document.forms["register"]["cognome"].value;
    let username = document.forms["register"]["new_username"].value;
    let phoneNumber = document.forms["register"]["phoneNumber"].value;
    let dataNascita = document.forms["register"]["dataNascita"].value;
    let email = document.forms["register"]["email"].value;
    let indirizzo = document.forms["register"]["indirizzo"].placeholder;
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
	!isAlphanumeric(form.username, errorMsg, errorTxt) || !birthDateIsValid(form.dataNascita) || 
	!emailIsValid(form.email) || !passwordIsValid(form.password, errorMsg) || !phoneNumberIsValid(form.phoneNumber)||
	!controlloCoerenza("form val")) {
		
		console.log("err");
		// Per mostrare l'elemento
		errorMsg.classList.remove("hiddenError");
		errorMsg.classList.add("visibleError");
		
		// Aggiunge il messagigo di errore
		errorTxt.innerHTML = "controlla meglio i dati inseriti";
		return;
	}
	
    // se i dati sono validi, invia una richiesta AJAX al server;
    // invia una richiesta all' url User perchè è come l'abbiamo mappata
    let xhr = new XMLHttpRequest();
    xhr.open("POST", "User", true);
    xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    xhr.onreadystatechange = function() {
		
        // se il server restituisce un messaggio di errore, visualizzalo nel tag <p>
        if (this.readyState === XMLHttpRequest.DONE) {
			
		    if (this.status === 200) {
				
				let val = JSON.parse(this.responseText);

			    if(val.val == 1) {
					
					console.log("si reg");
			        loginBtn.click();
			        
			        document.forms["register"].reset();
				    hideRequirements();
				    
				    // Per nascondere l'elemento
					errorMsg.classList.remove("visibleError");
					errorMsg.classList.add("hiddenError");
					
					// Svuoto il messaggio di errore
					errorTxt.innerHTML = "";

				} else {
					
					console.log("no reg");
					
					// Per nascondere l'elemento
					errorMsg.classList.remove("hiddenError");
					errorMsg.classList.add("visibleError");
					
					// Svuoto il messaggio di errore
					errorTxt.innerHTML = "Impossibile registrare questi dati";
					return;
				}
		        
		    } else if (this.status === 400 || this.status === 500) {
				return;
				// XML
		        //errorTxt.innerHTML = "Si è verificato un errore durante l'invio del form. Riprova più tardi.";
		    }
		}
    }
    
	// invia i dati del form al server
    xhr.send("nome=" + form.nome + "&cognome=" + form.cognome + "&phoneNumber=" + form.phoneNumber +
    "&new_username=" + form.username + "&new_username=" + form.username + "&dataNascita=" + form.dataNascita + 
    "&email=" + form.email+ "&indirizzo=" + form.indirizzo + "&new_password=" + form.password + 
    "&action=" + form.action);
    
	// previene la sottomissione del form
    return false; 
}

window.isAllLetter = isAllLetter;
window.isAlphanumeric = isAlphanumeric;
window.emailIsValid = emailIsValid;
window.phoneNumberIsValid = phoneNumberIsValid; 
window.birthDateIsValid = birthDateIsValid;
window.hideRequirements = hideRequirements;
window.passwordIsValidReg = passwordIsValidReg;
window.passwordIsValid = passwordIsValid;
window.isEmpty = isEmpty;
window.validateLogin = validateLogin;
window.validateRegister = validateRegister;
