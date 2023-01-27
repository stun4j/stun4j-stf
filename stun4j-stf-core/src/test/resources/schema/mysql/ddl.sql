create table stn_stf_cluster_member (
  id varchar(255) not null,
  ct_at bigint(20) unsigned not null,
  up_at bigint(20) unsigned not null,
  primary key (id)
);

create table stn_stf (
  id bigint(20) unsigned not null,
  callee json not null,
  st varchar(2) not null,
  is_dead char(1) not null,
  retry_times tinyint(3) unsigned not null,
  timeout_secs mediumint(7) unsigned not null,
  timeout_at bigint(20) unsigned not null,
  ct_at bigint(20) unsigned not null,
  up_at bigint(20) unsigned not null,
  primary key(id),
  key idx_ss_tat_st_isd (timeout_at, st, is_dead) using btree
);

create table stn_stf_delay (
  id bigint(20) unsigned not null,
  callee json not null,
  st varchar(2) not null,
  is_dead char(1) not null,
  retry_times tinyint(3) unsigned not null,
  timeout_secs mediumint(7) unsigned not null,
  timeout_at bigint(20) unsigned not null,
  ct_at bigint(20) unsigned not null,
  up_at bigint(20) unsigned not null,
  primary key(id),
  key idx_ssd_tat_st_isd (timeout_at, st, is_dead) using btree
);