<%@ page contentType="text/html;charset=UTF-8" language="java" import="java.util.*,Model.Bean.*"%>
<!DOCTYPE html>
<html>
<head>
	<meta charset="UTF-8">
	<meta name="google" content="notranslate">
    <link rel="icon" href="img/documentLogo.ico" type="image/x-icon">

<title>Fattura Ordine</title>
<style>
    body {
        font-family: Arial, sans-serif;
        margin: 0;
        padding: 0;
        background-color: #f9f9f9;
        font-size: 14px; /* Ridotta dimensione del font */
    }
    .invoice-box {
        max-width: 800px;
        margin: auto;
        padding: 30px;
        border: 1px solid #eee;
        box-shadow: 0 0 10px rgba(0, 0, 0, 0.15);
        background-color: #fff;
    }
    .invoice-box h2 {
        text-align: center;
        margin-bottom: 20px;
    }
    .invoice-box table {
        width: 100%;
        line-height: inherit;
        text-align: left;
    }
    .invoice-box table th, .invoice-box table td {
        padding: 8px;
    }
    .invoice-box table tr.top table td {
        padding-bottom: 20px;
    }
    .invoice-box table tr.information table td {
        padding-bottom: 40px;
    }
    .invoice-box table tr.heading th {
        background: #eee;
        border-bottom: 1px solid #ddd;
        font-weight: bold;
    }
    .invoice-box table tr.details td {
        padding-bottom: 20px;
    }
    .invoice-box table tr.item td {
        border-bottom: 1px solid #eee;
    }
    .invoice-box table tr.item.last td {
        border-bottom: none;
    }
    .invoice-box table tr.total td {
        border-top: 2px solid #eee;
        font-weight: bold;
    }
    .invoice-box table.product-table th, .invoice-box table.product-table td {
        border: 1px solid #ddd; /* Bordi solo per l'ultima tabella */
    }
    .invoice-box table td.right-align {
        text-align: right;
    }
    .price {
        width: 150px; /* Maggiore larghezza per i prezzi */
        word-wrap: break-word; /* Consente il wrap del testo */
    }
</style>
</head>
<body>
    <div class="invoice-box">
        <% OrdineBean b = (OrdineBean) request.getAttribute("ordine"); %>
        <h2>Fattura Ordine</h2>
        <table cellpadding="0" cellspacing="0">
            <tr class="top">
                <td colspan="2">
                    <table>
                        <tr>
                            <td class="title">
                                <h2>BeautyBlind</h2>
                            </td>
                            <td class="right-align">
                                Fattura #: <%= b.getIdOrdine() %><br>
                                Data: <%= b.getDataInoltro() %><br>
                                Percentuale IVA: <%= b.getPercentualeIva() %> %
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
            
            <tr class="information">
                <td colspan="2">
                    <table>
                        <tr>
                            <td>
                                BeautyBlind<br>
                                Università degli studi di Salerno<br>
                                Via Trenta 30
                            </td>
                            <td class="right-align">
                                spedito a:<br>
                                <%= b.getIndirizzoConsegna() %>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
        
        <h2>Dettagli Prodotti</h2>
        <table class="product-table" cellpadding="0" cellspacing="0">
            <tr class="heading">
                <th>
                    Dettagli Prodotto
                </th>
                <th class="right-align">
                    Quantità
                </th>
                <th class="right-align">
                    Prezzo Unità
                </th>
                <th class="right-align">
                    Prezzo Totale
                </th>
            </tr>
            
            <% for(ProductBean product : b.getProdotti()) { %>
            <tr class="item">
                <td>
                    <%= product.getNome() %><br>
                    <small><%= product.getDescrizione() %></small><br>
                </td>
                <td class="right-align">
                    <%= product.getQTA_ordinata() %>
                </td>
                <td class="right-align price">
                    <%= String.format("%.2f", product.getPrezzoAllAcquisto()) %> €
                </td>
                <td class="right-align price">
                    <%= String.format("%.2f", product.getPrezzoAllAcquisto() * product.getQTA_ordinata()) %> €
                </td>
            </tr>
            <% } %>
            
            <tr class="total">
                <td colspan="3"></td>
                <td class="right-align">
                   Totale: <%= b.getPrezzoTot() %> €
                </td>
            </tr>
        </table>
    </div>
</body>
</html>
