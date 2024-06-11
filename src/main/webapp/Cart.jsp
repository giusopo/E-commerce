<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page contentType="text/html; charset=UTF-8" 
    import="java.util.*,Model.Bean.*,Model.CartModel"%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<link href="./style/Carrello.css" rel="stylesheet" type="text/css">
	<link href="./style/menu-style.css" rel="stylesheet" type="text/css">
	  <!-- Footer -->
	<link rel="stylesheet" type="text/css" href="./style/footer.css">	
	<title>Carrello</title>
</head>
<body>
    <%@ include file="./html/menu.html" %>

    <%
        CartBean cart = (CartBean) request.getSession().getAttribute("cart");
        double prezzoTotale = 0;

        if(cart == null) {
            response.sendRedirect("./Cart?action=visualizza"); 
            return;
        }
    %>

    <div class="cart-container">
        <% if(cart != null && !cart.getProducts().isEmpty()) { %>
            <h2>Il tuo carrello</h2>
            <div class="cart-items">
                <%
                    Collection<?> products = (Collection<?>) cart.getProducts();
                    Iterator<?> it = products.iterator();
                    while (it.hasNext()) {
                        ProductBean bean = (ProductBean) it.next();
                        prezzoTotale += bean.getPrezzo();
                %>
                <div class="cart-item">
                    <img src="./getPicture?IDpic=<%=bean.getID()%>&ruolo=principale" alt="Immagine di: '<%=bean.getDescrizione()%>'">
                    <div class="item-details">
                        <h3><%=bean.getNome()%></h3>
                        <p>
                        <% if(bean.getPercentualeSconto() != 0) { %>
					        <span class="prezzo-scontato">
					            <%= bean.getPrezzo() - ((bean.getPrezzo() * bean.getPercentualeSconto()) / 100) %> &euro;
					        </span>
					        <mark>-<%= bean.getPercentualeSconto() %> %</mark>
					    <% } else { %>
					        <span class="prezzo-normale">
					            <%= bean.getPrezzo() %> &euro;
					        </span>
					    <% } %>   </p>
                        <p>Colorazione: <%=bean.getColorazione()%></p>
                        <div class="quantity-controls">
                            <button onclick="location.href='./Cart?action=decrementaQTA&id=<%=bean.getID()%>'">-</button>
                            <input type="text" value="<%=bean.getQTA_ordinata()%>" disabled>
                            <button onclick="location.href='./Cart?action=aumentaQTA&id=<%=bean.getID()%>'">+</button>
                        </div>
                        <button class="remove-item" onclick="location.href='./Cart?action=elimina&id=<%=bean.getID()%>'">Rimuovi</button>
                    </div>
                </div>
                <% } %>
            </div>

            <div class="order-summary">
                <p>Totale ordine: <strong><%=prezzoTotale%> €</strong></p>
                <p>Spedizione: <strong>Gratuita</strong></p>
                <p>Totale: <strong><%=prezzoTotale%> €</strong></p>
                <form action="Cart" method="post">
                    <button type="submit">Conferma ordine</button>
                </form>
            </div>
        <% } else { %>
            <div class="empty-cart">
                <p>Il carrello è vuoto</p>
            </div>
        <% } %>
    </div>
    
    <%@ include file="html/footer.html" %>
</body>
</html>
