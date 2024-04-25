<%@ page import="java.util.Collection" %>
<%@ page import="Model.Bean.ProductBean" %>
<%@ page import="java.util.Iterator" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%
    Collection<?> products = (Collection<?>) request.getAttribute("products");
%>

<!DOCTYPE html>
<html>
    <head>
        <title>Beauty Blind</title>

        <style>
            <%@ include file="style/Home/HomeSt.css" %>
            <%@ include file="Navbar/style/menuStyle.css" %>
        </style>

    </head>

    <body>

        <%@ include file="Navbar/menu.html" %>

        <h2>Products</h2>
        <table>
            <tr>
                <th>Codice</th>
                <th>Nome</th>
                <th>Descrizione</th>
                <th>Prezzo</th>
                <th>Grammatura</th>
                <th>Categoria</th>
                <th>QTA in magazzino</th>
                <th>percentuale sconto</th>
                <th>colorazione</th>
                <th>azioni</th>
            </tr>

            <%
                if (products != null && !products.isEmpty()) {
                    Iterator<?> it = products.iterator();
                    while (it.hasNext()) {
                        ProductBean bean = (ProductBean) it.next();
            %>

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
                <td><a href="productsView?action=read&id=<%=bean.getID()%>">descrizione</a></td>
            </tr>

            <%
                }
            } else {
            %>

            <tr>
                <td colspan="6">No products available</td>
            </tr>

            <%
                }
            %>
        </table>

    </body>
</html>