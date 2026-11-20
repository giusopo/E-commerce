# Immagine di BeautyBlind: Tomcat 9 (javax.servlet) con Java 21.
# Prima della build generare il WAR con: ./mvnw package -DskipTests

# Versione esplicita: Tomcat 9.0.124, JRE 21 Temurin, Ubuntu 24.04 (noble). Mai :latest.
FROM tomcat:9.0.124-jre21-temurin-noble

# Utente di sistema senza privilegi né shell, con UID fisso.
RUN groupadd --system --gid 10001 beautyblind \
    && useradd --system --uid 10001 --gid beautyblind \
       --no-create-home --shell /usr/sbin/nologin beautyblind

# Il datasource JNDI jdbc/ecommerce (META-INF/context.xml) è creato da Tomcat,
# quindi il driver JDBC deve stare nelle librerie del server, non solo nel WAR.
COPY target/ROOT/WEB-INF/lib/mysql-connector-j-*.jar /usr/local/tomcat/lib/

# L'app va pubblicata alla radice "/" perché usa collegamenti assoluti.
COPY --chown=beautyblind:beautyblind target/ROOT.war /usr/local/tomcat/webapps/ROOT.war

# Tomcat deve poter scrivere solo dove serve a runtime.
# conf/ resta di root e in sola lettura per l'utente dell'app.
RUN mkdir -p /usr/local/tomcat/conf/Catalina/localhost \
    && chown -R beautyblind:beautyblind \
       /usr/local/tomcat/webapps /usr/local/tomcat/work /usr/local/tomcat/temp \
       /usr/local/tomcat/logs /usr/local/tomcat/conf/Catalina

# Legge le credenziali del database dai secrets di Compose (vedi docker/app/entrypoint.sh).
COPY --chmod=0755 docker/app/entrypoint.sh /usr/local/bin/beautyblind-entrypoint.sh

USER beautyblind

EXPOSE 8080

# Forma exec: l'entrypoint termina con "exec", quindi Tomcat diventa il processo 1
# e riceve direttamente SIGTERM da "docker stop".
ENTRYPOINT ["beautyblind-entrypoint.sh"]
CMD ["catalina.sh", "run"]
