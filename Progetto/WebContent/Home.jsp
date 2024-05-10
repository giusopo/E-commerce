<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page contentType="text/html; charset=UTF-8" 
import="java.util.*,Model.Bean.ProductBean,Model.CartModel"%>

<!DOCTYPE html>
<html>

<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<link href="./style/HomeST.css" rel="stylesheet" type="text/css">
	<link href="./style/menu-style.css" rel="stylesheet" type="text/css">
	
	<title>Beauty Blind</title>
</head>

<body>

        <%@ include file="HTML/menu.html" %>

        <div id="cards-container">

            <%
                Collection<?> products = (Collection<?>) request.getAttribute("products");
            
	            if(products == null) {
	        		System.out.println("NIENTE PRODOTTI");
	        		
	        		// si potrebbe aggiungere error page
	        		response.sendRedirect("./");	
	        		return;
	        	}

                if (products != null && !products.isEmpty()) {

                    Iterator<?> it = products.iterator();
                    
					System.out.println("ciao 1384636");
					
                    while (it.hasNext()) {
                        System.out.println("ciao nel while");
                        ProductBean bean = (ProductBean) it.next();
            %>
                        <div class="card">

                            <div class="card-container-up" onclick="window.location.href='?action=read&id=<%=bean.getID()%>';">

                                <div class="image">
                                    <img src="./getPicture?IDpic=<%=bean.getID()%>" alt="Immagine di: '<%=bean.getDescrizione()%>'">
                                </div>

                                <div class="name">
                                    <h2><%=bean.getNome()%></h2>
                                </div>
                                
                                <div class = "stars">
                                	<svg width="15" height="15" viewBox="0 0 50 50">
									    <polygon class="active" points="25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18" fill="white" stroke="black"/>
									</svg>
									
									<svg width="15" height="15" viewBox="0 0 50 50">
									    <polygon points="25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18" fill="white" stroke="black"/>
									</svg>
									
									<svg width="15" height="15" viewBox="0 0 50 50">
									    <polygon points="25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18" fill="white" stroke="black"/>
									</svg>
									
									<svg width="15" height="15" viewBox="0 0 50 50">
									    <polygon points="25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18" fill="white" stroke="black"/>
									</svg>
									
									<svg width="15" height="15" viewBox="0 0 50 50">
									    <polygon points="25,1 32,18 50,18 36,29 40,46 25,36 10,46 14,29 1,18 18,18" fill="white" stroke="black"/>
									</svg>
                                </div>
                                
                                <div class="grammatura">
                                    <h6><%=bean.getGrammatura()%> ml</h6>
                                </div>
                            </div>

                            <div class="card-container-low">

                                <div class="price">
                                    <h6><%=bean.getPrezzo()%> &euro;</h6>
                                </div>

                                <div class="visualizza">
                                    <a href="/Cart?action=aggiungi&id=<%=bean.getID()%>">ADD TO CART</a>
                                </div>
                            </div>

                        </div>
<%
                    }
                }
%>
        </div>
    </body>
</html>


<%--
<tr>
<td><%=bean.getID()%></td>
<td><%=bean.getNome()%></td>
<td><%=bean.getDescrizione()%></td>
<td><%=bean.getPrezzo()%></td>
<td><%=bean.getGrammatura()%></td>
<td><%=bean.getCategoria()%></td>
<td><%=bean.getQTA_magazzino()%></td>
<td><%=bean.getPercentualeSconto()%></td>
<td><%=bean.getColorazione()%></td>
<td>
<a href="productsView?action=read&ProductID=<%=bean.getID()%>">descrizione</a>

<form action="Cart" method="POST">
<input type="hidden" name="action" value="AddToCart">
<input type="hidden" name="ProductID" value="<%=bean.getID()%>">
<input type="submit" value="Add to cart">
</form>
</td>
</tr>
--%>