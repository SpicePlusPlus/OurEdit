create database if not exists OurEditDB;
use OurEditDB;

create table if not exists Documents (
	doc_id int,
	doc_title varchar(50),
	creator_name varchar(50),
	first_created datetime,
	last_modified datetime,
	in_use boolean not null default 0,
	cur_user varchar(50),
	marked_for_deletion_by varchar(50) not null default 'None',
	deletion_confirmations int not null default 0,
	primary key (doc_id));
	
create table if not exists users (
	username varchar(50),
	password varchar(50),
	primary key (username));

create table if not exists ids (
	id_type varchar(50),
	cur_id int,
	primary key (id_type));

delete from ids;
insert into ids values ('doc_id', 0);