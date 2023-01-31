create table stn_stf_cluster_member (
  id varchar2(255) not null,
  ct_at number(20, 0) not null,
  up_at number(20, 0) not null,
  primary key(id)
);

create table stn_stf (
  id number(20, 0) not null,
  callee clob not null,
  callee_bytes blob null,
  st varchar2(2) not null,
  is_dead char(1) not null,
  retry_times number(3, 0) not null,
  timeout_secs number(7, 0) not null,
  timeout_at number(20, 0) not null,
  ct_at number(20, 0) not null,
  up_at number(20, 0) not null,
  primary key(id)
);
create index idx_ss_tat_st_isd on stn_stf(timeout_at, st, is_dead);
  
create table stn_stf_delay (
  id number(20, 0) not null,
  callee clob not null,
  callee_bytes blob null,
  st varchar2(2) not null,
  is_dead char(1) not null,
  retry_times number(3, 0) not null,
  timeout_secs number(7, 0) not null,
  timeout_at number(20, 0) not null,
  ct_at number(20, 0) not null,
  up_at number(20, 0) not null,
  primary key(id)
);
create index idx_ssd_tat_st_isd on stn_stf_delay(timeout_at, st, is_dead);