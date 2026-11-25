package Model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Model.Bean.ProductBean;
import testutil.JdbcMocks;
import testutil.JdbcMocks.ChiamataDao;

/**
 * Test 20-26b del piano: carrello salvato nel database.
 *
 * Le guardie (tipo di query, numero di righe, valori catturati) sono controllate per prime;
 * gli altri controlli, indipendenti tra loro, sono raccolti in assertAll.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CartModel - carrello salvato nel database")
class CartModelTest {

    private static final int ID_ORDINE = 5;
    private static final int ID_UTENTE = 30;
    private static final int ID_PRODOTTO = 12;
    private static final String INDIRIZZO = "Via Roma 1, Salerno";

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private PreparedStatement statement;

    // Uno statement per ognuno dei tre passi di confermaOrdine.
    @Mock private PreparedStatement prezziStatement;
    @Mock private PreparedStatement ordineStatement;
    @Mock private PreparedStatement carrelloStatement;

    private CartModel model;
    private DataSource dataSourceOriginale;

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        dataSourceOriginale = JdbcMocks.sostituisciDataSource(CartModel.class, dataSource);
        model = new CartModel();
    }

    @AfterEach
    void ripristinaDataSource() {
        JdbcMocks.sostituisciDataSource(CartModel.class, dataSourceOriginale);
    }

    @Test
    @DisplayName("20 - addProduct con prodotto già nel carrello esegue l'UPDATE della quantità con 4 parametri")
    void addProduct_giaPresente_eseguiUpdate() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        model.addProduct(ID_ORDINE, prodotto(ID_PRODOTTO, 9.90, 0, 1), true, 1);

        String sql = sqlPreparato();
        // Guardia: se la query non è l'UPDATE, controllarne i parametri non ha senso.
        assertTrue(sql.startsWith("UPDATE contiene"), "Atteso un UPDATE su contiene, trovato: " + sql);
        assertAll(
            () -> assertTrue(sql.contains("+1"), "L'UPDATE deve aumentare la quantità di 1: " + sql),
            () -> verify(statement).setInt(1, ID_ORDINE),
            () -> verify(statement).setInt(2, ID_PRODOTTO),
            () -> verify(statement).setInt(3, ID_ORDINE),
            () -> verify(statement).setInt(4, ID_PRODOTTO),
            () -> verify(statement, times(4)).setInt(anyInt(), anyInt()),
            () -> verify(statement).executeUpdate());
    }

    @Test
    @DisplayName("21 - addProduct con prodotto nuovo esegue l'INSERT con la quantità indicata")
    void addProduct_nuovo_eseguiInsertConQuantita() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        model.addProduct(ID_ORDINE, prodotto(ID_PRODOTTO, 9.90, 0, 0), false, 3);

        String sql = sqlPreparato();
        // Guardia: se la query non è l'INSERT, controllarne i parametri non ha senso.
        assertTrue(sql.startsWith("INSERT INTO `contiene`"), "Atteso un INSERT su contiene, trovato: " + sql);
        assertAll(
            () -> verify(statement).setInt(1, ID_ORDINE),
            () -> verify(statement).setInt(2, ID_PRODOTTO),
            () -> verify(statement).setInt(3, 3),
            () -> verify(statement, times(3)).setInt(anyInt(), anyInt()),
            () -> verify(statement).executeUpdate());
    }

    @ParameterizedTest(quoteTextArguments = false, name = "[{index}] isUltimo={0} → {1}")
    @CsvSource({
        "true,  DELETE FROM contiene, DELETE FROM, 2",
        "false, UPDATE contiene,      ') - 1',     4"
    })
    @DisplayName("22 - deleteProduct esegue DELETE sull'ultimo pezzo, altrimenti toglie 1 alla quantità")
    void deleteProduct_ultimoONonUltimo_deleteOppureUpdate(boolean isUltimo, String inizioQuery,
            String operazione, int parametri) throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);

        model.deleteProduct(ID_ORDINE, prodotto(ID_PRODOTTO, 9.90, 0, 1), isUltimo);

        String sql = sqlPreparato();
        // Guardia: se il tipo di query è sbagliato, controllarne contenuto e parametri non ha senso.
        assertTrue(sql.startsWith(inizioQuery), "Atteso " + inizioQuery + ", trovato: " + sql);
        assertAll(
            () -> assertTrue(sql.contains(operazione), "La query deve contenere '" + operazione + "': " + sql),
            () -> verify(statement).setInt(1, ID_ORDINE),
            () -> verify(statement).setInt(2, ID_PRODOTTO),
            () -> verify(statement, times(parametri)).setInt(anyInt(), anyInt()),
            () -> verify(statement).executeUpdate());
    }

    @Test
    @DisplayName("23 - getProducts copia nei bean i campi di ogni riga, quantità ordinata compresa")
    void getProducts_righePresenti_prodottiMappati() throws SQLException {
        ResultSet righe = JdbcMocks.resultSet(List.of(
            Map.<String, Object>of("ID", 7, "nome", "Rossetto opaco", "descrizione", "Lunga tenuta", "categoria", "labbra",
                   "prezzo", 12.99, "grammatura", 3.5, "QTAmagazzino", 40, "percentualeSconto", 10,
                   "colorazione", "rosso", "QTAordinata", 2),
            Map.<String, Object>of("ID", 8, "nome", "Mascara volume", "prezzo", 8.50, "QTAordinata", 1)));
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(righe);

        List<ProductBean> prodotti = model.getProducts(ID_UTENTE);

        // Guardia: prodotti.get(0) e prodotti.get(1) richiedono due elementi.
        assertEquals(2, prodotti.size(), "Attesi due prodotti, uno per riga");
        ProductBean primo = prodotti.get(0);
        ProductBean secondo = prodotti.get(1);
        assertAll(
            () -> verify(statement).setInt(1, ID_UTENTE),
            () -> assertEquals(7, primo.getID(), "ID"),
            () -> assertEquals("Rossetto opaco", primo.getNome(), "nome"),
            () -> assertEquals("Lunga tenuta", primo.getDescrizione(), "descrizione"),
            () -> assertEquals("labbra", primo.getCategoria(), "categoria"),
            () -> assertEquals("rosso", primo.getColorazione(), "colorazione"),
            () -> assertEquals(12.99, primo.getPrezzo(), 0.0001, "prezzo"),
            () -> assertEquals(3.5, primo.getGrammatura(), 0.0001, "grammatura"),
            () -> assertEquals(40, primo.getQTA_magazzino(), "quantità in magazzino"),
            () -> assertEquals(10, primo.getPercentualeSconto(), "percentuale di sconto"),
            () -> assertEquals(2, primo.getQTA_ordinata(), "quantità ordinata"),
            () -> assertEquals(8, secondo.getID(), "ID del secondo prodotto"),
            () -> assertEquals(8.50, secondo.getPrezzo(), 0.0001, "prezzo del secondo prodotto"),
            () -> assertEquals(1, secondo.getQTA_ordinata(), "quantità ordinata del secondo prodotto"));
    }

    @Test
    @DisplayName("23b - getProducts con il carrello vuoto restituisce una lista vuota")
    void getProducts_nessunaRiga_listaVuota() throws SQLException {
        ResultSet nessunaRiga = JdbcMocks.resultSet(List.of());
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(nessunaRiga);

        List<ProductBean> prodotti = model.getProducts(ID_UTENTE);

        assertAll(
            () -> verify(statement).setInt(1, ID_UTENTE),
            () -> assertTrue(prodotti.isEmpty(), "Senza righe il carrello deve essere vuoto"));
    }

    @ParameterizedTest(quoteTextArguments = false, name = "[{index}] sconto {0}% → prezzi {1} e {2}, totale {3}")
    @CsvSource({
        "0,   10.00, 4.50, 39.00",
        "50,  5.00,  2.25, 19.50",
        "100, 0.00,  0.00, 0.00"
    })
    @DisplayName("24 - confermaOrdine applica lo sconto al prezzo fatturato e calcola il totale")
    void confermaOrdine_scontiDiversi_totaleEPrezziCorretti(int sconto, double prezzoA, double prezzoB,
            double totale) throws SQLException {
        preparaConfermaOrdine();
        // 3 pezzi da 10,00 € e 2 pezzi da 4,50 €.
        List<ProductBean> carrello = List.of(prodotto(1, 10.00, sconto, 3), prodotto(2, 4.50, sconto, 2));

        model.confermaOrdine(INDIRIZZO, ID_ORDINE, ID_UTENTE, carrello);

        // Guardia: servono i due prezzi catturati per poterli confrontare.
        ArgumentCaptor<Double> prezzi = ArgumentCaptor.forClass(Double.class);
        verify(prezziStatement, times(2)).setDouble(eq(1), prezzi.capture());
        ArgumentCaptor<Double> prezzoTotale = ArgumentCaptor.forClass(Double.class);
        // Guardia: serve il totale catturato per poterlo confrontare.
        verify(ordineStatement).setDouble(eq(1), prezzoTotale.capture());
        assertAll(
            () -> assertEquals(prezzoA, prezzi.getAllValues().get(0), 0.0001, "Prezzo fatturato del primo prodotto"),
            () -> assertEquals(prezzoB, prezzi.getAllValues().get(1), 0.0001, "Prezzo fatturato del secondo prodotto"),
            () -> assertEquals(totale, prezzoTotale.getValue(), 0.0001, "Totale dell'ordine"),
            () -> verify(prezziStatement, times(2)).executeUpdate(),
            () -> verify(ordineStatement).setString(4, INDIRIZZO),
            () -> verify(ordineStatement).setInt(5, ID_ORDINE),
            () -> verify(ordineStatement).executeUpdate());
    }

    @Test
    @DisplayName("25 - confermaOrdine con carrello vuoto salva totale 0 e crea comunque il nuovo carrello")
    void confermaOrdine_carrelloVuoto_totaleZeroENuovoCarrello() throws SQLException {
        preparaConfermaOrdine();

        model.confermaOrdine(INDIRIZZO, ID_ORDINE, ID_UTENTE, List.of());

        assertAll(
            () -> verify(prezziStatement, never().description("Senza prodotti non si scrivono prezzi"))
                    .executeUpdate(),
            () -> verify(ordineStatement, description("Il totale di un carrello vuoto deve essere 0"))
                    .setDouble(1, 0.0),
            () -> verify(ordineStatement, description("L'ordine deve essere confermato")).executeUpdate(),
            () -> verify(carrelloStatement, description("Il nuovo carrello deve appartenere all'utente"))
                    .setInt(1, ID_UTENTE),
            () -> verify(carrelloStatement, description("Il nuovo carrello deve essere creato")).executeUpdate());
    }

    @Test
    @DisplayName("26 - confermaOrdine annulla i prezzi già scritti se la conferma dell'ordine fallisce")
    void confermaOrdine_erroreSecondoPasso_nessunaModificaParziale() throws SQLException {
        preparaConfermaOrdine();
        when(ordineStatement.executeUpdate()).thenThrow(new SQLException("errore simulato sulla conferma"));
        List<ProductBean> carrello = List.of(prodotto(1, 10.00, 0, 3), prodotto(2, 4.50, 0, 2));

        // Guardia: le verifiche sulla transazione hanno senso solo se il metodo è fallito.
        assertThrows(SQLException.class,
                () -> model.confermaOrdine(INDIRIZZO, ID_ORDINE, ID_UTENTE, carrello),
                "L'errore deve arrivare al chiamante");

        InOrder ordine = inOrder(connection, prezziStatement);
        assertAll(
            () -> verify(connection, description("I prezzi già scritti devono essere annullati con rollback()"))
                    .rollback(),
            () -> verify(connection, never().description("Un ordine fallito non deve essere confermato con commit()"))
                    .commit(),
            () -> verify(carrelloStatement, never().description("Dopo l'errore non va creato un nuovo carrello"))
                    .executeUpdate(),
            // La transazione va aperta prima di scrivere i prezzi (InOrder non accetta description).
            () -> {
                ordine.verify(connection).setAutoCommit(false);
                ordine.verify(prezziStatement, times(2)).executeUpdate();
            });
    }

    // Classe [error] della partizione per categorie: la connessione fallita si prova una volta per metodo.
    static Stream<Arguments> metodi() {
        ProductBean prodotto = prodotto(ID_PRODOTTO, 9.90, 0, 1);
        return Stream.of(
            chiamata("addProduct", m -> m.addProduct(ID_ORDINE, prodotto, false, 1)),
            chiamata("deleteProduct", m -> m.deleteProduct(ID_ORDINE, prodotto, true)),
            chiamata("getProducts", m -> m.getProducts(ID_UTENTE)),
            chiamata("confermaOrdine", m -> m.confermaOrdine(INDIRIZZO, ID_ORDINE, ID_UTENTE, List.of(prodotto))));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("metodi")
    @DisplayName("26b - se la connessione al database fallisce, ogni metodo propaga la SQLException")
    void metodi_connessioneFallita_lancianoSQLException(ChiamataDao<CartModel> chiamata) throws SQLException {
        doThrow(new SQLException("database non raggiungibile")).when(dataSource).getConnection();

        assertThrows(SQLException.class, () -> chiamata.esegui(model),
                "Dovrebbe propagare la SQLException, non una NullPointerException");
    }

    private static Arguments chiamata(String metodo, ChiamataDao<CartModel> chiamata) {
        return Arguments.of(Named.of(metodo, chiamata));
    }

    /** Collega ogni query di confermaOrdine al suo statement. */
    private void preparaConfermaOrdine() throws SQLException {
        lenient().when(connection.prepareStatement(startsWith("UPDATE contiene"))).thenReturn(prezziStatement);
        lenient().when(connection.prepareStatement(startsWith("UPDATE ordine"))).thenReturn(ordineStatement);
        lenient().when(connection.prepareStatement(startsWith("INSERT INTO ordine"))).thenReturn(carrelloStatement);
    }

    /** Guardia: verifica che sia stata preparata una sola query e la restituisce. */
    private String sqlPreparato() throws SQLException {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        return sql.getValue();
    }

    private static ProductBean prodotto(int id, double prezzo, int sconto, int quantita) {
        ProductBean bean = new ProductBean();
        bean.setID(id);
        bean.setNome("Prodotto " + id);
        bean.setPrezzo(prezzo);
        bean.setPercentualeSconto(sconto);
        bean.setQTA_ordinata(quantita);
        return bean;
    }
}
