UPDATE
    "huskelapp"
SET
    "is_active" = FALSE,
    "published_at" = NULL,
    "updated_at" = CURRENT_TIMESTAMP,
    "removed_by" = 'Z999999'
WHERE
    "huskelapp"."id" IN (
        SELECT
            "sub"."id"
        FROM (
             SELECT
                 "h"."id",
                 ROW_NUMBER() OVER (PARTITION BY "h"."personident" ORDER BY "h"."created_at" DESC ) AS "rank"
             FROM
                 "huskelapp" AS "h"
             WHERE
                 "h"."is_active" = TRUE ) AS "sub"
        WHERE
            "sub"."rank" > 1 );