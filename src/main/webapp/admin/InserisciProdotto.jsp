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
        <%-- L'ID del prodotto lo genera il database. Gli stessi controlli sono ripetuti in AdminControl. --%>
        <form class="form-inserimento-prodotti" action="<%=request.getContextPath()%>/Admin" method="post" enctype="multipart/form-data" onsubmit="return validateForm()">
            <label for="nome">Nome</label>
            <input type="text" id="nome" name="nome" maxlength="100" required>

            <label for="descrizione">Descrizione</label>
            <textarea id="descrizione" name="descrizione" rows="4" required></textarea>

            <label for="prezzo">Prezzo</label>
            <input type="number" id="prezzo" name="prezzo" step="0.01" min="0.01" required>

            <label for="grammatura">Grammatura</label>
            <input type="number" id="grammatura" name="grammatura" step="0.1" min="0.1" required>

            <label for="unita">Unità di misura</label>
            <select id="unita" name="unita" required>
                <option value="g">g</option>
                <option value="ml">ml</option>
            </select>

            <label for="categoria">Categoria</label>
            <input type="text" id="categoria" name="categoria" maxlength="100" required>

            <label for="subCategoria">Sottocategoria (facoltativa)</label>
            <input type="text" id="subCategoria" name="subCategoria" maxlength="100">

            <label for="QTAmagazzino">QTA Magazzino</label>
            <input type="number" id="QTAmagazzino" name="QTAmagazzino" step="1" min="0" required>

            <label for="percentualeSconto">Percentuale Sconto</label>
            <input type="number" id="percentualeSconto" name="percentualeSconto" step="1" min="0" max="100" value="0">

            <label for="colorazione">Colorazione</label>
            <input type="text" id="colorazione" name="colorazione" maxlength="20" required>

            <label for="immagine1">Immagine Principale [png,jpg]</label>
            <input type="file" id="immagine1" name="immagine1" accept=".png, .jpg, .jpeg" required>

            <label for="immagine2">Immagine Secondaria [png,jpg]</label>
            <input type="file" id="immagine2" name="immagine2" accept=".png, .jpg, .jpeg" required>

            <input type="hidden" id="action" name="action" value="inserisciProdotto" required>

            <input type="submit" value="Inserisci Prodotto">
        </form>
    </div>
    <%@ include file="../html/footer.html" %>
    <script>
        // Controlli prima dell'invio; il server li ripete (AdminControl.prodottoDalForm).
        function validateForm() {
            var prezzo = Number(document.getElementById('prezzo').value);
            if (!(prezzo > 0)) {
                alert("Il prezzo deve essere maggiore di zero.");
                return false;
            }
            var grammatura = Number(document.getElementById('grammatura').value);
            if (!(grammatura > 0)) {
                alert("La grammatura deve essere maggiore di zero.");
                return false;
            }
            var QTA = Number(document.getElementById('QTAmagazzino').value);
            if (!Number.isInteger(QTA) || QTA < 0) {
                alert("La quantità in magazzino deve essere un numero intero non negativo.");
                return false;
            }
            var campoSconto = document.getElementById('percentualeSconto').value;
            var percentualeSconto = campoSconto === "" ? 0 : Number(campoSconto);
            if (!Number.isInteger(percentualeSconto) || percentualeSconto < 0 || percentualeSconto > 100) {
                alert("La percentuale di sconto deve essere un numero intero compreso tra 0 e 100.");
                return false;
            }
            return true;
        }
    </script>
</body>
</html>
