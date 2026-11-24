package Model;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import Model.Bean.RecensioneBean;
import testutil.JdbcMocks;

/** Test 37–38 del piano: recensioni dei prodotti. */
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
    @Mock private PreparedStatement insertStatement;
    @Mock private PreparedStatement updateStatement;
    @Mock private ResultSet resultSet;

    private RecensioneModel model;
    private DataSource dataSourceOriginale;

    @BeforeEach
    void setUp() throws SQLException {
        lenient().when(dataSource.getConnection()).thenReturn(connection);
        dataSourceOriginale = JdbcMocks.sostituisciDataSource(RecensioneModel.class, dataSource);
        model = new RecensioneModel();
    }

    @AfterEach
    void ripristinaDataSource() {
        JdbcMocks.sostituisciDataSource(RecensioneModel.class, dataSourceOriginale);
    }

    @Test
    @DisplayName("37 - addRecensione e modificaRecensione passano al DB utente, prodotto, punteggio e commento")
    void addEModificaRecensione_parametriCorretti() throws SQLException {
        when(connection.prepareStatement(startsWith("INSERT INTO recensione"))).thenReturn(insertStatement);
        when(connection.prepareStatement(startsWith("UPDATE recensione"))).thenReturn(updateStatement);

        LocalDate prima = LocalDate.now();
        model.addRecensione(ID_UTENTE, ID_PRODOTTO, 4, "Colore intenso, dura tutto il giorno");
        LocalDate dopo = LocalDate.now();
        // Il primo parametro di modificaRecensione è l'id della recensione (vedi ProductControl).
        model.modificaRecensione(ID_RECENSIONE, 2, "Dopo un mese si secca");

        verify(insertStatement).setInt(1, ID_UTENTE);
        verify(insertStatement).setInt(2, ID_PRODOTTO);
        verify(insertStatement).setInt(3, 4);
        verify(insertStatement).setString(4, "Colore intenso, dura tutto il giorno");
        ArgumentCaptor<String> data = ArgumentCaptor.forClass(String.class);
        verify(insertStatement).setString(eq(5), data.capture());
        LocalDate dataInserita = LocalDate.parse(data.getValue());
        assertFalse(dataInserita.isBefore(prima) || dataInserita.isAfter(dopo),
                "La data di inserimento deve essere quella di oggi: " + data.getValue());
        verify(insertStatement).executeUpdate();

        verify(updateStatement).setInt(1, 2);
        verify(updateStatement).setString(2, "Dopo un mese si secca");
        verify(updateStatement).setInt(3, ID_RECENSIONE);
        verify(updateStatement).executeUpdate();
    }

    @ParameterizedTest(quoteTextArguments = false, name = "[{index}] recensioni presenti={0}")
    @ValueSource(booleans = {true, false})
    @DisplayName("38 - getAllRecensioni e getRecensione copiano i dati oppure restituiscono lista e bean vuoti")
    void getRecensioni_presentiOAssenti_mappateOppureVuote(boolean presenti) throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        // getAllRecensioni legge una riga e poi la fine; getRecensione legge una riga.
        when(resultSet.next()).thenReturn(presenti, false, presenti);
        lenient().when(resultSet.getInt("ID")).thenReturn(ID_RECENSIONE);
        lenient().when(resultSet.getDate("data_inserimento")).thenReturn(DATA_RECENSIONE);
        lenient().when(resultSet.getInt("votazione")).thenReturn(4);
        lenient().when(resultSet.getString("commento")).thenReturn("Ottima tenuta");
        lenient().when(resultSet.getInt("PRODOTTO_ID")).thenReturn(ID_PRODOTTO);
        lenient().when(resultSet.getInt("userID")).thenReturn(ID_UTENTE);

        List<RecensioneBean> lista = model.getAllRecensioni(ID_PRODOTTO);
        RecensioneBean recensione = model.getRecensione(ID_UTENTE, ID_PRODOTTO);

        verify(statement, times(2)).setInt(1, ID_PRODOTTO);
        verify(statement).setInt(2, ID_UTENTE);
        assertNotNull(recensione, "getRecensione non deve restituire null");
        if (presenti) {
            assertEquals(1, lista.size(), "Attesa una recensione");
            for (RecensioneBean letta : List.of(lista.get(0), recensione)) {
                assertAll(
                    () -> assertEquals(ID_RECENSIONE, letta.getID()),
                    () -> assertEquals(DATA_RECENSIONE, letta.getDataInserimento()),
                    () -> assertEquals(4, letta.getVotazione()),
                    () -> assertEquals("Ottima tenuta", letta.getCommento()),
                    () -> assertEquals(ID_PRODOTTO, letta.getPRODOTTO_ID()),
                    () -> assertEquals(ID_UTENTE, letta.getUserID()),
                    () -> assertFalse(letta.isEmpty(), "Una recensione letta dal DB non è vuota"));
            }
        } else {
            assertTrue(lista.isEmpty(), "Senza righe la lista deve essere vuota");
            assertEquals(-1, recensione.getID(), "Senza righe il bean resta quello di default");
            assertTrue(recensione.isEmpty(), "Una recensione non trovata deve risultare vuota (isEmpty)");
        }
    }
}
