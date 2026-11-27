-- Aggiorna allo schema attuale di ecommerce.sql un database creato con la versione precedente,
-- senza perdere i dati. Va eseguito una volta, da un utente con il privilegio ALTER (es. root):
-- l'utente dell'applicazione ha solo SELECT, INSERT, UPDATE e DELETE.
-- Un database nuovo non ne ha bisogno: ecommerce.sql contiene già queste modifiche.

-- Prezzi e totali con due decimali (prima decimal(10,0): i centesimi venivano arrotondati).
ALTER TABLE `prodotto` MODIFY `prezzo` decimal(10,2) NOT NULL;
ALTER TABLE `ordine` MODIFY `prezzo_tot` decimal(10,2) DEFAULT NULL;
-- I totali salvati prima erano arrotondati all'unità: si ricalcolano dai prezzi pagati, che hanno i decimali.
UPDATE `ordine` SET `prezzo_tot` =
  (SELECT SUM(`prezzo_all_acquisto` * `QTAordinata`) FROM `contiene` WHERE `contiene`.`ID_ordine` = `ordine`.`ID`)
  WHERE `stato` = 'concluso';

-- Al più un carrello (ordine in corso) per utente: carrello vale 1 solo per l'ordine in corso, e
-- l'indice UNIQUE non ammette due righe con lo stesso utente e carrello = 1 (i NULL non si confrontano).
ALTER TABLE `ordine`
  ADD `carrello` tinyint(1) DEFAULT 1 COMMENT '1 per il carrello (ordine in corso), NULL per un ordine concluso' AFTER `stato`;
UPDATE `ordine` SET `carrello` = NULL WHERE `stato` <> 'in corso';

-- Se questa istruzione fallisce, qualche utente ha più di un ordine in corso: va sistemato a mano.
ALTER TABLE `ordine`
  ADD UNIQUE KEY `un_carrello_per_utente` (`UTENTE_ID`,`carrello`),
  ADD CONSTRAINT `carrello_coerente` CHECK (`stato` = 'in corso' and `carrello` = 1 or `stato` <> 'in corso' and `carrello` is null);
