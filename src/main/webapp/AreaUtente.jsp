<%@ page contentType="text/html;charset=UTF-8" language="java" import="java.util.*,Model.Bean.*"%>
<html>
    <head>
        <title>Title</title>
        <link href="./style/menu-style.css" rel="stylesheet" type="text/css">
        <link href="./style/AreaUtente.css" rel="stylesheet" type="text/css">
        <!-- Footer -->
		<link rel="stylesheet" type="text/css" href="./style/footer.css">	
    </head>
    <body>
        <%@ include file="./html/menu.html" %>
        <%
        	UserBean user;
        
        	user = (UserBean) request.getAttribute("datiUtente");
            if (user != null) {
            	System.out.println(user.toString());
        %>
        <main>
            <div class="data-container">
                <section class="user-profile">
                    <h2>Bentornato <%= request.getSession().getAttribute("username")%></h2>
                    <div class="user-info">
                        <p><strong>Nome:</strong> <%= user.getNome() %> </p>
                        <p><strong>Cognome:</strong> <%= user.getCognome() %> </p>
                        <p><strong>Username:</strong> <%= user.getUserName() %> </p>
                        <p><strong>Email:</strong> <%= user.getEmail() %> </p>
                        <p><strong>Indirizzo:</strong> <%= user.getIndirizzoBase() %> </p>
                        <p><strong>Numero di telefono:</strong> <%= user.getNumeroDiTelefono() %> </p>
                        <a href="/User?action=logout">Logout</a>
                    </div>
                </section>
                <section class="user-actions">
                    <h2>Azioni</h2>
                    <div class="actions">
                        <button onclick="location.href='#'">Storico Ordini</button>
                        <button onclick="location.href='#'">Carrello</button>
                        <button onclick="location.href='#'">Impostazioni Account</button>
                        <button onclick="location.href='#'">Logout</button>
                    </div>
                    
                </section>
            </div>
        </main>
        <%}else{%>
              <p>Errore recupero dati dell'utente</p>                                         
        <%} %>
        
        <%@ include file="html/footer.html" %>
    </body>
</html>
