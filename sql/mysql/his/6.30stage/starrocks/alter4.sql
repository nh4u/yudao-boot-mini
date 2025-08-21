use acquisition;

ALTER TABLE usage_cost
    MODIFY COLUMN `standard_coal_equivalent` decimal(30, 10) NOT NULL DEFAULT "0.0" COMMENT "折标煤";