create table stn_stf (
  id bigint not null,
  callee text not null,
  st varchar(2) not null,
  is_dead char(1) not null,
  is_locked char(1) not null,
  retry_times smallint not null,
  timeout_at bigint not null,
  ct_at bigint not null,
  up_at bigint not null,
  primary key(id)
);
create index idx_stn_stf_timeout_at on stn_stf using btree (timeout_at);