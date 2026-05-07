use mye030;

drop view if exists v_articles_per_year;
create view v_articles_per_year as
select a.id              as article_id,
       a.article_type,
       year(a.date_pub)  as year_pub,
       a.date_pub,
       a.publisher,
       a.journal_id,
       j.country,
       j.journal_rank
from articles a
left join journals j on j.id = a.journal_id;

drop view if exists v_top_authors;
create view v_top_authors as
select au.id             as author_id,
       au.author_name,
       a.id              as article_id,
       a.article_type,
       year(a.date_pub)  as year_pub,
       a.date_pub,
       a.publisher,
       j.country,
       j.journal_rank
from articles_authors aa
join articles a  on a.id  = aa.article_id
join authors  au on au.id = aa.author_id
left join journals j on j.id = a.journal_id;

drop view if exists v_journals_by_country;
create view v_journals_by_country as
select id, journal_rank, country
from journals;

drop view if exists v_articles_by_publisher;
create view v_articles_by_publisher as
select a.id              as article_id,
       a.article_type,
       year(a.date_pub)  as year_pub,
       a.date_pub,
       a.publisher,
       j.country,
       j.journal_rank
from articles a
left join journals j on j.id = a.journal_id
where a.publisher is not null and a.publisher <> '';

drop view if exists v_conferences_by_rank;
create view v_conferences_by_rank as
select id, conf_rank
from conferences;

drop view if exists v_articles_by_journal_rank;
create view v_articles_by_journal_rank as
select a.id              as article_id,
       a.article_type,
       year(a.date_pub)  as year_pub,
       a.date_pub,
       a.publisher,
       j.id              as journal_id,
       j.country,
       j.journal_rank
from articles a
join journals j on j.id = a.journal_id;

drop view if exists v_articles_full;
create view v_articles_full as
select a.id,
       a.article_type,
       a.title,
       a.acronym,
       a.journal_id,
       a.conference_id,
       a.date_pub,
       year(a.date_pub) as year_pub,
       a.publisher,
       j.country,
       j.journal_rank
from articles a
left join journals j on j.id = a.journal_id;

drop view if exists v_journals_full;
create view v_journals_full as
select id, journal_rank, title, acronym, country,
       best_subject_area, total_docs, total_refs
from journals;

drop view if exists v_conferences_full;
create view v_conferences_full as
select id, conf_name, acronym, conf_rank, primaryFoR
from conferences;

drop view if exists v_journal_articles_per_year;
create view v_journal_articles_per_year as
select a.journal_id,
       a.id              as article_id,
       a.article_type,
       year(a.date_pub)  as year_pub,
       a.publisher,
       j.country,
       j.journal_rank
from articles a
join journals j on j.id = a.journal_id;

drop view if exists v_journal_authors_per_year;
create view v_journal_authors_per_year as
select a.journal_id,
       aa.author_id,
       a.article_type,
       year(a.date_pub)  as year_pub,
       a.publisher,
       j.country,
       j.journal_rank
from articles a
join articles_authors aa on aa.article_id = a.id
join journals j on j.id = a.journal_id;

drop view if exists v_conference_articles_per_year;
create view v_conference_articles_per_year as
select a.conference_id,
       a.id              as article_id,
       a.article_type,
       year(a.date_pub)  as year_pub,
       a.publisher,
       c.conf_rank
from articles a
join conferences c on c.id = a.conference_id;

drop view if exists v_conference_authors_per_year;
create view v_conference_authors_per_year as
select a.conference_id,
       aa.author_id,
       a.article_type,
       year(a.date_pub)  as year_pub,
       a.publisher,
       c.conf_rank
from articles a
join articles_authors aa on aa.article_id = a.id
join conferences c on c.id = a.conference_id;
