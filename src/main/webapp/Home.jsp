<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.*, java.text.DecimalFormat, Model.Bean.ProductBean" %>

<!DOCTYPE html>
<html>
<head>
    <link rel="icon" href="img/logo.ico" type="image/x-icon">
	<meta name="google" content="notranslate">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <link href="./style/HomeST.css" rel="stylesheet" type="text/css">
    <link href="./style/menu-style.css" rel="stylesheet" type="text/css">
    <link rel="stylesheet" type="text/css" href="./style/footer.css">
    <title>Beauty Blind</title>
</head>
<body>

    <%@ include file="./html/menu.html" %>

    <div id="cards-container">
        <h1><a href="/admin/prova.html"></a></h1>
        <%
            Collection<ProductBean> products = (Collection<ProductBean>) request.getAttribute("products");

            if (products == null || products.isEmpty()) {
        %>
                <p>Nessun prodotto disponibile al momento.</p>
        <%
            } else {
                DecimalFormat df = new DecimalFormat("#.00");
                int i = 0;

                for (ProductBean bean : products) {
        %>
                    <div class="card" id="c<%=i%>">
                    <div class="card-container-up" onclick="window.location.href='?action=read&ProductID=<%=bean.getID()%>';">

                        <div class="new-banner">NEW</div>
                        <div class="BackBanner"></div>

                        <div class="image">
                            <img class="immagine-principale" src="./getPicture?IDpic=<%=bean.getID()%>&ruolo=principale" alt="Immagine di: '<%=bean.getDescrizione()%>'">
                            <img class="immagine-secondaria" src="./getPicture?IDpic=<%=bean.getID()%>&ruolo=secondaria"  onerror="this.src='./img/beauty.png'"  alt="Immagine di: '<%=bean.getDescrizione()%>'">
                        </div>

                        <div class="name">
                            <h2><%=bean.getNome()%></h2>
                        </div>

                        <div class="stars" id="s<%=i%>">
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
                            <span class="Nrecensioni"><strong><a>(<%=bean.getNrecensioni()%> voti)</a></strong></span>
                            <div class="info">
                                <h3><%= String.format("%.1f", bean.getVotazione()) %> su 5 stelle</h3>
                            </div>
                        </div>

                        <div class="grammatura">
                            <h6><%=bean.getGrammatura()%> ml</h6>
                        </div>
                    </div>

                    <div class="card-container-low">
                        <div class="price">
                            <% if(bean.getPercentualeSconto() != 0) { %>
                                <span class="prezzo-scontato">
                                    <%= df.format(bean.getPrezzo() - ((bean.getPrezzo() * bean.getPercentualeSconto()) / 100)) %> &euro;
                                </span>
                                <mark>-<%= bean.getPercentualeSconto() %> %</mark>
                            <% } else { %>
                                <span class="prezzo-normale">
                                    <%= df.format(bean.getPrezzo()) %> &euro;
                                </span>
                            <% } %>
                        </div>

                        <div class="visualizza">
                            <a href="/Cart?action=aggiungi&id=<%=bean.getID()%>">ADD TO CART</a>
                        </div>
                    </div>
                </div>
        <%
                    i++;
                }
            }
        %>
    </div>
    <div class="pagination">
        <%
            Integer currentPage_temp = (Integer) request.getAttribute("currentPage");
        	Integer limit_temp = (Integer) request.getAttribute("limit");
        	int currentPage = 0;
        	int limit = 15;
    		
        	if(currentPage_temp != null || limit_temp != null){
        		currentPage = currentPage_temp.intValue();
        		limit = limit_temp.intValue();
        	}
        	
            if (currentPage > 1) {
        %>
                <a href="?page=<%=currentPage - 1%>&limit=<%=limit+1%>">Previous</a>
        <%
            }

            // Verifica se ci sono ulteriori prodotti per la pagina successiva
            if (products != null && products.size() == limit) {
        %>
                <a href="?page=<%=currentPage + 1%>&limit=<%=limit-1%>">Next</a>
        <%
            }
        %>
    </div>

    <script type="text/javascript" src="./javascript/HomeAnimations.js"></script>

    <%@ include file="html/footer.html" %>     
</body>
</html>
