create table stn_stf (
  id number(20, 0) not null,
  callee clob not null,
  st varchar2(2) not null,
  is_dead char(1) not null,
  is_running char(1) not null,
  retry_times number(3, 0) not null,
  timeout_secs number(7, 0),
  ct_at number(20, 0) not null,
  up_at number(20, 0) not null,
  constraint pk_stn_stf primary key(id)
);