-- Load the CSVs produced by java/src/DataExtractAndTransform.java into the mye030 schema.
--
-- Run after db_create.sql. Requires LOCAL INFILE to be enabled on both
-- server and client:
--   mysql --local-infile=1 -u root -p mye030 < load_csv.sql
-- Server side: SET GLOBAL local_infile = 1;   (or local_infile=1 in my.cnf)

SET GLOBAL local_infile = 1;

USE mye030;

-- Clean slate so the script is idempotent. Child tables first.
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE articles_authors;
TRUNCATE TABLE articles;
TRUNCATE TABLE journals;
TRUNCATE TABLE conferences;
TRUNCATE TABLE authors;
SET FOREIGN_KEY_CHECKS = 1;

-- authors.csv: id,author_name
LOAD DATA LOCAL INFILE '/home/pousoupou/Documents/MYE030/src/main/resources/data/out/authors.csv'
INTO TABLE authors
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' ESCAPED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(id, author_name);

-- journals.csv: id;journal_rank;title;acronym;country;best_subject_area;total_docs;total_refs;publisher
-- journal_rank/country/best_subject_area/total_docs/total_refs/publisher are
-- empty for unranked journals harvested from input_article.csv (e.g.
-- tech-report labs); map empty string -> NULL so they land cleanly in the
-- relaxed schema.
LOAD DATA LOCAL INFILE '/home/pousoupou/Documents/MYE030/src/main/resources/data/out/journals.csv'
INTO TABLE journals
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' ESCAPED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(id, @rank, title, acronym, @country, @bsa, @tdocs, @trefs, @pub)
SET journal_rank      = NULLIF(@rank, ''),
    country           = NULLIF(@country, ''),
    best_subject_area = NULLIF(@bsa, ''),
    total_docs        = NULLIF(@tdocs, ''),
    total_refs        = NULLIF(@trefs, ''),
    publisher         = NULLIF(@pub, '');

-- conferences.csv: id;conf_name;acronym;conf_rank;primaryFoR
LOAD DATA LOCAL INFILE '/home/pousoupou/Documents/MYE030/src/main/resources/data/out/conferences.csv'
INTO TABLE conferences
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' ESCAPED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(id, conf_name, acronym, conf_rank, primaryFoR);

-- articles.csv: id;article_type;title;acronym;date_pub;publisher;journal_id;conference_id
LOAD DATA LOCAL INFILE '/home/pousoupou/Documents/MYE030/src/main/resources/data/out/articles.csv'
INTO TABLE articles
FIELDS TERMINATED BY ';' OPTIONALLY ENCLOSED BY '"' ESCAPED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(id, article_type, title, acronym, date_pub, publisher, @jid, @cid)
SET journal_id = NULLIF(@jid, ''),
    conference_id = NULLIF(@cid, '');

-- articles_authors.csv: article_id,author_id
LOAD DATA LOCAL INFILE '/home/pousoupou/Documents/MYE030/src/main/resources/data/out/articles_authors.csv'
INTO TABLE articles_authors
FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' ESCAPED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(article_id, author_id);

-- Keep AUTO_INCREMENT counters in sync with the highest id we just inserted.
SELECT @max_author := IFNULL(MAX(id), 0) FROM authors;
SET @sql := CONCAT('ALTER TABLE authors AUTO_INCREMENT = ', @max_author + 1);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT @max_article := IFNULL(MAX(id), 0) FROM articles;
SET @sql := CONCAT('ALTER TABLE articles AUTO_INCREMENT = ', @max_article + 1);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT @max_journal := IFNULL(MAX(id), 0) FROM journals;
SET @sql := CONCAT('ALTER TABLE journals AUTO_INCREMENT = ', @max_journal + 1);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT @max_conference := IFNULL(MAX(id), 0) FROM conferences;
SET @sql := CONCAT('ALTER TABLE conferences AUTO_INCREMENT = ', @max_conference + 1);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT 'authors'  AS table_name, COUNT(*) AS n FROM authors
UNION ALL
SELECT 'articles',          COUNT(*) FROM articles
UNION ALL
SELECT 'articles_authors',  COUNT(*) FROM articles_authors
UNION ALL
SELECT 'journals',          COUNT(*) FROM journals
UNION ALL
SELECT 'conferences',       COUNT(*) FROM conferences;
