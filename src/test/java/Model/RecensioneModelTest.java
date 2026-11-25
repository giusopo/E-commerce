package Model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
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
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Model.Bean.RecensioneBean;
import testutil.JdbcMocks;
import testutil.JdbcMocks.ChiamataDao;

/**
 * Test 37-38e del piano: recensioni dei prodotti.
 *
 * Ogni test verifica un solo metodo in un solo scenario; i controlli indipendenti sono raccolti in assertAll.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecensioneModel - recensioni dei prodotti")
class RecensioneModelTest {

    private static final int ID_UTENTE = 30;
    private static final int ID_PRODOTTO = 12;
    private static final int ID_RECENSIONE = 55;
    private static final Date DATA_RECENSIONE = Date.valueOf("2026-10-15");

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private PreparedStatement statement;
    @Mock private ResultSet resultSet;

    private RecensioneModel model;
    private DataSource dataSourceOriginale;

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(connection.prepareStatement(anyString())).thenReturn(statement);
        dataSourceOriginale = JdbcMocks.sostituisciDataSource(RecensioneModel.class, dataSource);
        model = new RecensioneModel();
    }

    @AfterEach
    void ripristinaDataSource() {
        JdbcMocks.sostituisciDataSource(RecensioneModel.class, dataSourceOriginale);
    }

    @Test
    @DisplayName("37a - addRecensione passa al DB utente, prodotto, punteggio, commento e data di oggi")
    void addRecensione_parametriCorretti() throws SQLException {
        LocalDate prima = LocalDate.now();
        model.addRecensione(ID_UTENTE, ID_PRODOTTO, 4, "Colore intenso, dura tutto il giorno");
        LocalDate dopo = LocalDate.now();

        // Guardia: serve la data passata al DB per poterla confrontare con quella di oggi.
        ArgumentCaptor<String> data = ArgumentCaptor.forClass(String.class);
        verify(statement).setString(eq(5), data.capture());
        assertAll(
            () -> verify(statement, description("utente")).setInt(1, ID_UTENTE),
            () -> verify(statement, description("prodotto")).setInt(2, ID_PRODOTTO),
            () -> verify(statement, description("punteggio")).setInt(3, 4),
            () -> verify(statement, description("commento")).setString(4, "Colore intenso, dura tutto il giorno"),
            () -> {
                // Intervallo e non uguaglianza: il test resta ripetibile anche se gira a mezzanotte.
                LocalDate dataInserita = LocalDate.parse(data.getValue());
                assertFalse(dataInserita.isBefore(prima) || dataInserita.isAfter(dopo),
                        "La data di inserimento deve essere quella di oggi: " + data.getValue());
            },
            () -> verify(statement, description("L'INSERT deve essere eseguito")).executeUpdate());
    }

    @Test
    @DisplayName("37b - modificaRecensione passa al DB punteggio, commento e id della recensione")
    void modificaRecensione_parametriCorretti() throws SQLException {
        // Il primo parametro di modificaRecensione è l'id della recensione (vedi ProductControl).
        model.modificaRecensione(ID_RECENSIONE, 2, "Dopo un mese si secca");

        assertAll(
            () -> verify(statement, description("punteggio")).setInt(1, 2),
            () -> verify(statement, description("commento")).setString(2, "Dopo un mese si secca"),
            () -> verify(statement, description("id della recensione")).setInt(3, ID_RECENSIONE),
            () -> verify(statement, description("L'UPDATE deve essere eseguito")).executeUpdate());
    }

    @Test
    @DisplayName("38a - getAllRecensioni copia nei bean i dati di ogni recensione del prodotto")
    void getAllRecensioni_righePresenti_recensioniMappate() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        rigaRecensione();

        List<RecensioneBean> lista = model.getAllRecensioni(ID_PRODOTTO);

        // Guardia: senza esattamente una recensione, lista.get(0) non avrebbe senso.
        assertEquals(1, lista.size(), "Attesa una recensione");
        RecensioneBean letta = lista.get(0);
        assertAll(
            () -> verify(statement).setInt(1, ID_PRODOTTO),
            () -> assertRecensioneLetta(letta));
    }

    @Test
    @DisplayName("38b - getAllRecensioni senza recensioni restituisce una lista vuota")
    void getAllRecensioni_nessunaRiga_listaVuota() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        List<RecensioneBean> lista = model.getAllRecensioni(ID_PRODOTTO);

        assertAll(
            () -> verify(statement).setInt(1, ID_PRODOTTO),
            () -> assertTrue(lista.isEmpty(), "Senza righe la lista deve essere vuota"));
    }

    @Test
    @DisplayName("38c - getRecensione copia nel bean la recensione dell'utente sul prodotto")
    void getRecensione_trovata_recensioneMappata() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        rigaRecensione();

        RecensioneBean recensione = model.getRecensione(ID_UTENTE, ID_PRODOTTO);

        // Guardia: i campi si possono controllare solo se il bean esiste.
        assertNotNull(recensione, "getRecensione non deve restituire null");
        assertAll(
            () -> verify(statement).setInt(1, ID_PRODOTTO),
            () -> verify(statement).setInt(2, ID_UTENTE),
            () -> assertRecensioneLetta(recensione));
    }

    @Test
    @DisplayName("38d - getRecensione senza recensione restituisce un bean vuoto")
    void getRecensione_nonTrovata_beanVuoto() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        RecensioneBean recensione = model.getRecensione(ID_UTENTE, ID_PRODOTTO);

        // Guardia: i campi si possono controllare solo se il bean esiste.
        assertNotNull(recensione, "getRecensione non deve restituire null");
        assertAll(
            () -> assertEquals(-1, recensione.getID(), "Senza righe il bean resta quello di default"),
            () -> assertTrue(recensione.isEmpty(), "Una recensione non trovata deve risultare vuota (isEmpty)"));
    }

    // Classe [error] della partizione per categorie: la connessione fallita si prova una volta per metodo.
    static Stream<Arguments> metodi() {
        return Stream.of(
            chiamata("getAllRecensioni", m -> m.getAllRecensioni(ID_PRODOTTO)),
            chiamata("getRecensione", m -> m.getRecensione(ID_UTENTE, ID_PRODOTTO)),
            chiamata("modificaRecensione", m -> m.modificaRecensione(ID_RECENSIONE, 2, "Dopo un mese si secca")),
            chiamata("addRecensione", m -> m.addRecensione(ID_UTENTE, ID_PRODOTTO, 4, "Colore intenso")));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("metodi")
    @DisplayName("38e - se la connessione al database fallisce, ogni metodo propaga la SQLException")
    void metodi_connessioneFallita_lancianoSQLException(ChiamataDao<RecensioneModel> chiamata) throws SQLException {
        doThrow(new SQLException("database non raggiungibile")).when(dataSource).getConnection();

        assertThrows(SQLException.class, () -> chiamata.esegui(model),
                "Dovrebbe propagare la SQLException, non una NullPointerException");
    }

    private static Arguments chiamata(String metodo, ChiamataDao<RecensioneModel> chiamata) {
        return Arguments.of(Named.of(metodo, chiamata));
    }

    /** La riga della tabella recensione restituita dal database simulato. */
    private void rigaRecensione() throws SQLException {
        when(resultSet.getInt("ID")).thenReturn(ID_RECENSIONE);
        when(resultSet.getDate("data_inserimento")).thenReturn(DATA_RECENSIONE);
        when(resultSet.getInt("votazione")).thenReturn(4);
        when(resultSet.getString("commento")).thenReturn("Ottima tenuta");
        when(resultSet.getInt("PRODOTTO_ID")).thenReturn(ID_PRODOTTO);
        when(resultSet.getInt("userID")).thenReturn(ID_UTENTE);
    }

    private static void assertRecensioneLetta(RecensioneBean letta) {
        assertAll(
            () -> assertEquals(ID_RECENSIONE, letta.getID(), "ID"),
            () -> assertEquals(DATA_RECENSIONE, letta.getDataInserimento(), "data di inserimento"),
            () -> assertEquals(4, letta.getVotazione(), "votazione"),
            () -> assertEquals("Ottima tenuta", letta.getCommento(), "commento"),
            () -> assertEquals(ID_PRODOTTO, letta.getPRODOTTO_ID(), "id del prodotto"),
            () -> assertEquals(ID_UTENTE, letta.getUserID(), "id dell'utente"),
            () -> assertFalse(letta.isEmpty(), "Una recensione letta dal database non è vuota"));
    }
}
