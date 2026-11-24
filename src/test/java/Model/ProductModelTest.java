package Model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testutil.JdbcMocks.resultSet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Model.Bean.ProductBean;
import testutil.JdbcMocks;

/**
 * Test 11-19 di pianoTest.md: il DAO dei prodotti.
 *
 * Il database non viene usato: DataSource, Connection, PreparedStatement e ResultSet
 * sono mock di Mockito. Il DataSource viene sostituito nel campo statico del DAO
 * e ripristinato dopo ogni test, così i test restano indipendenti.
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
        // I ResultSet vanno creati prima di when(): creare un mock dentro uno stub non concluso non è permesso.
        ResultSet prodottiDb = resultSet(List.of(rigaProdotto(1, 10, 5), rigaProdotto(2, 20, 5)));
        ResultSet votazioniDb = resultSet(List.of(
                Map.of("PRODOTTO_ID", 2, "MediaValutazione", 4.5, "numValut", 3)));
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps, psVotazioni);
        when(ps.executeQuery()).thenReturn(prodottiDb);
        when(psVotazioni.executeQuery()).thenReturn(votazioniDb);

        List<ProductBean> prodotti = new ArrayList<>(model.doRetrieveAll(null, 10, 5));

        // Guardia: con meno di 2 prodotti, get(1) lancerebbe IndexOutOfBoundsException.
        assertEquals(2, prodotti.size(), "Dovrebbe restituire i 2 prodotti letti");
        assertAll(
                () -> verify(ps).setInt(1, 5),   // limit
                () -> verify(ps).setInt(2, 10),  // offset
                () -> assertEquals(1, prodotti.get(0).getID(), "primo prodotto"),
                () -> assertEquals(2, prodotti.get(1).getID(), "secondo prodotto"),
                () -> assertEquals(0, prodotti.get(0).getNrecensioni(), "Il prodotto 1 non ha recensioni"),
                () -> assertEquals(4.5, prodotti.get(1).getVotazione(), 0.001, "Media voti del prodotto 2"),
                () -> assertEquals(3, prodotti.get(1).getNrecensioni(), "Numero recensioni del prodotto 2"));
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

    // ---------------------------------------------------------------- doDelete

    @Test
    @DisplayName("19 - doDelete senza righe cancellate restituisce false e chiude la connessione")
    void doDelete_nessunaRigaCancellata_restituisceFalseEChiudeConnessione() throws SQLException {
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(0);

        boolean cancellato = model.doDelete(999);

        assertAll(
                () -> assertFalse(cancellato, "Se nessuna riga viene cancellata dovrebbe restituire false"),
                () -> verify(ps).setInt(1, 999),
                () -> verify(conn).close());
    }
}
