
<%@ page import="Model.Bean.*" %>
<%@ page import="java.util.Iterator" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>



<%
    ProductBean product = (ProductBean) request.getAttribute("product");
%>

<!DOCTYPE html>
<html lang="it">

<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dettagli Prodotto</title>

    <link href="./style/menu-style.css" rel="stylesheet" type="text/css">
    <link href="./style/Dettagli-prodotto.css" rel="stylesheet" type="text/css">
    <!-- Footer -->
	<link rel="stylesheet" type="text/css" href="./style/footer.css">	
</head>
<body>
	<%@ include file="./html/menu.html" %>
	
    <main>
        <section class="product-detail">
            <div class="product-images">
                <img  id="main-image"  src="./getPicture?IDpic=<%=product.getID()%>&ruolo=principale" alt="Immagine di: '<%=product.getDescrizione()%>'">        
                <div class="thumbnail-images">
                    		    <img src="./getPicture?IDpic=<%=product.getID()%>&ruolo=principale" alt="Immagine di: '<%=product.getDescrizione()%>'">        
		   						<img src="./getPicture?IDpic=<%=product.getID()%>&ruolo=secondaria"  onerror="this.src='./img/beauty.png'"  alt="Immagine di: '<%=product.getDescrizione()%>'">        

                </div>
            </div>
            <div class="product-info">
                <h2><%=product.getNome()%></h2>
	                <p class="price">   
					    <% if(product.getPercentualeSconto() != 0) { %>
					        <span class="prezzo-scontato">
					            <%= product.getPrezzo() - ((product.getPrezzo() * product.getPercentualeSconto()) / 100) %> &euro;
					        </span>
					        <sup class="prezzo-originale">
					            <del><%= product.getPrezzo() %> &euro;</del>
					        </sup> 
					        <mark>-<%= product.getPercentualeSconto() %> %</mark>
					    <% } else { %>
					        <span class="prezzo-normale">
					            <%= product.getPrezzo() %> &euro;
					        </span>
					    <% } %>   
					                                  
					</p>
                <p class="description"> <%=product.getDescrizione()%> </p>
                <div class="product-details">	   
                     
				  <table>
				    
				    <tr>
				      <th>Grammatura</th>
				      <td><%=product.getGrammatura()%></td>
				    </tr>
				    <tr>
				      <th>Categoria</th>
				      <td><%=product.getCategoria()%></td>
				    </tr>
				    <tr>
				      <th>QTA in magazzino</th>
				      <td><%=product.getQTA_magazzino()%></td>
				    </tr>
				    <tr>
				      <th>Colorazione</th>
				      <td><%=product.getColorazione()%></td>
				    </tr>
				  </table>
		  
                <a href="/Cart?action=aggiungi&id=<%=product.getID()%>" class="add-to-cart">Aggiungi al Carrello</a>
            </div>
          </div>
        </section>
    </main>
    <script>
        document.addEventListener('DOMContentLoaded', () => {
            const thumbnails = document.querySelectorAll('.thumbnail-images img');
            const mainImage = document.getElementById('main-image');

            thumbnails.forEach(thumbnail => {
                thumbnail.addEventListener('click', () => {
                    mainImage.src = thumbnail.src;
                });
            });
        });
    </script>
    
    <%@ include file="html/footer.html" %>
</body>
</html>

