package admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
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

/** Test 34–36 del piano: azioni dell'amministratore sul catalogo. */
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

    @ParameterizedTest(quoteTextArguments = false, name = "[{index}] righe inserite={0} → {1}")
    @CsvSource({"1, true", "0, false"})
    @DisplayName("34 - inserisciProdotto passa i 10 campi e restituisce true solo se la riga è inserita")
    void inserisciProdotto_esitoUpdate_trueOppureFalse(int righeInserite, boolean esitoAtteso) throws SQLException {
        when(statement.executeUpdate()).thenReturn(righeInserite);

        boolean esito = model.inserisciProdotto(nuovoProdotto());

        verify(statement).setInt(1, ID_PRODOTTO);
        verify(statement).setString(2, "Fondotinta fluido");
        verify(statement).setString(3, "Copertura media, finish naturale");
        verify(statement).setDouble(4, 19.90);
        verify(statement).setDouble(5, 30.0);
        verify(statement).setString(6, "viso");
        verify(statement).setInt(7, 25);
        verify(statement).setDouble(8, 10);
        verify(statement).setString(9, "beige");
        verify(statement).setDate(10, DATA_INSERIMENTO);
        assertEquals(esitoAtteso, esito);
    }

    @Test
    @DisplayName("35 - modificaSconto passa al DB la percentuale di sconto e l'id del prodotto")
    void modificaSconto_parametri_scontoEIdImpostati() throws SQLException {
        model.modificaSconto(ID_PRODOTTO, 30);

        verify(statement).setDouble(1, 30);
        verify(statement).setInt(2, ID_PRODOTTO);
        verify(statement).executeUpdate();
    }

    @Test
    @DisplayName("36 - inserisciProdotto e modificaSconto chiudono PreparedStatement e Connection")
    void azioniAdmin_dopoEsecuzione_risorseChiuse() throws SQLException {
        model.inserisciProdotto(nuovoProdotto());
        model.modificaSconto(ID_PRODOTTO, 30);

        verify(statement, times(2).description("Ogni PreparedStatement aperto deve essere chiuso"))
                .close();
        verify(connection, times(2).description("Ogni Connection presa dal pool deve essere restituita con close()"))
                .close();
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
