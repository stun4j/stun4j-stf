-- ----------------------------------------------
-- Table structure for Stun4j Stf framework
-- (Copied from stun4j-stf-core project)
-- ----------------------------------------------
--drop table if exists stn_stf_cluster_member;
create table stn_stf_cluster_member (
  id varchar(255) not null,
  ct_at bigint not null,
  up_at bigint not null,
  primary key (id)
);

--drop table if exists stn_stf;
create table stn_stf (
  id bigint not null,
  callee text not null,
  st varchar(2) not null,
  is_dead char(1) not null,
  retry_times smallint not null,
  timeout_secs int not null,
  timeout_at bigint not null,
  ct_at bigint not null,
  up_at bigint not null,
  primary key(id)
);
create index idx_stn_stf_timeout_at on stn_stf using btree (timeout_at);

--drop table if exists stn_stf_delay;
create table stn_stf_delay (
  id bigint not null,
  callee text not null,
  st varchar(2) not null,
  is_dead char(1) not null,
  retry_times smallint not null,
  timeout_secs int not null,
  timeout_at bigint not null,
  ct_at bigint not null,
  up_at bigint not null,
  primary key(id)
);
create index idx_stn_stf_delay_timeout_at on stn_stf_delay using btree (timeout_at);

-- -----------------------------------------------
-- Table structure for a Simplified Business Scenario(Transfer transaction)
-- -----------------------------------------------
-- ----------------------------
-- Table structure for the purpose do some mock
-- ----------------------------
--drop table if exists stn_stf_sample_mock;
create table stn_stf_sample_mock (
  id varchar(255) not null,
  value smallint not null,
  primary key (id)
);

-- ----------------------------
-- Table structure for account
-- ----------------------------
--drop table if exists stn_stf_sample_acct;
create table stn_stf_sample_acct (
  no bigint not null,
  amt decimal(19,2) not null,
  freeze_amt decimal(19,2) not null,
  st varchar(3) not null,
  ct_at timestamp not null,
  up_at timestamp not null,
  primary key (no)
);

-- ----------------------------
-- Table structure for request
-- ----------------------------
--drop table if exists stn_stf_sample_req;
create table stn_stf_sample_req (
  id varchar(255) not null,
  body text default null,
  ct_at timestamp default null,
  primary key (id)
);

-- ----------------------------
-- Table structure for transaction
-- ----------------------------
--drop table if exists stn_stf_sample_tx;
create table stn_stf_sample_tx (
  id bigint not null,
  req_id varchar(255) not null,
  body text not null,
  st varchar(2) not null,
  ct_at timestamp not null,
  up_at timestamp not null,
  primary key (id),
  constraint uk_req_id unique (req_id)
);

-- ----------------------------
-- Table structure for account operation
-- ----------------------------
--drop table if exists stn_stf_sample_acct_op;
create table stn_stf_sample_acct_op (
  id bigint not null,
  acct_no bigint not null,
  amt_dt decimal(19,2) not null,
  decr_incr char(1) not null,
  tx_id bigint not null,
  ct_at timestamp not null,
  primary key (id),
  constraint uk_tx_id_acct_no unique (tx_id, acct_no)
);