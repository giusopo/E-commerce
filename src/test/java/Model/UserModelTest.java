package Model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Model.Bean.UserBean;
import testutil.JdbcMocks;

/** Test 27–33 del piano: registrazione, login e ordini dell'utente. Dati inventati. */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserModel - registrazione, login e ordini dell'utente")
class UserModelTest {

    private static final int ID_UTENTE = 42;
    private static final String USERNAME = "giulia.test";
    private static final String HASH_PASSWORD = "hash-di-prova";

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private PreparedStatement statement;
    @Mock private ResultSet resultSet;

    // Uno statement per ognuno dei tre passi di AddUser.
    @Mock private PreparedStatement utenteStatement;
    @Mock private PreparedStatement idUtenteStatement;
    @Mock private PreparedStatement carrelloStatement;

    private UserModel model;
    private DataSource dataSourceOriginale;

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        dataSourceOriginale = JdbcMocks.sostituisciDataSource(UserModel.class, dataSource);
        model = new UserModel();
    }

    @AfterEach
    void ripristinaDataSource() {
        JdbcMocks.sostituisciDataSource(UserModel.class, dataSourceOriginale);
    }

    @Test
    @DisplayName("27 - AddUser passa i 9 campi dell'utente nell'ordine giusto, con permessi_admin = 0")
    void addUser_utenteValido_parametriInOrdine() throws SQLException {
        preparaAddUser();

        model.AddUser(nuovoUtente());

        verify(utenteStatement).setString(1, "Giulia");
        verify(utenteStatement).setString(2, "Bianchi");
        verify(utenteStatement).setString(3, USERNAME);
        verify(utenteStatement).setString(4, "2000-05-10");
        verify(utenteStatement).setString(5, "giulia@example.com");
        verify(utenteStatement).setString(6, HASH_PASSWORD);
        verify(utenteStatement).setString(7, "3330000000");
        verify(utenteStatement).setString(8, "Via Roma 1, Salerno");
        verify(utenteStatement, description("Un nuovo utente non deve essere amministratore"))
                .setString(9, "0");
        verify(utenteStatement).executeUpdate();
    }

    @Test
    @DisplayName("28 - AddUser crea il carrello associato all'id dell'utente appena inserito")
    void addUser_utenteValido_carrelloCreatoConIdUtente() throws SQLException {
        preparaAddUser();

        model.AddUser(nuovoUtente());

        verify(idUtenteStatement).setString(1, USERNAME);
        verify(idUtenteStatement).setString(2, HASH_PASSWORD);
        verify(carrelloStatement).setInt(1, ID_UTENTE);
        verify(carrelloStatement).executeUpdate();
    }

    @Test
    @DisplayName("29 - AddUser annulla l'inserimento dell'utente se la creazione del carrello fallisce")
    void addUser_creazioneCarrelloFallita_utenteNonSalvato() throws SQLException {
        preparaAddUser();
        when(carrelloStatement.executeUpdate()).thenThrow(new SQLException("errore simulato sul carrello"));

        assertThrows(SQLException.class, () -> model.AddUser(nuovoUtente()),
                "L'errore deve arrivare al chiamante");

        verify(connection, description("L'utente inserito deve essere annullato con rollback()"))
                .rollback();
        verify(connection, never().description("Una registrazione fallita non deve essere confermata con commit()"))
                .commit();
        InOrder ordine = inOrder(connection, utenteStatement);
        ordine.verify(connection, description("La transazione va aperta prima di inserire l'utente"))
                .setAutoCommit(false);
        ordine.verify(utenteStatement).executeUpdate();
    }

    @Test
    @DisplayName("30 - getUserTramiteCredenziali con credenziali corrette restituisce id, cartID e flag admin")
    void getUserTramiteCredenziali_credenzialiCorrette_utentePopolato() throws SQLException {
        preparaQuery();
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt("ID")).thenReturn(ID_UTENTE);
        when(resultSet.getString("USER_NAME")).thenReturn(USERNAME);
        when(resultSet.getInt("cartID")).thenReturn(99);
        when(resultSet.getBoolean("permessi_admin")).thenReturn(true);

        UserBean utente = model.getUserTramiteCredenziali(USERNAME, HASH_PASSWORD);

        verify(statement).setString(1, USERNAME);
        verify(statement).setString(2, HASH_PASSWORD);
        assertAll(
            () -> assertEquals(ID_UTENTE, utente.getID()),
            () -> assertEquals(USERNAME, utente.getUserName()),
            () -> assertEquals(99, utente.getCartID()),
            () -> assertTrue(utente.getPermessiAdmin(), "Il flag admin deve essere copiato dal database"));
    }

    @Test
    @DisplayName("31 - getUserTramiteCredenziali con credenziali errate restituisce un bean vuoto, non null")
    void getUserTramiteCredenziali_credenzialiErrate_beanVuoto() throws SQLException {
        preparaQuery();
        when(resultSet.next()).thenReturn(false);

        UserBean utente = model.getUserTramiteCredenziali(USERNAME, "password-sbagliata");

        assertNotNull(utente, "Il metodo non deve restituire null");
        assertAll(
            () -> assertEquals(0, utente.getID()),
            () -> assertEquals(-1, utente.getCartID()),
            () -> assertEquals("", utente.getUserName()),
            () -> assertFalse(utente.getPermessiAdmin()));
    }

    @ParameterizedTest(quoteTextArguments = false, name = "[{index}] carrello trovato={0} → {1}")
    @CsvSource({"true, 77", "false, -1"})
    @DisplayName("32 - getIdCart e getIdCartByUserId restituiscono l'id del carrello oppure -1")
    void getIdCart_trovatoONonTrovato_idOppureMenoUno(boolean trovato, int idAtteso) throws SQLException {
        preparaQuery();
        when(resultSet.next()).thenReturn(trovato);
        lenient().when(resultSet.getInt("cartID")).thenReturn(77);

        int perCredenziali = model.getIdCart(USERNAME, HASH_PASSWORD);
        int perIdUtente = model.getIdCartByUserId(ID_UTENTE);

        assertEquals(idAtteso, perCredenziali, "getIdCart");
        assertEquals(idAtteso, perIdUtente, "getIdCartByUserId");
        verify(statement).setInt(1, ID_UTENTE);
    }

    static Stream<List<Integer>> ordiniUtente() {
        return Stream.of(List.of(3, 8, 15), List.of());
    }

    @ParameterizedTest(quoteTextArguments = false, name = "[{index}] ordini nel database: {0}")
    @MethodSource("ordiniUtente")
    @DisplayName("33 - getAllOrdine restituisce gli id degli ordini conclusi oppure una lista vuota")
    void getAllOrdine_ordiniPresentiOAssenti_listaIdOppureVuota(List<Integer> ordini) throws SQLException {
        preparaQuery();
        AtomicInteger riga = new AtomicInteger();
        when(resultSet.next()).thenAnswer(invocazione -> riga.incrementAndGet() <= ordini.size());
        lenient().when(resultSet.getInt("id")).thenAnswer(invocazione -> ordini.get(riga.get() - 1));

        List<Integer> risultato = model.getAllOrdine(ID_UTENTE);

        verify(statement).setInt(1, ID_UTENTE);
        assertEquals(ordini, risultato);
    }

    /** Collega ogni query di AddUser al suo statement; l'utente inserito riceve l'id 42. */
    private void preparaAddUser() throws SQLException {
        lenient().when(connection.prepareStatement(startsWith("INSERT INTO utente"))).thenReturn(utenteStatement);
        lenient().when(connection.prepareStatement(startsWith("SELECT id FROM utente"))).thenReturn(idUtenteStatement);
        lenient().when(connection.prepareStatement(startsWith("INSERT INTO ordine"))).thenReturn(carrelloStatement);
        lenient().when(idUtenteStatement.executeQuery()).thenReturn(resultSet);
        lenient().when(resultSet.next()).thenReturn(true);
        lenient().when(resultSet.getInt("id")).thenReturn(ID_UTENTE);
    }

    private void preparaQuery() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
    }

    private static UserBean nuovoUtente() {
        UserBean utente = new UserBean();
        utente.setNome("Giulia");
        utente.setCognome("Bianchi");
        utente.setUserName(USERNAME);
        utente.setDataNascita(Date.valueOf("2000-05-10"));
        utente.setEmail("giulia@example.com");
        utente.setPassword(HASH_PASSWORD);
        utente.setPhoneNumber("3330000000");
        utente.setIndirizzoBase("Via Roma 1, Salerno");
        return utente;
    }
}
