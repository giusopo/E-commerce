<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, Model.Bean.UserBean" %>

<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link href="/style/HomeST.css" rel="stylesheet" type="text/css">
    <link href="/style/menu-style.css" rel="stylesheet" type="text/css">
    <link rel="icon" href="/img/adminLogo.ico" type="image/x-icon">
    
    <!-- Footer -->
    <link rel="stylesheet" type="text/css" href="./style/footer.css">
    <title>Beauty Blind</title>
    <style>
        .table-container {
            margin: 20px;
        }

        .user-table {
            width: 100%;
            border-collapse: collapse;
        }

        .user-table, .user-table th, .user-table td {
            border: 1px solid #ddd;
        }

        .user-table th, .user-table td {
            padding: 12px;
            text-align: left;
        }

        .user-table th {
            background-color: #f2f2f2;
        }

        .user-table tr:nth-child(even) {
            background-color: #f9f9f9;
        }

        .user-table tr:hover {
            background-color: #f1f1f1;
        }

        .table-title {
            text-align: center;
        }
    </style>
</head>
<body>
    <%@ include file="/html/menu.html" %>

    <div class="table-container">
        <h2 class="table-title">Elenco Utenti</h2>
        <%
            Collection<?> utenti = (Collection<?>) request.getSession().getAttribute("listaUtenti");
            if (utenti == null || utenti.isEmpty()) {
                // Nessun utente trovato, reindirizza alla pagina di gestione utenti
                response.sendRedirect(request.getContextPath() + "/Admin?action=visualizzaUtenti");
                return;
            } else {
        %>
                <table class="user-table">
                    <thead>
                        <tr>
                            <th>Nome</th>
                            <th>Cognome</th>
                            <th>Username</th>
                            <th>Data di Nascita</th>
                            <th>Email</th>
                            <th>Numero di Telefono</th>
                            <th>Indirizzo</th>
                        </tr>
                    </thead>
                    <tbody>
                        <%
                            for (Object obj : utenti) {
                                UserBean user = (UserBean) obj;
                        %>
                        <tr>
                            <td><%= user.getNome() %></td>
                            <td><%= user.getCognome() %></td>
                            <td><%= user.getUserName() %></td>
                            <td><%= user.getDataNascita().toString() %></td>
                            <td><%= user.getEmail() %></td>
                            <td><%= user.getPhoneNumber() %></td>
                            <td><%= user.getIndirizzoBase() %></td>
                        </tr>
                        <%
                            }
                        %>
                    </tbody>
                </table>
        <%
            }
        %>
    </div>

    <%@ include file="/html/footer.html" %>
</body>
</html>
