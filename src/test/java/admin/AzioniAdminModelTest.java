package admin;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AzioniAdminModel - azioni dell'amministratore sul catalogo")
class AzioniAdminModelTest {

    private static final int ID_PRODOTTO = 50;
    private static final Date DATA_INSERIMENTO = Date.valueOf("2026-11-01");

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private PreparedStatement statement;

    private AzioniAdminModel model;
    private DataSource dataSourceOriginale;

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        lenient().when(connection.prepareStatement(anyString())).thenReturn(statement);
        dataSourceOriginale = JdbcMocks.sostituisciDataSource(AzioniAdminModel.class, dataSource);
        model = new AzioniAdminModel();
    }

    @AfterEach
    void ripristinaDataSource() {
        JdbcMocks.sostituisciDataSource(AzioniAdminModel.class, dataSourceOriginale);
    }

    @Test
    @DisplayName("34a - inserisciProdotto passa al DB i 10 campi del prodotto")
    void inserisciProdotto_prodottoValido_dieciCampiImpostati() throws SQLException {
        model.inserisciProdotto(nuovoProdotto());

        assertAll(
            () -> verify(statement, description("id")).setInt(1, ID_PRODOTTO),
            () -> verify(statement, description("nome")).setString(2, "Fondotinta fluido"),
            () -> verify(statement, description("descrizione")).setString(3, "Copertura media, finish naturale"),
            () -> verify(statement, description("prezzo")).setDouble(4, 19.90),
            () -> verify(statement, description("grammatura")).setDouble(5, 30.0),
            () -> verify(statement, description("categoria")).setString(6, "viso"),
            () -> verify(statement, description("quantità in magazzino")).setInt(7, 25),
            () -> verify(statement, description("percentuale di sconto")).setDouble(8, 10),
            () -> verify(statement, description("colorazione")).setString(9, "beige"),
            () -> verify(statement, description("data di inserimento")).setDate(10, DATA_INSERIMENTO),
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
    @DisplayName("35 - modificaSconto passa al DB la percentuale di sconto e l'id del prodotto")
    void modificaSconto_parametri_scontoEIdImpostati() throws SQLException {
        model.modificaSconto(ID_PRODOTTO, 30);

        assertAll(
            () -> verify(statement, description("percentuale di sconto")).setDouble(1, 30),
            () -> verify(statement, description("id del prodotto")).setInt(2, ID_PRODOTTO),
            () -> verify(statement, description("L'UPDATE deve essere eseguito")).executeUpdate());
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

    private static ProductBean nuovoProdotto() {
        ProductBean prodotto = new ProductBean();
        prodotto.setID(ID_PRODOTTO);
        prodotto.setNome("Fondotinta fluido");
        prodotto.setDescrizione("Copertura media, finish naturale");
        prodotto.setPrezzo(19.90);
        prodotto.setGrammatura(30.0);
        prodotto.setCategoria("viso");
        prodotto.setQTA_magazzino(25);
        prodotto.setPercentualeSconto(10);
        prodotto.setColorazione("beige");
        prodotto.setDataInserimento(DATA_INSERIMENTO);
        return prodotto;
    }
}
