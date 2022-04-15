create table stn_stf (
  id bigint(20) unsigned not null,
  callee json not null,
  st varchar(2) not null,
  is_dead char(1) not null,
  is_running char(1) not null,
  retry_times tinyint(3) unsigned not null,
  ct_at bigint(20) unsigned not null,
  up_at bigint(20) unsigned not null,
  primary key(id)
);