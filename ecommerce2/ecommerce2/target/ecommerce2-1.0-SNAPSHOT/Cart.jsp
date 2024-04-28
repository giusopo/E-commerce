
<%@ page import="Model.Bean.*" %>
<%@ page import="java.util.Iterator"%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<html>
    <head>
        <title>Title</title>

        <style>
            /* RESET CSS */
            * {
                margin: 0;
                padding: 0;
                border: 0;
                font-size: 100%;
                font: inherit;
                vertical-align: baseline;

                /* serve a gestire la grandezza di tutti gli elementi senza sfalsarli con il padding */
                box-sizing: border-box;

                font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            }
            <%@ include file="/Navbar/style/menuStyle.css" %>
        </style>
    </head>

    <body>
        <%@ include file="Navbar/menu.html" %>


    </body>
</html>
