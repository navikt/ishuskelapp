 DELETE FROM huskelapp_versjon hv WHERE tekst='' AND EXISTS
 (SELECT 1 FROM huskelapp_versjon hv2 WHERE hv2.huskelapp_id = hv.huskelapp_id AND hv2.id < hv.id);
