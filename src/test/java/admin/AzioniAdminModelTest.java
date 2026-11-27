package admin;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Model.Bean.ProductBean;
import testutil.JdbcMocks;

/**
 * Test 34-36 del piano: azioni dell'amministratore sul catalogo.
 *
 * Ogni test verifica un solo metodo e un solo aspetto; i controlli indipendenti sono raccolti in assertAll.
 * Non c'è un test sulla connessione fallita: entrambi i metodi usano try-with-resources, quindi
 * l'eccezione arriva al chiamante senza codice di chiusura che potrebbe sbagliare.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AzioniAdminModel - azioni dell'amministratore sul catalogo")
class AzioniAdminModelTest {

    private static final int ID_PRODOTTO = 50;
    /** Id che il database assegna al prodotto inserito. */
    private static final int ID_GENERATO = 102;
    private static final Date DATA_INSERIMENTO = Date.valueOf("2026-11-01");

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private PreparedStatement statement;
    /** Chiavi generate dall'INSERT del prodotto. */
    @Mock private ResultSet chiavi;

    private AzioniAdminModel model;
    private DataSource dataSourceOriginale;

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        // modificaSconto usa prepareStatement(sql), inserisciProdotto l'overload con le chiavi generate
        lenient().when(connection.prepareStatement(anyString())).thenReturn(statement);
        lenient().when(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(statement);
        lenient().when(statement.executeUpdate()).thenReturn(1);
        lenient().when(statement.getGeneratedKeys()).thenReturn(chiavi);
        lenient().when(chiavi.next()).thenReturn(true);
        lenient().when(chiavi.getInt(1)).thenReturn(ID_GENERATO);
        dataSourceOriginale = JdbcMocks.sostituisciDataSource(AzioniAdminModel.class, dataSource);
        model = new AzioniAdminModel();
    }

    @AfterEach
    void ripristinaDataSource() {
        JdbcMocks.sostituisciDataSource(AzioniAdminModel.class, dataSourceOriginale);
    }

    @Test
    @DisplayName("34a - inserisciProdotto passa al DB gli 11 campi del prodotto, senza l'id")
    void inserisciProdotto_prodottoValido_campiImpostati() throws SQLException {
        model.inserisciProdotto(nuovoProdotto());

        assertAll(
            () -> verify(statement, description("nome")).setString(1, "Fondotinta fluido"),
            () -> verify(statement, description("descrizione")).setString(2, "Copertura media, finish naturale"),
            () -> verify(statement, description("prezzo")).setDouble(3, 19.90),
            () -> verify(statement, description("grammatura")).setDouble(4, 30.0),
            () -> verify(statement, description("unità")).setString(5, "ml"),
            () -> verify(statement, description("categoria")).setString(6, "viso"),
            () -> verify(statement, description("sottocategoria")).setString(7, "Fondotinta"),
            () -> verify(statement, description("quantità in magazzino")).setInt(8, 25),
            () -> verify(statement, description("percentuale di sconto")).setDouble(9, 10),
            () -> verify(statement, description("colorazione")).setString(10, "beige"),
            () -> verify(statement, description("data di inserimento")).setDate(11, DATA_INSERIMENTO),
            () -> verify(statement, never().description("L'id lo genera il database")).setInt(eq(1), anyInt()),
            () -> verify(statement, description("L'INSERT deve essere eseguito")).executeUpdate());
    }

    @ParameterizedTest(quoteTextArguments = false, name = "[{index}] righe inserite={0} → {1}")
    @CsvSource({"1, true", "0, false"})
    @DisplayName("34b - inserisciProdotto restituisce true solo se la riga è stata inserita")
    void inserisciProdotto_esitoUpdate_trueOppureFalse(int righeInserite, boolean esitoAtteso) throws SQLException {
        when(statement.executeUpdate()).thenReturn(righeInserite);

        boolean esito = model.inserisciProdotto(nuovoProdotto());

        assertEquals(esitoAtteso, esito, "Esito con " + righeInserite + " righe inserite");
    }

    @Test
    @DisplayName("34c - dopo l'inserimento il prodotto ha l'id generato dal database")
    void inserisciProdotto_inserito_idGeneratoNelBean() throws SQLException {
        ProductBean prodotto = nuovoProdotto();

        model.inserisciProdotto(prodotto);

        assertAll(
            () -> assertEquals(ID_GENERATO, prodotto.getID(), "Il bean deve avere l'id assegnato dal database"),
            () -> verify(chiavi, description("Il ResultSet delle chiavi va chiuso")).close());
    }

    // La sottocategoria è facoltativa: vuota o assente diventa NULL, come nei prodotti del dump senza sottocategoria.
    @ParameterizedTest(quoteTextArguments = false, name = "[{index}] sottocategoria \"{0}\"")
    @CsvSource(value = {"''", "NULL"}, nullValues = "NULL")
    @DisplayName("34d - inserisciProdotto salva NULL come sottocategoria se è vuota o assente")
    void inserisciProdotto_sottocategoriaVuota_null(String sottocategoria) throws SQLException {
        ProductBean prodotto = nuovoProdotto();
        prodotto.setSubCategoria(sottocategoria);

        model.inserisciProdotto(prodotto);

        assertAll(
            () -> verify(statement, description("sottocategoria NULL")).setNull(7, Types.VARCHAR),
            () -> verify(statement, never().description("nessuna sottocategoria come stringa")).setString(eq(7), anyString()));
    }

    @Test
    @DisplayName("34e - inserisciProdotto senza l'id generato lancia SQLException e lascia il bean com'era")
    void inserisciProdotto_idNonRestituito_sqlException() throws SQLException {
        when(chiavi.next()).thenReturn(false);
        ProductBean prodotto = nuovoProdotto();

        assertThrows(SQLException.class, () -> model.inserisciProdotto(prodotto),
                "Senza l'id del prodotto non si possono salvare le immagini");
        assertEquals(ID_PRODOTTO, prodotto.getID(), "L'id del bean non deve cambiare");
    }

    @Test
    @DisplayName("35 - modificaSconto passa al DB la percentuale di sconto e l'id del prodotto")
    void modificaSconto_parametri_scontoEIdImpostati() throws SQLException {
        model.modificaSconto(ID_PRODOTTO, 30);

        assertAll(
            () -> verify(statement, description("percentuale di sconto")).setDouble(1, 30),
            () -> verify(statement, description("id del prodotto")).setInt(2, ID_PRODOTTO),
            () -> verify(statement, description("L'UPDATE deve essere eseguito")).executeUpdate());
    }

    // Valori limite come nel 34b: 1 riga modificata (prodotto esistente) e 0 righe (id inesistente).
    @ParameterizedTest(quoteTextArguments = false, name = "[{index}] righe modificate={0} → {1}")
    @CsvSource({"1, true", "0, false"})
    @DisplayName("35b - modificaSconto restituisce true solo se il prodotto è stato modificato")
    void modificaSconto_esitoUpdate_trueOppureFalse(int righeModificate, boolean esitoAtteso) throws SQLException {
        when(statement.executeUpdate()).thenReturn(righeModificate);

        boolean esito = model.modificaSconto(ID_PRODOTTO, 30);

        assertEquals(esitoAtteso, esito, "Esito con " + righeModificate + " righe modificate");
    }

    @Test
    @DisplayName("36a - inserisciProdotto chiude PreparedStatement e Connection")
    void inserisciProdotto_dopoEsecuzione_risorseChiuse() throws SQLException {
        model.inserisciProdotto(nuovoProdotto());

        assertAll(
            () -> verify(statement, description("Il PreparedStatement deve essere chiuso")).close(),
            () -> verify(connection, description("La Connection deve tornare al pool con close()")).close());
    }

    @Test
    @DisplayName("36b - modificaSconto chiude PreparedStatement e Connection")
    void modificaSconto_dopoEsecuzione_risorseChiuse() throws SQLException {
        model.modificaSconto(ID_PRODOTTO, 30);

        assertAll(
            () -> verify(statement, description("Il PreparedStatement deve essere chiuso")).close(),
            () -> verify(connection, description("La Connection deve tornare al pool con close()")).close());
    }

    /** Prodotto come lo prepara AdminControl dal form; l'id è quello iniziale, prima dell'inserimento. */
    private static ProductBean nuovoProdotto() {
        ProductBean prodotto = new ProductBean();
        prodotto.setID(ID_PRODOTTO);
        prodotto.setNome("Fondotinta fluido");
        prodotto.setDescrizione("Copertura media, finish naturale");
        prodotto.setPrezzo(19.90);
        prodotto.setGrammatura(30.0);
        prodotto.setUnita("ml");
        prodotto.setCategoria("viso");
        prodotto.setSubCategoria("Fondotinta");
        prodotto.setQTA_magazzino(25);
        prodotto.setPercentualeSconto(10);
        prodotto.setColorazione("beige");
        prodotto.setDataInserimento(DATA_INSERIMENTO);
        return prodotto;
    }
}
