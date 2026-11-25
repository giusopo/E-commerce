package Model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testutil.JdbcMocks.resultSet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Model.Bean.ProductBean;
import testutil.JdbcMocks;
import testutil.JdbcMocks.ChiamataDao;

/**
 * Test 11-19 di pianoTest.md, con i sotto-numeri: il DAO dei prodotti.
 *
 * Il database non viene usato: DataSource, Connection, PreparedStatement e ResultSet
 * sono mock di Mockito. Il DataSource viene sostituito nel campo statico del DAO
 * e ripristinato dopo ogni test, così i test restano indipendenti.
 *
 * Classi di equivalenza: numero di righe lette (0, 1, molte), categoria ("all" oppure una categoria,
 * anche malevola), ordinamento (null, vuoto, indicato), righe cancellate (0 o 1, valori limite)
 * e connessione al database fallita, provata una volta per ogni metodo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductModel - DAO prodotti (database simulato con Mockito)")
class ProductModelTest {

    @Mock
    private DataSource ds;
    @Mock
    private Connection conn;
    @Mock
    private PreparedStatement ps;
    @Mock
    private PreparedStatement psVotazioni;

    private DataSource dsOriginale;
    private ProductModel model;

    @BeforeEach
    void init() {
        dsOriginale = JdbcMocks.sostituisciDataSource(ProductModel.class, ds);
        model = new ProductModel();
    }

    @AfterEach
    void ripristinaDataSource() {
        JdbcMocks.sostituisciDataSource(ProductModel.class, dsOriginale);
    }

    /** Il database simulato risponde a ogni query con il ResultSet indicato. */
    private void databaseRisponde(ResultSet rs) throws SQLException {
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
    }

    /** Una riga della tabella prodotto. */
    private static Map<String, Object> rigaProdotto(int id, Object prezzo, Object grammatura) {
        Map<String, Object> riga = new HashMap<>();
        riga.put("ID", id);
        riga.put("nome", "Rossetto " + id);
        riga.put("descrizione", "Rossetto opaco");
        riga.put("prezzo", prezzo);
        riga.put("grammatura", grammatura);
        riga.put("categoria", "labbra");
        riga.put("QTAmagazzino", 20);
        riga.put("percentualeSconto", 10);
        riga.put("colorazione", "rosso");
        return riga;
    }

    /**
     * Il database simulato restituisce i prodotti 1 e 2 e, nella seconda query, la media voti del solo
     * prodotto 2 (4,5 su 3 recensioni). Il prodotto 2 ha prezzo e grammatura decimali (come nel test 13).
     */
    private void databaseRispondeConDueProdottiEUnaVotazione() throws SQLException {
        // I ResultSet vanno creati prima di when(): creare un mock dentro uno stub non concluso non è permesso.
        ResultSet prodottiDb = resultSet(List.of(rigaProdotto(1, 10, 5), rigaProdotto(2, 19.99, 7.5)));
        ResultSet votazioniDb = resultSet(List.of(
                Map.of("PRODOTTO_ID", 2, "MediaValutazione", 4.5, "numValut", 3)));
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps, psVotazioni);
        when(ps.executeQuery()).thenReturn(prodottiDb);
        when(psVotazioni.executeQuery()).thenReturn(votazioniDb);
    }

    /** I prodotti letti da databaseRispondeConDueProdottiEUnaVotazione, con le loro votazioni. */
    private static void assertDueProdottiConVotazioni(Collection<ProductBean> letti) {
        List<ProductBean> prodotti = new ArrayList<>(letti);
        // Guardia: con meno di 2 prodotti, get(1) lancerebbe IndexOutOfBoundsException.
        assertEquals(2, prodotti.size(), "Dovrebbe restituire i 2 prodotti letti");
        assertAll(
                () -> assertEquals(1, prodotti.get(0).getID(), "primo prodotto"),
                () -> assertEquals(2, prodotti.get(1).getID(), "secondo prodotto"),
                () -> assertEquals("Rossetto 2", prodotti.get(1).getNome(), "nome del secondo prodotto"),
                () -> assertEquals(19.99, prodotti.get(1).getPrezzo(), 0.001, "Il prezzo 19,99 non dovrebbe perdere i decimali"),
                () -> assertEquals(7.5, prodotti.get(1).getGrammatura(), 0.001, "La grammatura 7,5 non dovrebbe perdere i decimali"),
                () -> assertEquals(0, prodotti.get(0).getNrecensioni(), "Il prodotto 1 non ha recensioni"),
                () -> assertEquals(4.5, prodotti.get(1).getVotazione(), 0.001, "Media voti del prodotto 2"),
                () -> assertEquals(3, prodotti.get(1).getNrecensioni(), "Numero recensioni del prodotto 2"));
    }

    /** Guardia: le query preparate, nell'ordine (prima i prodotti, poi le votazioni). */
    private List<String> queryPreparate() throws SQLException {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conn, atLeastOnce()).prepareStatement(sql.capture());
        return sql.getAllValues();
    }

    private static Arguments chiamata(String metodo, ChiamataDao<ProductModel> chiamata) {
        return Arguments.of(Named.of(metodo, chiamata));
    }

    // ---------------------------------------------------------------- doRetrieveByKey

    @Test
    @DisplayName("11 - doRetrieveByKey con id esistente: bean popolato")
    void doRetrieveByKey_idEsistente_beanPopolato() throws SQLException {
        databaseRisponde(resultSet(List.of(rigaProdotto(7, 15, 3))));

        ProductBean p = model.doRetrieveByKey(7);

        verify(ps).setInt(1, 7);
        assertAll(
                () -> assertEquals(7, p.getID(), "ID"),
                () -> assertEquals("Rossetto 7", p.getNome(), "nome"),
                () -> assertEquals("Rossetto opaco", p.getDescrizione(), "descrizione"),
                () -> assertEquals(15.0, p.getPrezzo(), 0.001, "prezzo"),
                () -> assertEquals("labbra", p.getCategoria(), "categoria"),
                () -> assertEquals(20, p.getQTA_magazzino(), "quantità in magazzino"),
                () -> assertEquals(10, p.getPercentualeSconto(), "percentuale di sconto"),
                () -> assertEquals("rosso", p.getColorazione(), "colorazione"));
    }

    @Test
    @DisplayName("12 - doRetrieveByKey con id inesistente: bean vuoto")
    void doRetrieveByKey_idInesistente_beanVuoto() throws SQLException {
        databaseRisponde(resultSet(List.of()));

        ProductBean p = model.doRetrieveByKey(999);

        assertTrue(p.isEmpty(), "Senza righe dovrebbe restituire un bean vuoto");
    }

    @Test
    @DisplayName("13 - doRetrieveByKey non tronca prezzo e grammatura decimali")
    void doRetrieveByKey_prezzoDecimale_prezzoNonTroncato() throws SQLException {
        databaseRisponde(resultSet(List.of(rigaProdotto(7, 12.99, 7.5))));

        ProductBean p = model.doRetrieveByKey(7);

        assertAll(
                () -> assertEquals(12.99, p.getPrezzo(), 0.001, "Il prezzo 12,99 non dovrebbe perdere i decimali"),
                () -> assertEquals(7.5, p.getGrammatura(), 0.001, "La grammatura 7,5 non dovrebbe perdere i decimali"));
    }

    @Test
    @DisplayName("14 - doRetrieveByKey chiude le risorse anche in caso di errore")
    void doRetrieveByKey_sqlException_risorseChiuse() throws SQLException {
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenThrow(new SQLException("errore simulato"));

        assertThrows(SQLException.class, () -> model.doRetrieveByKey(1),
                "L'errore del database dovrebbe essere propagato");
        assertAll(
                () -> verify(ps).close(),
                () -> verify(conn).close());
    }

    // ---------------------------------------------------------------- doRetrieveAll

    @Test
    @DisplayName("15 - doRetrieveAll usa limit e offset e restituisce tutti i prodotti con le votazioni")
    void doRetrieveAll_limitOffset_parametriImpostatiERisultati() throws SQLException {
        databaseRispondeConDueProdottiEUnaVotazione();

        // "ID" è l'ordinamento che usano davvero ProductControl e StarControl.
        Collection<ProductBean> prodotti = model.doRetrieveAll("ID", 10, 5);

        assertAll(
                () -> verify(ps).setInt(1, 5),   // limit
                () -> verify(ps).setInt(2, 10),  // offset
                () -> assertDueProdottiConVotazioni(prodotti));
    }

    // Categoria per il parametro order: null, stringa vuota (valore speciale) e un ordinamento indicato.
    static Stream<Arguments> ordinamenti() {
        return Stream.of(
                Arguments.of(null, "'principale' LIMIT ? OFFSET ?"),
                Arguments.of("", "'principale' LIMIT ? OFFSET ?"),
                Arguments.of("ID", "'principale' ORDER BY ID LIMIT ? OFFSET ?"));
    }

    @ParameterizedTest(name = "[{index}] order = \"{0}\"")
    @MethodSource("ordinamenti")
    @DisplayName("15b - doRetrieveAll aggiunge ORDER BY solo se order è indicato, non se è null o vuoto")
    void doRetrieveAll_orderNulloVuotoOIndicato_orderBySoloSeIndicato(String order, String fineQuery)
            throws SQLException {
        databaseRispondeConDueProdottiEUnaVotazione();

        model.doRetrieveAll(order, 0, 10);

        String queryProdotti = queryPreparate().get(0);
        assertTrue(queryProdotti.endsWith(fineQuery),
                "La query dei prodotti dovrebbe finire con \"" + fineQuery + "\": " + queryProdotti);
    }

    // ---------------------------------------------------------------- doRetrieveByInput

    static Stream<Arguments> risultatiRicerca() {
        return Stream.of(
                Arguments.of(0, "[]"),
                Arguments.of(1, "[{\"Id\": 1, \"nome\": \"Prodotto 1\"}]"),
                Arguments.of(3, "[{\"Id\": 1, \"nome\": \"Prodotto 1\"}, "
                        + "{\"Id\": 2, \"nome\": \"Prodotto 2\"}, "
                        + "{\"Id\": 3, \"nome\": \"Prodotto 3\"}]"));
    }

    @ParameterizedTest(name = "[{index}] {0} risultati")
    @MethodSource("risultatiRicerca")
    @DisplayName("16 - doRetrieveByInput costruisce un JSON valido")
    void doRetrieveByInput_zeroUnoOPiuRisultati_jsonCorretto(int numeroRisultati, String jsonAtteso)
            throws SQLException {
        List<Map<String, Object>> righe = IntStream.rangeClosed(1, numeroRisultati)
                .mapToObj(i -> Map.<String, Object>of("ID", i, "nome", "Prodotto " + i))
                .collect(Collectors.toList());
        databaseRisponde(resultSet(righe));

        String json = model.doRetrieveByInput("ross").toString();

        verify(ps).setString(1, "%ross%");
        assertEquals(jsonAtteso, json, "JSON non corretto con " + numeroRisultati + " risultati");
    }

    @Test
    @DisplayName("17 - doRetrieveByInput propaga la SQLException se la connessione fallisce")
    void doRetrieveByInput_connessioneFallita_lanciaSQLException() throws SQLException {
        when(ds.getConnection()).thenThrow(new SQLException("database non raggiungibile"));

        assertThrows(SQLException.class, () -> model.doRetrieveByInput("ross"),
                "Dovrebbe propagare la SQLException, non una NullPointerException");
    }

    // Classe [error] della partizione per categorie: la connessione fallita si prova una volta per metodo.
    static Stream<Arguments> altriMetodi() {
        return Stream.of(
                chiamata("doRetrieveByKey", m -> m.doRetrieveByKey(1)),
                chiamata("doRetrieveAll", m -> m.doRetrieveAll("ID", 0, 10)),
                chiamata("doRetrieveByCat", m -> m.doRetrieveByCat("all", "ID")),
                chiamata("doRetrieveBySubCat", m -> m.doRetrieveBySubCat("all", "rossetti", "ID")),
                chiamata("doDelete", m -> m.doDelete(1)));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("altriMetodi")
    @DisplayName("17b - anche gli altri metodi propagano la SQLException se la connessione fallisce")
    void altriMetodi_connessioneFallita_lancianoSQLException(ChiamataDao<ProductModel> chiamata) throws SQLException {
        when(ds.getConnection()).thenThrow(new SQLException("database non raggiungibile"));

        assertThrows(SQLException.class, () -> chiamata.esegui(model),
                "Dovrebbe propagare la SQLException, non una NullPointerException");
    }

    // ---------------------------------------------------------------- doRetrieveByCat

    @Test
    @DisplayName("18 - doRetrieveByCat non è vulnerabile a SQL injection")
    void doRetrieveByCat_categoriaMalevola_nessunaSqlInjection() throws SQLException {
        String categoriaMalevola = "x' OR '1'='1";
        databaseRisponde(resultSet(List.of()));

        model.doRetrieveByCat(categoriaMalevola, null);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(conn, atLeastOnce()).prepareStatement(sql.capture());
        assertTrue(sql.getAllValues().stream().noneMatch(query -> query.contains(categoriaMalevola)),
                "La categoria non deve essere concatenata nella query SQL: " + sql.getAllValues().get(0));
        verify(ps).setString(1, categoriaMalevola); // il filtro resta, ma come parametro
    }

    @Test
    @DisplayName("18b - doRetrieveByCat con categoria \"all\" restituisce tutti i prodotti con le votazioni, senza filtro")
    void doRetrieveByCat_categoriaAll_tuttiIProdottiConVotazioni() throws SQLException {
        databaseRispondeConDueProdottiEUnaVotazione();

        // È la chiamata usata da ProductControl e AdminControl per mostrare l'intero catalogo.
        Collection<ProductBean> prodotti = model.doRetrieveByCat("all", "ID");

        String queryProdotti = queryPreparate().get(0);
        assertAll(
                () -> assertFalse(queryProdotti.contains("categoria = ?"),
                        "Con \"all\" la query non deve filtrare per categoria: " + queryProdotti),
                () -> verify(ps, never().description("Con \"all\" non ci sono parametri da impostare"))
                        .setString(anyInt(), anyString()),
                () -> assertTrue(queryProdotti.endsWith("ORDER BY ID"),
                        "I prodotti vanno ordinati per ID: " + queryProdotti),
                () -> assertDueProdottiConVotazioni(prodotti));
    }

    // ---------------------------------------------------------------- doRetrieveBySubCat

    @Test
    @DisplayName("18c - doRetrieveBySubCat non è vulnerabile a SQL injection")
    void doRetrieveBySubCat_categoriaESottocategoriaMalevole_nessunaSqlInjection() throws SQLException {
        String categoriaMalevola = "x' OR '1'='1";
        String sottocategoriaMalevola = "y' OR '1'='1";
        databaseRisponde(resultSet(List.of()));

        model.doRetrieveBySubCat(categoriaMalevola, sottocategoriaMalevola, null);

        List<String> query = queryPreparate();
        assertAll(
                () -> assertTrue(query.stream().noneMatch(
                                q -> q.contains(categoriaMalevola) || q.contains(sottocategoriaMalevola)),
                        "Categoria e sottocategoria non devono essere concatenate nella query SQL: " + query),
                () -> verify(ps).setString(1, categoriaMalevola),       // il filtro resta, ma come parametro
                () -> verify(ps).setString(2, sottocategoriaMalevola));
    }

    @Test
    @DisplayName("18d - doRetrieveBySubCat con categoria \"all\" restituisce tutti i prodotti con le votazioni, senza filtro")
    void doRetrieveBySubCat_categoriaAll_tuttiIProdottiConVotazioni() throws SQLException {
        databaseRispondeConDueProdottiEUnaVotazione();

        Collection<ProductBean> prodotti = model.doRetrieveBySubCat("all", "rossetti", "ID");

        String queryProdotti = queryPreparate().get(0);
        assertAll(
                () -> assertFalse(queryProdotti.contains("subCategoria = ?"),
                        "Con \"all\" la query non deve filtrare per sottocategoria: " + queryProdotti),
                () -> verify(ps, never().description("Con \"all\" non ci sono parametri da impostare"))
                        .setString(anyInt(), anyString()),
                () -> assertTrue(queryProdotti.endsWith("ORDER BY ID"),
                        "I prodotti vanno ordinati per ID: " + queryProdotti),
                () -> assertDueProdottiConVotazioni(prodotti));
    }

    // ---------------------------------------------------------------- doDelete

    // 0 e 1 sono i valori limite tra "nessuna riga cancellata" (false) e "almeno una" (true).
    @ParameterizedTest(quoteTextArguments = false, name = "[{index}] righe cancellate={0} → {1}")
    @CsvSource({"0, false", "1, true"})
    @DisplayName("19 - doDelete restituisce true solo se cancella almeno una riga, e chiude la connessione")
    void doDelete_zeroOUnaRigaCancellata_trueSoloSeCancellaEChiudeConnessione(int righeCancellate, boolean atteso)
            throws SQLException {
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(righeCancellate);

        boolean cancellato = model.doDelete(999);

        assertAll(
                () -> assertEquals(atteso, cancellato, "Esito con " + righeCancellate + " righe cancellate"),
                () -> verify(ps).setInt(1, 999),
                () -> verify(conn).close());
    }
}
