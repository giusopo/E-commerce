<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    <%@ page contentType="text/html; charset=UTF-8" 
	import="java.util.*,Model.Bean.*,Model.CartModel"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
	<link href="./style/menu-style.css" rel="stylesheet" type="text/css">
	<link href="./style/Tabelle.css" rel="stylesheet" type="text/css">


<title>Carrello</title>
</head>
<body>
        <%@ include file="HTML/menu.html" %>
        
        <%
                CartBean cart = (CartBean) request.getSession().getAttribute("cart");
        		double prezzoTotale = 0;

	            if(cart == null) {
	        		System.out.println("NIENTE PRODOTTI");
	        		response.sendRedirect("./Cart?action=visualizza");	

	        		return;
	        	}
        %>
  
		<% if(cart != null && !cart.getProducts().isEmpty()) { %>

            <div class="contenitore-tabella">
                <h2>Cart</h2>
           
                <table>
                <tr>
                    <th>Immagine</th>
                    <th>Nome</th>
                    <th>Prezzo</th>
                    <th>percentuale sconto</th>
                    <th>Colorazione</th>
                    <th>Quantità ordinata</th>
                    <th>azioni</th>
                </tr>

                <%
                    Collection<?> products = (Collection<?>) cart.getProducts();
                   
                    Iterator<?> it = products.iterator();
                    while (it.hasNext()) {
                    	ProductBean bean = (ProductBean) it.next();
                        prezzoTotale += bean.getPrezzo();
                        
                %>

                <tr>
                
                    <td><img src="./getPicture?IDpic=<%=bean.getID()%>" alt="Immagine di: '<%=bean.getFirstImages()%>'"></td>
                    <td><%=bean.getNome()%></td>
                    <td><%=bean.getPrezzo()%> $</td>
                    <td><%=bean.getPercentualeSconto()%></td>
                    <td><%=bean.getColorazione()%></td>
                    <td><%=bean.getQTA_ordinata()%></td>
                    <td>
                        <ul class="lista-azioni-carrello">
                            <li class="azione-carrello"><a href="./?action=read&id=<%=bean.getID()%>">Dettagli prodotto</a></li>
                            <li class="azione-carrello"><a href="./Cart?action=aumentaQTA&id=<%=bean.getID()%>">Aumenta QTA</a></li>
                            <li class="azione-carrello"><a href="./Cart?action=decrementaQTA&id=<%=bean.getID()%>">Decrementa QTA</a></li>
                            <li class="azione-carrello"><a href="./Cart?action=elimina&id=<%=bean.getID()%>">Elimina</a></li>          
                        </ul>
                    </td>
                </tr>

                <% } %>

                	
                
           </table>
           
           <div class="box-di-servizio-ordine">
                <span>Totale ordine : <%=prezzoTotale%> $ </span>
                <span>spedizione : gratuita </span>
                <span>Totale : <%=prezzoTotale%> $ </span>
                	
                <form action="Cart" method="post">
                    <input type="submit" value="Conferma ordine">
                </form>
           </div>           	
        </div>
        <% } else if(cart.getProducts().isEmpty()) {%>
        
        <div class="box-di-servizio-ordine">
               <span>Il carrello è vuoto</span>
                	
           </div>
        
       	<%}%>
        

</body>
</html>