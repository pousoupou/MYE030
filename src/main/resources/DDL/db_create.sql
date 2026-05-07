drop database if exists mye030;
create database mye030;

use mye030;

create table if not exists authors (
    id INT AUTO_INCREMENT PRIMARY KEY,
    author_name VARCHAR(255) NOT NULL
);

create table if not exists journals (
    id INT AUTO_INCREMENT PRIMARY KEY,
    journal_rank INT NOT NULL,
    title TEXT NOT NULL,
    acronym VARCHAR(60) NOT NULL,
    country VARCHAR(60) NOT NULL,
    best_subject_area VARCHAR(255) NOT NULL,
    total_docs INT NOT NULL,
    total_refs INT NOT NULL
);

create table if not exists conferences (
    id INT AUTO_INCREMENT PRIMARY KEY,
    conf_name TEXT NOT NULL,
    acronym VARCHAR(60) NOT NULL,
    conf_rank VARCHAR(50) NOT NULL,
    primaryFoR VARCHAR(10) NOT NULL
);

create table if not exists articles (
    id INT AUTO_INCREMENT PRIMARY KEY,
    article_type ENUM ('J', 'C') NOT NULL,
    title TEXT NOT NULL,
    acronym VARCHAR(200) NOT NULL,
    journal_id INT NULL,
    conference_id INT NULL,
    date_pub DATE NOT NULL,
    publisher VARCHAR(255),
    FOREIGN KEY (journal_id) REFERENCES journals(id),
    FOREIGN KEY (conference_id) REFERENCES conferences(id)
);

create table if not exists articles_authors (
    article_id INT NOT NULL,
    author_id INT NOT NULL,
    PRIMARY KEY (article_id, author_id),
    FOREIGN KEY (article_id) REFERENCES articles(id),
    FOREIGN KEY (author_id) REFERENCES authors(id)
);
