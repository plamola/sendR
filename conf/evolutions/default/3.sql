# --- !Ups
ALTER TABLE TRANSFORMER
ALTER COLUMN id bigint NOT NULL DEFAULT nextval('transformer_seq');


# --- !Downs
ALTER TABLE TRANSFORMER
ALTER COLUMN  id bigint NOT NULL;


