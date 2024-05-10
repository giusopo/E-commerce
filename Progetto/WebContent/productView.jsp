
<%@ page import="Model.Bean.*" %>
<%@ page import="java.util.Iterator" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>



<%
    ProductBean product = (ProductBean) request.getAttribute("product");
    ImgBean img = null;

    // in caso vero bisogna gestire l'eccezione
    if (product == null) System.out.println("product di default sarà stampato");
    
    else if (product.getImages().isEmpty()) System.out.println("Niente immagine, product di default sarà stampato");
%>

<html>
    <head>
    
        <title><%=product.getNome()%></title>
        
		<link href="./style/menu-style.css" rel="stylesheet" type="text/css">
		
        <style>
        	img{
        		height: 250px;
        	}
        </style>
    </head>

    <body>
    
    	<%@ include file="HTML/menu.html" %>
    
        <table>
            <tr>
                <th>Immagine</th>
                <th>Codice</th>
                <th>Nome</th>
                <th>Descrizione</th>
                <th>Prezzo</th>
                <th>Grammatura</th>
                <th>Categoria</th>
                <th>QTA in magazzino</th>
                <th>percentuale sconto</th>
                <th>colorazione</th>
            </tr>

            <%
                if (product != null) {
            %>

            <tr>
                <td>

                    <%
                        Iterator<?> it = product.getImages().iterator();

                        while (it.hasNext()) {
                            img = (ImgBean) it.next();
                    %>
                    <!--gestire eccezione del metodo nel tag img-->
                    <img src="./getPicture?IDpic=<%=product.getID()%>" alt="Product Image">
          <% } %>

                </td>

                <td><%=product.getID()%></td>
                <td><%=product.getNome()%></td>
                <td><%=product.getDescrizione()%></td>
                <td><%=product.getPrezzo()%></td>
                <td><%=product.getGrammatura()%></td>
                <td><%=product.getCategoria()%></td>
                <td><%=product.getQTA_magazzino()%></td>
                <td><%=product.getPercentualeSconto()%></td>
                <td><%=product.getColorazione()%></td>
            </tr>
            <%}%>
            
        </table>
    </body>
</html>
