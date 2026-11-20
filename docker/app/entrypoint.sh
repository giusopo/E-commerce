#!/bin/sh
# Avvio del container BeautyBlind.
# Legge la password del database dal secret di Compose (DB_PASSWORD_FILE) e la consegna a Tomcat
# tramite una copia privata di catalina.properties: Tomcat trasforma ogni voce in proprieta' di
# sistema, usate da META-INF/context.xml (${db.url}, ${db.user}, ${db.password}).
# Cosi' la password non compare in variabili d'ambiente ne' negli argomenti dei processi.
set -eu

if [ -n "${DB_PASSWORD_FILE:-}" ]; then
    : "${DB_URL:?Variabile DB_URL non impostata}"
    : "${DB_USER:?Variabile DB_USER non impostata}"
    if [ ! -r "$DB_PASSWORD_FILE" ]; then
        echo "Secret non leggibile: $DB_PASSWORD_FILE" >&2
        exit 1
    fi

    # Le proprieta' Java interpretano la barra rovesciata come carattere di escape.
    escape() { printf '%s' "$1" | sed 's/\\/\\\\/g'; }

    umask 077
    config_directory=$(mktemp -d)
    config_file="$config_directory/catalina.properties"
    {
        cat "$CATALINA_HOME/conf/catalina.properties"
        printf '\n# Aggiunte da entrypoint.sh\n'
        printf 'db.url=%s\n' "$(escape "$DB_URL")"
        printf 'db.user=%s\n' "$(escape "$DB_USER")"
        printf 'db.password=%s\n' "$(escape "$(cat "$DB_PASSWORD_FILE")")"
    } > "$config_file"

    export CATALINA_OPTS="${CATALINA_OPTS:-} -Dcatalina.config=file://$config_file"
fi

exec "$@"
