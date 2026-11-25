package Model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Model.Bean.OrdineBean;
import Model.Bean.ProductBean;
import Model.Bean.UserBean;
import testutil.JdbcMocks;
import testutil.JdbcMocks.ChiamataDao;

/**
 * Test 27-33j del piano: registrazione, login e ordini dell'utente. Dati inventati.
 *
 * Ogni test verifica un solo metodo; i controlli indipendenti tra loro sono raccolti in assertAll.
 * Per ogni metodo di lettura c'è la classe "trovato" e la classe "non trovato" (0 righe);
 * la connessione fallita è la classe di errore, provata una volta per metodo.
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

    // Uno statement per ognuna delle due query di getOrdineById.
    @Mock private PreparedStatement ordineStatement;
    @Mock private PreparedStatement prodottiOrdineStatement;

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
    @DisplayName("28b - AddUser riuscito conferma la transazione con commit e ripristina l'autocommit")
    void addUser_utenteValido_transazioneConfermata() throws SQLException {
        preparaAddUser();

        model.AddUser(nuovoUtente());

        InOrder ordine = inOrder(connection, carrelloStatement);
        assertAll(
            () -> verify(connection, never().description("Una registrazione riuscita non va annullata")).rollback(),
            // Il commit arriva dopo la creazione del carrello, poi l'autocommit torna attivo (InOrder non accetta description).
            () -> {
                ordine.verify(connection).setAutoCommit(false);
                ordine.verify(carrelloStatement).executeUpdate();
                ordine.verify(connection).commit();
                ordine.verify(connection).setAutoCommit(true);
            });
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
        when(resultSet.getString("password")).thenReturn(HASH_PASSWORD);
        when(resultSet.getInt("cartID")).thenReturn(99);
        when(resultSet.getString("indirizzoBase")).thenReturn("Via Roma 1, Salerno");
        when(resultSet.getString("telefono")).thenReturn("3330000000");
        when(resultSet.getBoolean("permessi_admin")).thenReturn(true);

        UserBean utente = model.getUserTramiteCredenziali(USERNAME, HASH_PASSWORD);

        // Guardia: i campi si possono controllare solo se il bean esiste.
        assertNotNull(utente, "Il metodo non deve restituire null");
        assertAll(
            () -> verify(statement).setString(1, USERNAME),
            () -> verify(statement).setString(2, HASH_PASSWORD),
            () -> assertEquals(ID_UTENTE, utente.getID(), "ID"),
            () -> assertEquals(USERNAME, utente.getUserName(), "username"),
            // UserControl confronta questa password con l'hash di quella inserita al login.
            () -> assertEquals(HASH_PASSWORD, utente.getPassword(), "password"),
            () -> assertEquals(99, utente.getCartID(), "id del carrello"),
            () -> assertEquals("Via Roma 1, Salerno", utente.getIndirizzoBase(), "indirizzo"),
            () -> assertEquals("3330000000", utente.getPhoneNumber(), "telefono"),
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

    @Test
    @DisplayName("33b - getUserTramiteID copia nel bean i dati dell'utente, con il carrello a -1")
    void getUserTramiteID_trovato_utentePopolato() throws SQLException {
        queryRestituisce(List.of(rigaUtente(ID_UTENTE, USERNAME, true)));

        UserBean utente = model.getUserTramiteID(ID_UTENTE);

        // Guardia: i campi si possono controllare solo se il bean esiste.
        assertNotNull(utente, "Il metodo non deve restituire null");
        assertAll(
            () -> verify(statement).setInt(1, ID_UTENTE),
            () -> assertEquals(ID_UTENTE, utente.getID(), "ID"),
            () -> assertEquals("Giulia", utente.getNome(), "nome"),
            () -> assertEquals("Bianchi", utente.getCognome(), "cognome"),
            () -> assertEquals(USERNAME, utente.getUserName(), "username"),
            () -> assertEquals(HASH_PASSWORD, utente.getPassword(), "password"),
            () -> assertEquals("giulia@example.com", utente.getEmail(), "email"),
            () -> assertEquals("Via Roma 1, Salerno", utente.getIndirizzoBase(), "indirizzo"),
            () -> assertEquals("3330000000", utente.getPhoneNumber(), "telefono"),
            () -> assertEquals(-1, utente.getCartID(), "Il carrello non viene letto: deve restare -1"),
            () -> assertTrue(utente.getPermessiAdmin(), "Il flag admin deve essere copiato dal database"));
    }

    @Test
    @DisplayName("33c - getUserTramiteID con un id inesistente restituisce un bean vuoto, non null")
    void getUserTramiteID_nonTrovato_beanVuoto() throws SQLException {
        queryRestituisce(List.of());

        UserBean utente = model.getUserTramiteID(999);

        // Guardia: i campi si possono controllare solo se il bean esiste.
        assertNotNull(utente, "Il metodo non deve restituire null");
        assertAll(
            () -> assertEquals(0, utente.getID(), "ID"),
            () -> assertEquals("", utente.getUserName(), "username"),
            () -> assertFalse(utente.getPermessiAdmin(), "flag admin"));
    }

    @Test
    @DisplayName("33d - getUtenti restituisce tutti gli utenti con il loro ruolo")
    void getUtenti_piuUtenti_utentiMappati() throws SQLException {
        queryRestituisce(List.of(rigaUtente(1, USERNAME, false), rigaUtente(2, "admin.test", true)));

        List<UserBean> utenti = new ArrayList<>(model.getUtenti());

        // Guardia: utenti.get(0) e utenti.get(1) richiedono due elementi.
        assertEquals(2, utenti.size(), "Attesi due utenti, uno per riga");
        UserBean primo = utenti.get(0);
        assertAll(
            () -> assertEquals(1, primo.getID(), "ID del primo utente"),
            () -> assertEquals("Giulia", primo.getNome(), "nome del primo utente"),
            () -> assertEquals("Bianchi", primo.getCognome(), "cognome del primo utente"),
            () -> assertEquals(USERNAME, primo.getUserName(), "username del primo utente"),
            () -> assertEquals(HASH_PASSWORD, primo.getPassword(), "password del primo utente"),
            () -> assertEquals("giulia@example.com", primo.getEmail(), "email del primo utente"),
            () -> assertEquals("Via Roma 1, Salerno", primo.getIndirizzoBase(), "indirizzo del primo utente"),
            () -> assertEquals("3330000000", primo.getPhoneNumber(), "telefono del primo utente"),
            () -> assertFalse(primo.getPermessiAdmin(), "Il primo utente non è admin"),
            () -> assertEquals(2, utenti.get(1).getID(), "ID del secondo utente"),
            () -> assertEquals("admin.test", utenti.get(1).getUserName(), "username del secondo utente"),
            () -> assertTrue(utenti.get(1).getPermessiAdmin(), "Il secondo utente è admin"),
            () -> assertEquals(-1, utenti.get(1).getCartID(), "Il carrello non viene letto: deve restare -1"));
    }

    @Test
    @DisplayName("33e - getUtenti senza utenti restituisce una lista vuota")
    void getUtenti_nessunUtente_listaVuota() throws SQLException {
        queryRestituisce(List.of());

        assertTrue(model.getUtenti().isEmpty(), "Senza righe la lista deve essere vuota");
    }

    @Test
    @DisplayName("33f - getOrdineById cerca l'ordine tra quelli dell'utente e ne copia dati e prodotti")
    void getOrdineById_ordineDellUtente_ordineEProdottiMappati() throws SQLException {
        preparaOrdine(
            List.of(Map.of("id", 7, "prezzo_tot", 39.0, "percentuale_iva", 20,
                    "data_inoltro", "2026-11-20", "indirizzo_consegna", "Via Roma 1, Salerno")),
            List.of(rigaProdottoOrdine(),
                    Map.of("ID", 2, "nome", "Mascara", "QTAordinata", 2, "prezzo_all_acquisto", 4.5)));

        OrdineBean ordine = model.getOrdineById(ID_UTENTE, 7);

        // Guardia: prodotti.get(0) e prodotti.get(1) richiedono due elementi.
        List<ProductBean> prodotti = ordine.getProdotti();
        assertEquals(2, prodotti.size(), "Attesi i due prodotti dell'ordine");
        ProductBean primo = prodotti.get(0);
        assertAll(
            () -> assertEquals("Rossetto", primo.getNome(), "nome del primo prodotto"),
            () -> assertEquals("Rossetto opaco", primo.getDescrizione(), "descrizione del primo prodotto"),
            () -> assertEquals(12.0, primo.getPrezzo(), 0.0001, "prezzo attuale del primo prodotto"),
            () -> assertEquals(3.5, primo.getGrammatura(), 0.0001, "grammatura del primo prodotto"),
            () -> assertEquals("labbra", primo.getCategoria(), "categoria del primo prodotto"),
            () -> assertEquals(40, primo.getQTA_magazzino(), "quantità in magazzino del primo prodotto"),
            () -> assertEquals(25, primo.getPercentualeSconto(), "percentuale di sconto del primo prodotto"),
            () -> assertEquals("rosso", primo.getColorazione(), "colorazione del primo prodotto"),
            () -> verify(ordineStatement, description("L'ordine va cercato tra quelli dell'utente"))
                    .setInt(1, ID_UTENTE),
            () -> verify(ordineStatement, description("id dell'ordine")).setInt(2, 7),
            () -> verify(prodottiOrdineStatement, description("I prodotti vanno letti dall'ordine trovato"))
                    .setInt(1, 7),
            () -> assertEquals(7, ordine.getIdOrdine(), "id dell'ordine"),
            () -> assertEquals(39.0, ordine.getPrezzoTot(), 0.0001, "totale"),
            () -> assertEquals(20, ordine.getPercentualeIva(), "IVA"),
            () -> assertEquals("2026-11-20", ordine.getDataInoltro(), "data di inoltro"),
            () -> assertEquals("Via Roma 1, Salerno", ordine.getIndirizzoConsegna(), "indirizzo di consegna"),
            () -> assertEquals(1, prodotti.get(0).getID(), "ID del primo prodotto"),
            () -> assertEquals(3, prodotti.get(0).getQTA_ordinata(), "quantità ordinata del primo prodotto"),
            () -> assertEquals(9.0, prodotti.get(0).getPrezzoAllAcquisto(), 0.0001,
                    "Va mostrato il prezzo pagato, non quello attuale"),
            () -> assertEquals(2, prodotti.get(1).getID(), "ID del secondo prodotto"));
    }

    @Test
    @DisplayName("33g - getOrdineById con un ordine non trovato (o di un altro utente) restituisce un ordine vuoto")
    void getOrdineById_ordineNonTrovato_ordineVuoto() throws SQLException {
        preparaOrdine(List.of(), List.of());

        OrdineBean ordine = model.getOrdineById(ID_UTENTE, 7);

        assertAll(
            () -> verify(ordineStatement, description("L'ordine va cercato tra quelli dell'utente"))
                    .setInt(1, ID_UTENTE),
            () -> assertEquals(0, ordine.getIdOrdine(), "Senza righe l'ordine resta quello di default"),
            () -> assertTrue(ordine.getProdotti().isEmpty(), "Un ordine non trovato non ha prodotti"));
    }

    // Classe [error] della partizione per categorie: la connessione fallita si prova una volta per ogni
    // metodo che chiude le risorse a mano. AddUser è escluso: usa try-with-resources e non ha codice di
    // chiusura che possa sbagliare.
    static Stream<Arguments> metodi() {
        return Stream.of(
            chiamata("getUserTramiteCredenziali", m -> m.getUserTramiteCredenziali(USERNAME, HASH_PASSWORD)),
            chiamata("getIdCart", m -> m.getIdCart(USERNAME, HASH_PASSWORD)),
            chiamata("getIdCartByUserId", m -> m.getIdCartByUserId(ID_UTENTE)),
            chiamata("getAllOrdine", m -> m.getAllOrdine(ID_UTENTE)),
            chiamata("getUserTramiteID", m -> m.getUserTramiteID(ID_UTENTE)),
            chiamata("getUtenti", UserModel::getUtenti),
            chiamata("getOrdineById", m -> m.getOrdineById(ID_UTENTE, 7)));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("metodi")
    @DisplayName("33h - se la connessione al database fallisce, i metodi che chiudono le risorse a mano propagano la SQLException")
    void metodi_connessioneFallita_lancianoSQLException(ChiamataDao<UserModel> chiamata) throws SQLException {
        doThrow(new SQLException("database non raggiungibile")).when(dataSource).getConnection();

        assertThrows(SQLException.class, () -> chiamata.esegui(model),
                "Dovrebbe propagare la SQLException, non una NullPointerException");
    }

    // Categoria "chiamata riuscita" per la chiusura delle risorse, sugli stessi sette metodi:
    // getOrdineById esegue due query, quindi apre e chiude due statement e due connessioni.
    static Stream<Arguments> metodiConQuery() {
        return Stream.of(
            chiamataConQuery("getUserTramiteCredenziali", m -> m.getUserTramiteCredenziali(USERNAME, HASH_PASSWORD), 1),
            chiamataConQuery("getIdCart", m -> m.getIdCart(USERNAME, HASH_PASSWORD), 1),
            chiamataConQuery("getIdCartByUserId", m -> m.getIdCartByUserId(ID_UTENTE), 1),
            chiamataConQuery("getAllOrdine", m -> m.getAllOrdine(ID_UTENTE), 1),
            chiamataConQuery("getUserTramiteID", m -> m.getUserTramiteID(ID_UTENTE), 1),
            chiamataConQuery("getUtenti", UserModel::getUtenti, 1),
            chiamataConQuery("getOrdineById", m -> m.getOrdineById(ID_UTENTE, 7), 2));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("metodiConQuery")
    @DisplayName("33i - dopo una chiamata riuscita ogni metodo chiude lo statement e la connessione di ogni query")
    void metodi_chiamataRiuscita_risorseChiuse(ChiamataDao<UserModel> chiamata, int query) throws SQLException {
        queryRestituisce(List.of());

        chiamata.esegui(model);

        assertAll(
            () -> verify(statement, times(query).description("Ogni statement aperto va chiuso")).close(),
            () -> verify(connection, times(query).description("Ogni connessione aperta va chiusa")).close());
    }

    // Categoria "query fallita": in getOrdineById la lettura dei prodotti fallisce dopo che l'ordine è stato
    // trovato. Negli altri metodi il caso non serve: chiudono le risorse in un unico finally, già percorso
    // in caso di errore dal test 33h.
    @Test
    @DisplayName("33j - se in getOrdineById fallisce la lettura dei prodotti, l'errore arriva al chiamante e le risorse vengono chiuse")
    void getOrdineById_letturaProdottiFallita_errorePropagatoERisorseChiuse() throws SQLException {
        SQLException errore = new SQLException("query fallita");
        ResultSet ordineTrovato = JdbcMocks.resultSet(List.of(Map.of("id", 7)));
        when(connection.prepareStatement(startsWith("SELECT ordine.*"))).thenReturn(ordineStatement);
        when(connection.prepareStatement(startsWith("SELECT prodotto.*"))).thenReturn(prodottiOrdineStatement);
        when(ordineStatement.executeQuery()).thenReturn(ordineTrovato);
        when(prodottiOrdineStatement.executeQuery()).thenThrow(errore);

        // Guardia: le verifiche sulla chiusura hanno senso solo se il metodo è fallito con l'errore della query.
        SQLException lanciata = assertThrows(SQLException.class, () -> model.getOrdineById(ID_UTENTE, 7),
                "L'errore della query deve arrivare al chiamante");
        assertAll(
            () -> assertSame(errore, lanciata, "L'errore della query non deve essere sostituito da un altro"),
            () -> verify(ordineStatement, description("Lo statement della lettura dell'ordine va chiuso")).close(),
            () -> verify(prodottiOrdineStatement, description("Lo statement della query fallita va chiuso")).close(),
            () -> verify(connection, times(2).description("Le connessioni di entrambe le query vanno chiuse")).close());
    }

    private static Arguments chiamata(String metodo, ChiamataDao<UserModel> chiamata) {
        return Arguments.of(Named.of(metodo, chiamata));
    }

    private static Arguments chiamataConQuery(String metodo, ChiamataDao<UserModel> chiamata, int query) {
        return Arguments.of(Named.of(metodo, chiamata), query);
    }

    /** Un prodotto di un ordine con tutti i campi letti da getOrdineById. */
    private static Map<String, Object> rigaProdottoOrdine() {
        Map<String, Object> riga = new HashMap<>();
        riga.put("ID", 1);
        riga.put("nome", "Rossetto");
        riga.put("descrizione", "Rossetto opaco");
        riga.put("prezzo", 12.0);
        riga.put("grammatura", 3.5);
        riga.put("categoria", "labbra");
        riga.put("QTAmagazzino", 40);
        riga.put("percentualeSconto", 25);
        riga.put("colorazione", "rosso");
        riga.put("QTAordinata", 3);
        riga.put("prezzo_all_acquisto", 9.0);
        return riga;
    }

    /** Ogni query restituisce le righe indicate. */
    private void queryRestituisce(List<Map<String, Object>> righe) throws SQLException {
        // Il ResultSet va creato prima di when(): creare un mock dentro uno stub non concluso non è permesso.
        ResultSet risultato = JdbcMocks.resultSet(righe);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(risultato);
    }

    /** Collega le due query di getOrdineById ai loro statement e alle righe indicate. */
    private void preparaOrdine(List<Map<String, Object>> righeOrdine, List<Map<String, Object>> righeProdotti)
            throws SQLException {
        ResultSet ordine = JdbcMocks.resultSet(righeOrdine);
        ResultSet prodotti = JdbcMocks.resultSet(righeProdotti);
        when(connection.prepareStatement(startsWith("SELECT ordine.*"))).thenReturn(ordineStatement);
        when(connection.prepareStatement(startsWith("SELECT prodotto.*"))).thenReturn(prodottiOrdineStatement);
        when(ordineStatement.executeQuery()).thenReturn(ordine);
        when(prodottiOrdineStatement.executeQuery()).thenReturn(prodotti);
    }

    /** Una riga della tabella utente. */
    private static Map<String, Object> rigaUtente(int id, String username, boolean admin) {
        Map<String, Object> riga = new HashMap<>();
        riga.put("ID", id);
        riga.put("nome", "Giulia");
        riga.put("cognome", "Bianchi");
        riga.put("USER_NAME", username);
        riga.put("password", HASH_PASSWORD);
        riga.put("email", "giulia@example.com");
        riga.put("indirizzoBase", "Via Roma 1, Salerno");
        riga.put("telefono", "3330000000");
        riga.put("permessi_admin", admin);
        return riga;
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
