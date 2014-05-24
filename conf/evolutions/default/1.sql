# --- !Ups

create table transformer (
  id                        bigint not null,
  name                      varchar(255) not null,
  import_path               varchar(255),
  import_file_extension     varchar(255),
  import_filecontent_type   varchar(255),
  webservice_char_set       varchar(255),
  webservice_url            varchar(255),
  webservice_user           varchar(255),
  webservice_password       varchar(255),
  webservice_timeout        integer,
  webservice_template       clob,
  time_stamp_string         varchar(255),
  constraint uq_transformer_name unique (name),
  constraint pk_transformer primary key (id))
;

create table account (
  email                     varchar(255) not null,
  name                      varchar(255),
  password                  varchar(255),
  constraint pk_account primary key (email))
;

create sequence transformer_seq;

create sequence account_seq;




# --- !Downs

SET REFERENTIAL_INTEGRITY FALSE;

drop table if exists transformer;

drop table if exists account;

SET REFERENTIAL_INTEGRITY TRUE;

drop sequence if exists transformer_seq;

drop sequence if exists account_seq;