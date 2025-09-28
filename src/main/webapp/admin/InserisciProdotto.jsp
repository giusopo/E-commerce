<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Inserimento Prodotto Makeup</title>
    <link href="../style/menu-style.css" rel="stylesheet" type="text/css">
    <link href="../style/Inserimento-prodotti.css" rel="stylesheet" type="text/css">
    <link rel="icon" href="/img/adminLogo.ico" type="image/x-icon">
    <!-- Footer -->
    <link rel="stylesheet" type="text/css" href="../style/footer.css">
</head>
<body>
    <%@ include file="../html/menu.html" %>
    <div class="container">
        <h2>Inserimento Prodotto Makeup</h2>
        <form class="form-inserimento-prodotti" action="<%=request.getContextPath()%>/Admin" method="post" enctype="multipart/form-data" onsubmit="return validateForm()">
            <label for="ID">ID</label>
            <input type="number" id="ID" name="ID" min="0" required>
            
            <label for="nome">Nome</label>
            <input type="text" id="nome" name="nome" required>
            
            <label for="descrizione">Descrizione</label>
            <textarea id="descrizione" name="descrizione" rows="4" required></textarea>
            
            <label for="prezzo">Prezzo</label>
            <input type="number" id="prezzo" name="prezzo" step="0.1" min="0" required>
            
            <label for="grammatura">Grammatura</label>
            <input type="text" id="grammatura" name="grammatura" required>
            
            <label for="categoria">Categoria</label>
            <input type="text" id="categoria" name="categoria" required>
            
            <label for="QTAmagazzino">QTA Magazzino</label>
            <input type="number" id="QTAmagazzino" name="QTAmagazzino" min="0" required>
            
            <label for="percentualeSconto">Percentuale Sconto</label>
            <input type="number" id="percentualeSconto" name="percentualeSconto" step="1" min="0" max="100">
            
            <label for="colorazione">Colorazione</label>
            <input type="text" id="colorazione" name="colorazione" required>

            <label for="immagine1">Immagine Principale [png,jpg]</label>
            <input type="file" id="immagine1" name="immagine1" accept=".png, .jpg, .jpeg" required>

            <label for="immagine2">Immagine Secondaria [png,jpg]</label>
            <input type="file" id="immagine2" name="immagine2" accept=".png, .jpg, .jpeg" required>
            
            <input type="hidden" id="action" name="action" value="inserisciProdotto" required>
            
            <input type="submit" value="Inserisci Prodotto">
        </form>
    </div>
    <%@ include file="../html/footer.html" %>
    <script type="text/javascript" src="../javascript/barraDiRicerca.js"></script>
    <script>
        function validateForm() {
            // You can add custom validation logic here if necessary
            var prezzo = document.getElementById('prezzo').value;
            if (prezzo <= 0) {
                alert("Il prezzo deve essere maggiore di zero.");
                return false;
            }
            var QTA = document.getElementById('QTAmagazzino').value;
            if (QTA < 0) {
                alert("La quantità in magazzino non può essere negativa.");
                return false;
            }
            var percentualeSconto = document.getElementById('percentualeSconto').value;
            if (percentualeSconto < 0 || percentualeSconto > 100) {
                alert("La percentuale di sconto deve essere compresa tra
