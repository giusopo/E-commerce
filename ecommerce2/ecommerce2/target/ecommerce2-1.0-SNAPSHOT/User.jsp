<%--
  Created by IntelliJ IDEA.
  User: utente
  Date: 25/04/2024
  Time: 15:37
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
    <head>
        <title>Title</title>
        <style>
            /* RESET CSS */
            * {
                margin: 0;
                padding: 0;
                border: 0;
                font-size: 100%;
                font: inherit;
                vertical-align: baseline;

                /* serve a gestire la grandezza di tutti gli elementi senza sfalsarli con il padding */
                box-sizing: border-box;

                font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            }
            <%@ include file="/Navbar/style/menuStyle.css" %>
        </style>
    </head>

    <body>
        <%@ include file="Navbar/menu.html" %>

        <h2>Login</h2>
        <form action="User" method="post">
            <label for="username">Username:</label><br>
            <input type="text" id="username" name="username"><br>

            <label for="password">Password:</label><br>
            <input type="password" id="password" name="password"><br>

            <!-- parametro che serve a vedere se l'utente vuole loggarsi o registrarsi -->
            <input type="hidden" name="action" value="login">

            <input type="submit" value="Login">
        </form>

        <h2>Register</h2>
        <form action="User" method="post">
            <label for="nome">Nome:</label><br>
            <input type="text" id="nome" name="nome"><br>

            <label for="cognome">Cognome:</label><br>
            <input type="text" id="cognome" name="cognome"><br>

            <label for="new_username">Username:</label><br>
            <input type="text" id="new_username" name="new_username"><br>

            <label for="dataNascita">Data di nascita:</label><br>
            <input type="date" id="dataNascita" name="dataNascita"><br>

            <label for="email">E-mail:</label><br>
            <input type="text" id="email" name="email"><br>

            <label for="new_password">Password:</label><br>
            <input type="password" id="new_password" name="new_password"><br>

            <!-- parametro che serve a vedere se l'utente vuole loggarsi o registrarsi -->
            <input type="hidden" name="action" value="register">

            <input type="submit" value="Register">
        </form>
    </body>
</html>
