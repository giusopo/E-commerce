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

/**
 * Test 27-33 del piano: registrazione, login e ordini dell'utente. Dati inventati.
 *
 * Ogni test verifica un solo metodo; i controlli indipendenti tra loro sono raccolti in assertAll.
 */
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

        assertAll(
            () -> verify(utenteStatement, description("nome")).setString(1, "Giulia"),
            () -> verify(utenteStatement, description("cognome")).setString(2, "Bianchi"),
            () -> verify(utenteStatement, description("username")).setString(3, USERNAME),
            () -> verify(utenteStatement, description("data di nascita")).setString(4, "2000-05-10"),
            () -> verify(utenteStatement, description("email")).setString(5, "giulia@example.com"),
            () -> verify(utenteStatement, description("password")).setString(6, HASH_PASSWORD),
            () -> verify(utenteStatement, description("telefono")).setString(7, "3330000000"),
            () -> verify(utenteStatement, description("indirizzo")).setString(8, "Via Roma 1, Salerno"),
            () -> verify(utenteStatement, description("Un nuovo utente non deve essere amministratore"))
                    .setString(9, "0"),
            () -> verify(utenteStatement, description("L'INSERT dell'utente deve essere eseguito"))
                    .executeUpdate());
    }

    @Test
    @DisplayName("28 - AddUser crea il carrello associato all'id dell'utente appena inserito")
    void addUser_utenteValido_carrelloCreatoConIdUtente() throws SQLException {
        preparaAddUser();

        model.AddUser(nuovoUtente());

        assertAll(
            () -> verify(idUtenteStatement, description("L'id va cercato con lo username")).setString(1, USERNAME),
            () -> verify(idUtenteStatement, description("L'id va cercato con la password")).setString(2, HASH_PASSWORD),
            () -> verify(carrelloStatement, description("Il carrello deve avere l'id dell'utente inserito"))
                    .setInt(1, ID_UTENTE),
            () -> verify(carrelloStatement, description("Il carrello deve essere creato")).executeUpdate());
    }

    @Test
    @DisplayName("29 - AddUser annulla l'inserimento dell'utente se la creazione del carrello fallisce")
    void addUser_creazioneCarrelloFallita_utenteNonSalvato() throws SQLException {
        preparaAddUser();
        when(carrelloStatement.executeUpdate()).thenThrow(new SQLException("errore simulato sul carrello"));

        // Guardia: le verifiche sulla transazione hanno senso solo se il metodo è fallito.
        assertThrows(SQLException.class, () -> model.AddUser(nuovoUtente()),
                "L'errore deve arrivare al chiamante");

        InOrder ordine = inOrder(connection, utenteStatement);
        assertAll(
            () -> verify(connection, description("L'utente inserito deve essere annullato con rollback()"))
                    .rollback(),
            () -> verify(connection, never().description("Una registrazione fallita non deve essere confermata con commit()"))
                    .commit(),
            // La transazione va aperta prima di inserire l'utente (InOrder non accetta description).
            () -> {
                ordine.verify(connection).setAutoCommit(false);
                ordine.verify(utenteStatement).executeUpdate();
            });
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

        // Guardia: i campi si possono controllare solo se il bean esiste.
        assertNotNull(utente, "Il metodo non deve restituire null");
        assertAll(
            () -> verify(statement).setString(1, USERNAME),
            () -> verify(statement).setString(2, HASH_PASSWORD),
            () -> assertEquals(ID_UTENTE, utente.getID(), "ID"),
            () -> assertEquals(USERNAME, utente.getUserName(), "username"),
            () -> assertEquals(99, utente.getCartID(), "id del carrello"),
            () -> assertTrue(utente.getPermessiAdmin(), "Il flag admin deve essere copiato dal database"));
    }

    @Test
    @DisplayName("31 - getUserTramiteCredenziali con credenziali errate restituisce un bean vuoto, non null")
    void getUserTramiteCredenziali_credenzialiErrate_beanVuoto() throws SQLException {
        preparaQuery();
        when(resultSet.next()).thenReturn(false);

        UserBean utente = model.getUserTramiteCredenziali(USERNAME, "password-sbagliata");

        // Guardia: i campi si possono controllare solo se il bean esiste.
        assertNotNull(utente, "Il metodo non deve restituire null");
        assertAll(
            () -> assertEquals(0, utente.getID(), "ID"),
            () -> assertEquals(-1, utente.getCartID(), "id del carrello"),
            () -> assertEquals("", utente.getUserName(), "username"),
            () -> assertFalse(utente.getPermessiAdmin(), "flag admin"));
    }

    @ParameterizedTest(quoteTextArguments = false, name = "[{index}] carrello trovato={0} → {1}")
    @CsvSource({"true, 77", "false, -1"})
    @DisplayName("32a - getIdCart restituisce l'id del carrello in corso dell'utente con quelle credenziali, oppure -1")
    void getIdCart_trovatoONonTrovato_idOppureMenoUno(boolean trovato, int idAtteso) throws SQLException {
        preparaQuery();
        when(resultSet.next()).thenReturn(trovato);
        lenient().when(resultSet.getInt("cartID")).thenReturn(77);

        int idCarrello = model.getIdCart(USERNAME, HASH_PASSWORD);

        assertAll(
            () -> verify(statement).setString(1, USERNAME),
            () -> verify(statement).setString(2, HASH_PASSWORD),
            () -> assertEquals(idAtteso, idCarrello, "id del carrello"));
    }

    @ParameterizedTest(quoteTextArguments = false, name = "[{index}] carrello trovato={0} → {1}")
    @CsvSource({"true, 77", "false, -1"})
    @DisplayName("32b - getIdCartByUserId restituisce l'id del carrello in corso dell'utente, oppure -1")
    void getIdCartByUserId_trovatoONonTrovato_idOppureMenoUno(boolean trovato, int idAtteso) throws SQLException {
        preparaQuery();
        when(resultSet.next()).thenReturn(trovato);
        lenient().when(resultSet.getInt("cartID")).thenReturn(77);

        int idCarrello = model.getIdCartByUserId(ID_UTENTE);

        assertAll(
            () -> verify(statement).setInt(1, ID_UTENTE),
            () -> assertEquals(idAtteso, idCarrello, "id del carrello"));
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

        assertAll(
            () -> verify(statement).setInt(1, ID_UTENTE),
            () -> assertEquals(ordini, risultato, "id degli ordini"));
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
