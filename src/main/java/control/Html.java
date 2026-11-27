package control;

/**
 * Escape dei caratteri speciali dell'HTML. Serve a stampare nelle pagine, senza che venga interpretato come
 * codice, il testo scritto da utenti e amministratori, e a salvare già sottoposti a escape i commenti delle
 * recensioni.
 */
public final class Html {

	private Html() {
	}

	/** Sostituisce {@code & < > " '} con le entità HTML corrispondenti; {@code null} diventa la stringa vuota. */
	public static String escape(String testo) {
		if (testo == null) {
			return "";
		}
		StringBuilder risultato = new StringBuilder(testo.length());
		for (char c : testo.toCharArray()) {
			switch (c) {
				case '&': risultato.append("&amp;"); break;
				case '<': risultato.append("&lt;"); break;
				case '>': risultato.append("&gt;"); break;
				case '"': risultato.append("&quot;"); break;
				case '\'': risultato.append("&#039;"); break;
				default: risultato.append(c);
			}
		}
		return risultato.toString();
	}
}
