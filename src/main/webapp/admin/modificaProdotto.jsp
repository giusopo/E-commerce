<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, Model.Bean.ProductBean, Model.CartModel" %>

<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link href="/style/HomeST.css" rel="stylesheet" type="text/css">
    <link href="/style/menu-style.css" rel="stylesheet" type="text/css">
    <link href="/style/Modifica-prodotto.css" rel="stylesheet" type="text/css">
    <link rel="icon" href="/img/adminLogo.ico" type="image/x-icon">
    
    <!-- Footer -->
    <link rel="stylesheet" type="text/css" href="/style/footer.css">
    <title>Beauty Blind</title>
    <style>
        
    </style>
</head>
<body>
    <%@ include file="/html/menu.html" %>

    <div class="form-container">
        <form action="/Admin" method="POST">
            <label for="product-select">Seleziona Prodotto:</label>
            <select name="id" id="product-select" required>
                <%
                    Collection<?> products = (Collection<?>) request.getSession().getAttribute("products");

                    if (products == null || products.isEmpty()) {
                        // Nessun prodotto trovato, reindirizza alla home
                        System.out.println("prodotti non caricati");
                        response.sendRedirect("/");
                        return;
                    }

                    for (Object obj : products) {
                        ProductBean bean = (ProductBean) obj;
                %>
                        <option value="<%= bean.getID() %>"><%= bean.getNome() %> (ID: <%= bean.getID() %>)</option>
                <%
                    }
                %>
            </select>
            <label for="sconto">Percentuale Sconto:</label>
            <input name="sconto" id="sconto" type="number" min="0" max="100" required>
            <input type="hidden" name="action" value="impostaSconto">
            <input type="submit" value="Imposta Sconto">
        </form>
    </div>

    <script type="text/javascript" src="./javascript/HomeAnimations.js"></script>
    <%@ include file="/html/footer.html" %>
</body>
</html>
