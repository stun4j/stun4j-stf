create table stn_stf_cluster_member (
  id varchar(255) not null,
  ct_at bigint not null,
  up_at bigint not null,
  primary key (id)
);

create table stn_stf (
  id bigint not null,
  callee text not null,
  callee_bytes bytea,
  st varchar(2) not null,
  is_dead char(1) not null,
  retry_times smallint not null,
  timeout_secs int not null,
  timeout_at bigint not null,
  ct_at bigint not null,
  up_at bigint not null,
  primary key(id)
);
create index idx_ss_tat_st_isd on stn_stf using btree (timeout_at, st, is_dead);
  
create table stn_stf_delay (
  id bigint not null,
  callee text not null,
  callee_bytes bytea,
  st varchar(2) not null,
  is_dead char(1) not null,
  retry_times smallint not null,
  timeout_secs int not null,
  timeout_at bigint not null,
  ct_at bigint not null,
  up_at bigint not null,
  primary key(id)
);
create index idx_ssd_tat_st_isd on stn_stf_delay using btree (timeout_at, st, is_dead);