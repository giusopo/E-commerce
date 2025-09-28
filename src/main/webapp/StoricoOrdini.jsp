<%@ page contentType="text/html;charset=UTF-8" language="java" import="java.util.*,Model.Bean.*"%>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="google" content="notranslate">
<link rel="icon" href="img/documentLogo.ico" type="image/x-icon">

<title>Storico Ordini</title>
<style>
    body {
        font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        background-color: #f0f0f0;
        margin: 0;
        padding: 0;
    }
    .container {
        width: 80%;
        margin: 50px auto;
        background-color: #fff;
        padding: 20px;
        border-radius: 8px;
        box-shadow: 0 0 10px rgba(0, 0, 0, 0.1);
    }
    h2 {
        text-align: center;
        color: #333;
        margin-bottom: 20px;
    }
    .order-list {
        list-style: none;
        padding: 0;
    }
    .order-item {
        border-bottom: 1px solid #ddd;
        padding: 15px 0;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    .order-id {
        font-size: 18px;
        color: #007bff;
        font-weight: bold;
    }
    .order-link {
        text-decoration: none;
        color: #007bff;
        font-weight: bold;
        transition: color 0.3s ease;
    }
    .order-link:hover {
        color: #0056b3;
    }
</style>
</head>
<body>
    <div class="container">
        <h2>Storico Ordini</h2>
        <% 
            ArrayList<Integer> idOrdini = (ArrayList<Integer>) request.getAttribute("ordini");
            if (idOrdini != null && !idOrdini.isEmpty()) {
        %>
        <ul class="order-list">
            <% for (Integer id : idOrdini) { %>
            <li class="order-item">
                <span>ID ordine: <span class="order-id"><%= id %></span></span>
                <a class="order-link" href="/Cart?action=getOrdineStorico&id=<%= id %>">Dettagli</a>
            </li>
            <% } %>
        </ul>
        <% } else { %>
        <p>Nessun ordine trovato.</p>
        <% } %>
    </div>
</body>
</html>
