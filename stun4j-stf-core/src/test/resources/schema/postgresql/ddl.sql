create table stn_stf (
  id bigint not null,
  callee text not null,
  st varchar(2) not null,
  is_dead char(1) not null,
  is_running char(1) not null,
  retry_times smallint not null,
  ct_at bigint not null,
  up_at bigint not null,
  primary key(id)
);