# --- !Ups

create table "rows" (
  "id" bigserial primary key,
  "NFAItem" varchar not null,
  "FormType" varchar not null,
  "CheckCashed" bigint not null,
  "Approved" bigint not null
);

