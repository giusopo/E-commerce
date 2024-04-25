<%--
  Created by IntelliJ IDEA.
  User: utente
  Date: 19/04/2024
  Time: 15:26
  To change this template use File | Settings | File Templates.
--%>

<%@ page import="Model.Bean.ProductBean" %>
<%@ page import="java.util.Iterator" %>
<%@ page import="Model.Bean.ImgBean" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>


<%
    ProductBean product = (ProductBean) request.getAttribute("product");
    ImgBean img = null;

    // in caso vero bisogna gestire l'eccezione
    if (product == null || product.getImages().isEmpty()) System.out.println("product di default sarà stampato");
%>

<html>
    <head>
        <title><%=product.getNome()%></title>

        <style>

            img {
                width: 200px;
            }

            td {
                padding: 10px;
            }

        </style>
    </head>

    <body>
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
                    <img src="<%=product.getBase64Image(img.getImg())%>" alt="Product Image">
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

            <%} else {%>

            <tr>
                <td colspan="6">Errore nel cercare il prodotto</td>
            </tr>

            <%}%>
        </table>
    </body>
</html>
