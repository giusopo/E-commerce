package testutil;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

/**
 * Strumenti condivisi dai test dei DAO, che sostituiscono il database con oggetti Mockito.
 */
public final class JdbcMocks {

    private JdbcMocks() {
    }

    /**
     * Una chiamata a un metodo di un DAO. Serve ai test parametrizzati che provano lo stesso caso
     * di errore (es. connessione fallita) su tutti i metodi: nel report ogni metodo ha la sua riga.
     */
    @FunctionalInterface
    public interface ChiamataDao<T> {
        void esegui(T dao) throws SQLException;
    }

    /**
     * Sostituisce il campo statico privato {@code ds} di un DAO con il DataSource indicato.
     * Serve perché i DAO ottengono il DataSource via JNDI e non permettono di iniettarlo.
     *
     * @return il DataSource che c'era prima, da rimettere al suo posto in @AfterEach
     */
    public static DataSource sostituisciDataSource(Class<?> dao, DataSource nuovo) {
        try {
            Field campo = dao.getDeclaredField("ds");
            campo.setAccessible(true);
            DataSource precedente = (DataSource) campo.get(null);
            campo.set(null, nuovo);
            return precedente;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Impossibile impostare il DataSource di " + dao.getSimpleName(), e);
        }
    }

    /**
     * Crea un ResultSet finto che restituisce le righe indicate, una per ogni chiamata a next().
     * Ogni riga associa il nome della colonna al suo valore.
     * Come un vero driver JDBC, getInt su un valore decimale tronca i decimali
     * (12.99 diventa 12), mentre una colonna assente vale 0 oppure null.
     */
    public static ResultSet resultSet(List<Map<String, Object>> righe) {
        ResultSet rs = mock(ResultSet.class);
        AtomicInteger cursore = new AtomicInteger(-1);
        try {
            lenient().when(rs.next()).thenAnswer(inv -> cursore.incrementAndGet() < righe.size());
            lenient().when(rs.getInt(anyString())).thenAnswer(inv -> {
                Object v = valore(righe, cursore, inv.getArgument(0));
                return v == null ? 0 : ((Number) v).intValue();
            });
            lenient().when(rs.getDouble(anyString())).thenAnswer(inv -> {
                Object v = valore(righe, cursore, inv.getArgument(0));
                return v == null ? 0.0 : ((Number) v).doubleValue();
            });
            lenient().when(rs.getString(anyString())).thenAnswer(inv -> {
                Object v = valore(righe, cursore, inv.getArgument(0));
                return v == null ? null : v.toString();
            });
            lenient().when(rs.getBoolean(anyString())).thenAnswer(
                    inv -> Boolean.TRUE.equals(valore(righe, cursore, inv.getArgument(0))));
            lenient().when(rs.getDate(anyString())).thenAnswer(
                    inv -> (Date) valore(righe, cursore, inv.getArgument(0)));
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
        return rs;
    }

    private static Object valore(List<Map<String, Object>> righe, AtomicInteger cursore, String colonna)
            throws SQLException {
        int riga = cursore.get();
        if (riga < 0 || riga >= righe.size()) {
            throw new SQLException("Lettura della colonna " + colonna + " fuori da una riga valida");
        }
        return righe.get(riga).get(colonna);
    }
}
