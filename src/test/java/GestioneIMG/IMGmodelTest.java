package GestioneIMG;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import testutil.JdbcMocks;

/**
 * Test 78a-78d di pianoTest.md: lettura e salvataggio delle immagini. IMGmodel ha solo metodi statici, ma
 * il suo DataSource è un campo statico come nei DAO, quindi si sostituisce allo stesso modo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IMGmodel - immagini dei prodotti")
class IMGmodelTest {

    private static final int ID_PRODOTTO = 12;
    private static final byte[] IMMAGINE = {(byte) 0xFF, (byte) 0xD8, 1, 2, 3};

    @Mock private DataSource dataSource;
    @Mock private Connection connection;
    @Mock private PreparedStatement statement;
    @Mock private ResultSet resultSet;

    private DataSource dataSourceOriginale;

    @BeforeEach
    void setUp() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        dataSourceOriginale = JdbcMocks.sostituisciDataSource(IMGmodel.class, dataSource);
    }

    @AfterEach
    void ripristinaDataSource() {
        JdbcMocks.sostituisciDataSource(IMGmodel.class, dataSourceOriginale);
    }

    // Prima id e ruolo, presi dalla richiesta, venivano concatenati alla query.
    @Test
    @DisplayName("78a - load passa id e ruolo come parametri, mai nel testo della query, e restituisce i byte")
    void load_ruoloMalevolo_parametriEImmagine() throws SQLException {
        String ruoloMalevolo = "principale' OR '1'='1";
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getBytes("IMMAGINE")).thenReturn(IMMAGINE);

        byte[] letta = IMGmodel.load(ID_PRODOTTO, ruoloMalevolo);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertAll(
                () -> assertFalse(sql.getValue().contains(ruoloMalevolo), "Il ruolo non va concatenato: " + sql.getValue()),
                () -> verify(statement).setInt(1, ID_PRODOTTO),
                () -> verify(statement).setString(2, ruoloMalevolo),
                () -> assertArrayEquals(IMMAGINE, letta, "Byte dell'immagine"));
    }

    @Test
    @DisplayName("78b - load senza immagine con quel ruolo restituisce null")
    void load_nessunaRiga_null() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        assertNull(IMGmodel.load(ID_PRODOTTO, "secondaria"), "Senza riga non c'è immagine");
    }

    @Test
    @DisplayName("78c - load chiude ResultSet, statement e connessione")
    void load_chiamataRiuscita_risorseChiuse() throws SQLException {
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        IMGmodel.load(ID_PRODOTTO, "principale");

        assertAll(
                () -> verify(resultSet, description("Il ResultSet va chiuso")).close(),
                () -> verify(statement, description("Lo statement va chiuso")).close(),
                () -> verify(connection, description("La connessione va chiusa")).close());
    }

    @Test
    @DisplayName("78d - updatePhoto salva il contenuto del file con ruolo e prodotto, e chiude statement e connessione")
    void updatePhoto_file_immagineSalvataERisorseChiuse(@TempDir Path cartella) throws Exception {
        Path file = cartella.resolve("foto.jpg");
        Files.write(file, IMMAGINE);

        IMGmodel.updatePhoto(ID_PRODOTTO, "principale", file.toString());

        assertAll(
                () -> verify(statement, description("contenuto del file, con la sua lunghezza"))
                        .setBinaryStream(eq(1), any(InputStream.class), eq((long) IMMAGINE.length)),
                () -> verify(statement, description("ruolo")).setString(2, "principale"),
                () -> verify(statement, description("prodotto")).setInt(3, ID_PRODOTTO),
                () -> verify(statement, description("L'INSERT deve essere eseguito")).executeUpdate(),
                () -> verify(statement, description("Lo statement va chiuso")).close(),
                () -> verify(connection, description("La connessione va chiusa")).close());
    }
}
