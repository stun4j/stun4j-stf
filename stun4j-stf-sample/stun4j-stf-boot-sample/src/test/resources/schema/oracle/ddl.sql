-- ----------------------------------------------
-- Table structure for Stun4j Stf framework
-- (Copied from stun4j-stf-core project)
-- ----------------------------------------------
--drop table stn_stf_cluster_member;
create table stn_stf_cluster_member (
  id varchar2(255) not null,
  ct_at number(20, 0) not null,
  up_at number(20, 0) not null,
  primary key(id)
);

--drop table stn_stf;
create table stn_stf (
  id number(20, 0) not null,
  callee clob not null,
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

--drop table stn_stf_delay;
create table stn_stf_delay (
  id number(20, 0) not null,
  callee clob not null,
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

-- -----------------------------------------------
-- Table structure for a Simplified Business Scenario(Transfer transaction)
-- -----------------------------------------------
-- ----------------------------
-- Table structure for the purpose do some mock
-- ----------------------------
--drop table stn_stf_sample_mock;
create table stn_stf_sample_mock (
  id varchar2(255) not null,
  value number(20, 0) not null,
  primary key (id)
);

-- ----------------------------
-- Table structure for account
-- ----------------------------
--drop table stn_stf_sample_acct;
create table stn_stf_sample_acct (
  no number(20, 0) not null,
  amt decimal(19,2) not null,
  freeze_amt decimal(19,2) not null,
  st varchar2(3) not null,
  ct_at date not null,
  up_at date not null,
  primary key (no)
);

-- ----------------------------
-- Table structure for request
-- ----------------------------
--drop table stn_stf_sample_req;
create table stn_stf_sample_req (
  id varchar2(255) not null,
  body clob default null,
  ct_at date default null,
  primary key (id)
);

-- ----------------------------
-- Table structure for transaction
-- ----------------------------
--drop table stn_stf_sample_tx;
create table stn_stf_sample_tx (
  id number(20, 0) not null,
  req_id varchar2(255) not null,
  body clob not null,
  st varchar2(2) not null,
  ct_at date not null,
  up_at date not null,
  primary key (id)
);
create unique index uk_ssstx_rid on stn_stf_sample_tx(req_id);

-- ----------------------------
-- Table structure for account operation
-- ----------------------------
--drop table stn_stf_sample_acct_op;
create table stn_stf_sample_acct_op (
  id number(20, 0) not null,
  acct_no number(20, 0) not null,
  amt_dt decimal(19,2) not null,
  decr_incr char(1) not null,
  tx_id number(20, 0) not null,
  ct_at date not null,
  primary key (id)
);
create unique index uk_sssaop_txid_ano on stn_stf_sample_acct_op(tx_id, acct_no);