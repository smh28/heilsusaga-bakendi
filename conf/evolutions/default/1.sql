# --- Created by Ebean DDL
# To stop Ebean DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table agree_written (
  id                            bigint auto_increment not null,
  created                       datetime(6),
  date_agreed                   datetime(6),
  person_id                     bigint,
  staff_id                      bigint,
  constraint pk_agree_written primary key (id)
);

create table log (
  id                            bigint auto_increment not null,
  created                       datetime(6),
  person_id                     bigint,
  text                          varchar(255),
  log_type_id                   bigint,
  constraint pk_log primary key (id)
);

create table log_type (
  id                            bigint auto_increment not null,
  code                          varchar(255),
  name                          varchar(255),
  constraint pk_log_type primary key (id)
);

create table person (
  id                            bigint auto_increment not null,
  ssn                           varchar(255),
  name                          varchar(255),
  password_hash                 varchar(255),
  email                         varchar(255),
  agreed                        tinyint(1) default 0,
  rejected                      tinyint(1) default 0,
  has_logged_in                 tinyint(1) default 0,
  admin                         tinyint(1) default 0,
  alive                         tinyint(1) default 0,
  wrong_password                integer,
  sex                           varchar(255),
  dob                           datetime(6),
  agreed_online                 bigint not null,
  agreed_logintype              varchar(255),
  agreed_date                   datetime(6),
  constraint pk_person primary key (id)
);

alter table agree_written add constraint fk_agree_written_person_id foreign key (person_id) references person (id) on delete restrict on update restrict;
create index ix_agree_written_person_id on agree_written (person_id);

alter table agree_written add constraint fk_agree_written_staff_id foreign key (staff_id) references person (id) on delete restrict on update restrict;
create index ix_agree_written_staff_id on agree_written (staff_id);

alter table log add constraint fk_log_person_id foreign key (person_id) references person (id) on delete restrict on update restrict;
create index ix_log_person_id on log (person_id);

alter table log add constraint fk_log_log_type_id foreign key (log_type_id) references log_type (id) on delete restrict on update restrict;
create index ix_log_log_type_id on log (log_type_id);


# --- !Downs

alter table agree_written drop foreign key fk_agree_written_person_id;
drop index ix_agree_written_person_id on agree_written;

alter table agree_written drop foreign key fk_agree_written_staff_id;
drop index ix_agree_written_staff_id on agree_written;

alter table log drop foreign key fk_log_person_id;
drop index ix_log_person_id on log;

alter table log drop foreign key fk_log_log_type_id;
drop index ix_log_log_type_id on log;

drop table if exists agree_written;

drop table if exists log;

drop table if exists log_type;

drop table if exists person;

