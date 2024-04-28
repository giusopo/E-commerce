
<%@ page import="java.util.Collection" %>
<%@ page import="Model.Bean.ProductBean" %>
<%@ page import="java.util.Iterator" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

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

        <div id="cards-container">

            <%
                Collection<?> products = (Collection<?>) request.getAttribute("products");

                if (products != null && !products.isEmpty()) {

                    System.out.println("ciaoo 124535435676578");
                    Iterator<?> it = products.iterator();

                    while (it.hasNext()) {
                        System.out.println("ciao nel while");
                        ProductBean bean = (ProductBean) it.next();
            %>
                        <div class="card">

                            <div class="card-container-up" onclick="window.location.href='?action=read&ProductID=<%=bean.getID()%>';">

                                <div class="image">
<%--                                    <img src="<%=bean.EncodeImg(bean.getImages().get(0).getImg())%>" alt="Immagine di: '<%=bean.getDescrizione()%>'">--%>
                                </div>

                                <div class="name">
                                    <h2><%=bean.getNome()%></h2>
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
                                    <a href="#">ADD TO CART</a>
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