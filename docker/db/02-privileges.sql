-- Eseguito dall'immagine MariaDB solo alla prima inizializzazione del volume, dopo il dump.
-- L'immagine concede all'utente MARIADB_USER tutti i privilegi su MARIADB_DATABASE:
-- l'applicazione esegue solo SELECT, INSERT, UPDATE e DELETE, quindi si riducono a questi.
REVOKE ALL PRIVILEGES ON `ecommerce`.* FROM 'beautyblind_app'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON `ecommerce`.* TO 'beautyblind_app'@'%';
