CREATE VIEW HUSKELAPP_VERSJON_LATEST(huskelapp_id,latest_versjon_id) AS
SELECT huskelapp_id, max(id) FROM HUSKELAPP_VERSJON GROUP BY huskelapp_id;
