package dev;

import java.io.Reader;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;

/** Launcher di sviluppo: eseguire dalla radice del repository. */
public final class LocalServer {
    private LocalServer() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        Properties local = new Properties();
        Path configuration = root.resolve("dev.properties");
        if (Files.isRegularFile(configuration)) {
            try (Reader reader = Files.newBufferedReader(configuration, StandardCharsets.UTF_8)) {
                local.load(reader);
            }
        }

        String url = setting(local, "DB_URL",
                "jdbc:mysql://127.0.0.1:3307/ecommerce?serverTimezone=UTC&connectTimeout=5000");
        String user = setting(local, "DB_USER", "beautyblind");
        String password = setting(local, "DB_PASSWORD", "beautyblind");
        int port = Integer.parseInt(setting(local, "HTTP_PORT", "8080"));

        // Verifica prima del deploy per rendere leggibili gli errori di configurazione.
        Class.forName("com.mysql.cj.jdbc.Driver");
        try (Connection connection = DriverManager.getConnection(url, user, password);
             ResultSet tables = connection.getMetaData().getTables(null, null, "prodotto", null)) {
            if (!tables.next()) {
                throw new SQLException("Tabella prodotto assente: importare src/main/db/ecommerce.sql.");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Database non disponibile. Eseguire il task VS Code "
                    + "'BeautyBlind: avvia database locale' oppure configurare dev.properties.", exception);
        }

        Path webapp = root.resolve("src/main/webapp");
        // Usa anche l'output del compilatore Java di VS Code quando differisce da target/classes.
        Path classes = Path.of(LocalServer.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (!Files.isDirectory(webapp) || !Files.isDirectory(classes)) {
            throw new IllegalStateException("Avviare dalla radice del repository dopo la compilazione Maven.");
        }

        System.setProperty("db.url", url);
        System.setProperty("db.user", user);
        System.setProperty("db.password", password);

        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(root.resolve("target/tomcat").toString());
        tomcat.setPort(port);
        tomcat.getConnector().setProperty("address", InetAddress.getLoopbackAddress().getHostAddress());
        tomcat.enableNaming();

        // La radice e' necessaria per i link /Cart, /User, /img e /javascript esistenti.
        Context context = tomcat.addWebapp("", webapp.toString());
        context.setParentClassLoader(LocalServer.class.getClassLoader());
        StandardRoot resources = new StandardRoot(context);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", classes.toString(), "/"));
        context.setResources(resources);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                tomcat.stop();
                tomcat.destroy();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }, "beautyblind-shutdown"));

        tomcat.start();
        if (!context.getState().isAvailable()) {
            throw new IllegalStateException("Deploy fallito: consultare gli errori Tomcat precedenti.");
        }
        System.out.println("BeautyBlind pronto: http://localhost:" + port + "/");
        tomcat.getServer().await();
    }

    private static String setting(Properties local, String name, String fallback) {
        String environment = System.getenv(name);
        return environment != null ? environment : local.getProperty(name, fallback);
    }
}
