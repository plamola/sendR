# --- !Ups
ALTER TABLE TRANSFORMER
ALTER COLUMN id bigint NOT NULL DEFAULT nextval('transformer_seq');

DROP SEQUENCE account_seq;

ALTER TABLE account
DROP COLUMN name;

# --- !Downs
ALTER TABLE TRANSFORMER
ALTER COLUMN  id bigint NOT NULL;

CREATE SEQUENCE account_seq;

ALTER TABLE account
ADD COLUMN name varchar(255);

