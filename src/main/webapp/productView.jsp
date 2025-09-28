
<%@ page import="Model.Bean.*" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.ArrayList" %>

<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>



<%
    ProductBean product = (ProductBean) request.getAttribute("product");
	RecensioneBean recensione = (RecensioneBean) request.getAttribute("recensione");
	
	// lista di tutte le recensioni del prodotto
	ArrayList<RecensioneBean> listaRecensioni = (ArrayList<RecensioneBean>) request.getAttribute("listaRecensioni");
	System.out.println("lista recensioni:");
	System.out.println(listaRecensioni);
	
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
	            
				<%
				Integer userIDobj = (Integer)request.getSession().getAttribute("userID");
				
				int voto = 0;
			    String commento = "";
			    
			    if(recensione.getID() != -1) {
			    	// l'utente ha lasciato una recensione
			    	voto = recensione.getVotazione();
			    	commento = recensione.getCommento();
			    }
				
				if(userIDobj != null) {
				    int userID = userIDobj.intValue();
				    System.out.println("userID: "+userID);
				%>            
	            <div class="containerRecensione">
					 <h2>Lascia una recensione</h2>
					
					 <div class = "stars" id="str">
					 
					 	<%if(voto == 0) {%>
		                    <svg width="20" height="20" viewBox="0 0 50 50">
							    <polygon points="25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18" fill="white" stroke="black"/>
							</svg>
							
							<svg width="20" height="20" viewBox="0 0 50 50">
							    <polygon points="25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18" fill="white" stroke="black"/>
							</svg>
							
							<svg width="20" height="20" viewBox="0 0 50 50">
							    <polygon points="25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18" fill="white" stroke="black"/>
							</svg>
							
							<svg width="20" height="20" viewBox="0 0 50 50">
							    <polygon points="25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18" fill="white" stroke="black"/>
							</svg>
							
							<svg width="20" height="20" viewBox="0 0 50 50">
							    <polygon points="25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18" fill="white" stroke="black"/>
							</svg>
						<%} else {
							
								int i = 0;
							
								for(; i < voto; i++) { %>
									<svg width="20" height="20" viewBox="0 0 50 50">
									    <polygon points="25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18" fill="black" stroke="black"/>
									</svg>
								<%}
							
								for(; i < 5; i++) { %>
									<svg width="20" height="20" viewBox="0 0 50 50">
									    <polygon points="25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18" fill="white" stroke="black"/>
									</svg>
								<%}
								
							}%>
					  </div>
					  
					  <div class="recensione">
					  	<p id="errRec" style="display:none">
						  	<img src="/img/errorFormValidation.png" alt="error">
						  	Qualcosa è andato storto
					  	</p>
					  	
			  			<form name="recensione" onsubmit="validateRecensione(event)">
			  				<textarea id="commento" class="commentoClass" name="commento" placeholder="Inserisci un commento..."><%=commento%></textarea>
			  				<input type="hidden" id="voto" name="voto" value="<%=voto%>" required>
			  				<input type="hidden" id="ProductID" name="ProductID" value="<%=product.getID()%>" required>
			  				<input type="hidden" id="recensioneID" name="recensioneID" value="<%=recensione.getID()%>" required>
			  				
			  				<%if(recensione.getID() == -1) {%>
			  					<input type="submit" value="invia" class="add-to-cart">
			  				<%} else {%>
			  					<input type="submit" value="modifica" class="add-to-cart">
			  				<%}%>
			  			</form>
			  			
			  		  </div>
		  		</div>
	  		<%} else {%>
	  			<a href="/LoginRegistrazione.jsp">Lascia una recensione come utente</a>
	  		<%}%>
	  			<h2>Recensioni di altri utenti</h2>
		  		<div class="containerRecensioni">
		  		
		  			<%	if(listaRecensioni.size() > 0) { 
		  			
			  				for(int i = 0; i < listaRecensioni.size(); i++) {
			  			
				  				int j = 0;
				  				
				  				for(; j < listaRecensioni.get(i).getVotazione(); j++) {
				  				
			  					%>
			  						<svg width="20" height="20" viewBox="0 0 50 50">
									   <polygon points="25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18" fill="black" stroke="black"/>
									</svg>
			  					<%}
			  					
		  						for(; j < 5; j++) {
		  							
		  							%>
				  						<svg width="20" height="20" viewBox="0 0 50 50">
										   <polygon points="25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18" fill="white" stroke="black"/>
										</svg>
				  					<%
		  						}
			  				
		  						j = 0;
				  			
				  				%><div class="rec">
					  				<textarea id="recCommento" class="commentoClass" name="recCommento" placeholder="nessun commento..." disabled><%=listaRecensioni.get(i).getCommento()%></textarea>
				  				</div>
		  					<%}%>
		  				<%} else {%>
		  				
		  					<p>Il prodotto non ha recensioni al momento...</p>
		  				<%}%>
		  		</div>
          	</div>
        </section>
    </main>
    
    <script type="text/javascript" src="./javascript/recensioniProdotto.js"></script>
    <%@ include file="html/footer.html" %>
</body>
</html>

