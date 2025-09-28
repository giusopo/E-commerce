<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>	
<meta name="google" content="notranslate">

    <meta charset="UTF-8">
    <link href="./style/menu-style.css" rel="stylesheet" type="text/css">
    <link href="./style/contatti.css" rel="stylesheet" type="text/css">
    
    <link rel="icon" href="img/logo.ico" type="image/x-icon">
    
    <!-- Footer -->
    <link rel="stylesheet" type="text/css" href="./style/footer.css">    
    <title>Contatti</title>
    
    <script>
        function inviaEmail(event) {
            event.preventDefault(); // Evita il comportamento predefinito di invio del form
            
            // Recupera i dati dal form
            var name = document.getElementById("name").value;
            var email = document.getElementById("email").value;
            var message = document.getElementById("message").value;
            
            // Costruisco il corpo dell'email
            var body = "Nome: " + name + "\n";
            body += "Email: " + email + "\n\n";
            body += "Messaggio:\n" + message;
            
            // Codice per aprire il client email predefinito del sistema utente
            var subject = "Messaggio dal sito web"; // Oggetto dell'email
            
            var mailtoLink = "mailto:megliomarco6@gmail.com"
                            + "?subject=" + encodeURIComponent(subject)
                            + "&body=" + encodeURIComponent(body);
            
            // Apre il client email predefinito del sistema utente
            window.location.href = mailtoLink;
        }
    </script>
</head>
<body>

    <%@ include file="./html/menu.html" %>

    <div class="container">
        <h1>Contatti</h1>
        
        <div class="contact-info">
            <h3>Informazioni di Contatto</h3>
            <p><strong>Indirizzo:</strong> Via Trenta, 30</p>
            <p><strong>Telefono:</strong> +39 333 361 8716</p>
            <p><strong>Email:</strong> megliomarco6@gmail.com</p>
        </div>
        
        <div class="contact-form">
            <h3>Invia un Messaggio</h3>
            <form onsubmit="inviaEmail(event)">
                <label for="name">Nome:</label>
                <input type="text" id="name" name="name" required>

                <label for="email">Email:</label>
                <input type="text" id="email" name="email" required>

                <label for="message">Messaggio:</label>
                <textarea id="message" name="message" required></textarea>

                <input type="submit" value="Invia">
            </form>
        </div>
    </div>
    <%@ include file="html/footer.html" %>
</body>
</html>
