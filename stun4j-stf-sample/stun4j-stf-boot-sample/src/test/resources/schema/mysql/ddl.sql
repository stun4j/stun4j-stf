-- ----------------------------------------------
-- Table structure for Stun4j Stf framework
-- (Copied from stun4j-stf-core project)
-- ----------------------------------------------
--drop table if exists stn_stf_cluster_member;
create table stn_stf_cluster_member (
  id varchar(255) not null,
  ct_at bigint(20) unsigned not null,
  up_at bigint(20) unsigned not null,
  primary key (id)
);

--drop table if exists stn_stf;
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

--drop table if exists stn_stf_delay;
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

-- -----------------------------------------------
-- Table structure for a Simplified Business Scenario(Transfer transaction)
-- -----------------------------------------------
-- ----------------------------
-- Table structure for the purpose do some mock
-- ----------------------------
--drop table if exists stn_stf_sample_mock;
create table stn_stf_sample_mock (
  id varchar(255) not null,
  value bigint(20) not null,
  primary key (id)
);

-- ----------------------------
-- Table structure for account
-- ----------------------------
--drop table if exists stn_stf_sample_acct;
create table stn_stf_sample_acct (
  no bigint(20) unsigned not null,
  amt decimal(19,2) not null,
  freeze_amt decimal(19,2) not null,
  st varchar(3) not null,
  ct_at datetime not null,
  up_at datetime not null,
  primary key (no)
);

-- ----------------------------
-- Table structure for request
-- ----------------------------
--drop table if exists stn_stf_sample_req;
create table stn_stf_sample_req (
  id varchar(255) not null,
  body json default null,
  ct_at datetime default null,
  primary key (id)
);

-- ----------------------------
-- Table structure for transaction
-- ----------------------------
--drop table if exists stn_stf_sample_tx;
create table stn_stf_sample_tx (
  id bigint(20) unsigned not null,
  req_id varchar(255) not null,
  body json not null,
  st varchar(2) not null,
  ct_at datetime not null,
  up_at datetime not null,
  primary key (id),
  unique key uk_ssstx_rid (req_id)
);

-- ----------------------------
-- Table structure for account operation
-- ----------------------------
--drop table if exists stn_stf_sample_acct_op;
create table stn_stf_sample_acct_op (
  id bigint(20) unsigned not null,
  acct_no bigint(20) unsigned not null,
  amt_dt decimal(19,2) unsigned not null,
  decr_incr char(1) not null,
  tx_id bigint(20) unsigned not null,
  ct_at datetime not null,
  primary key (id),
  unique key uk_sssaop_txid_ano (tx_id, acct_no)
);