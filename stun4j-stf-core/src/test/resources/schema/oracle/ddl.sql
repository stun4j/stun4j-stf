create table stn_stf (
  id number(20, 0) not null,
  callee clob not null,
  st varchar2(2) not null,
  is_dead char(1) not null,
  is_locked char(1) not null,
  retry_times number(3, 0) not null,
  timeout_at number(20, 0) not null,
  ct_at number(20, 0) not null,
  up_at number(20, 0) not null,
  constraint pk_stn_stf primary key(id)
);
create index idx_stn_stf_timeout_at on stn_stf(timeout_at);