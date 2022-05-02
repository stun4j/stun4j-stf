-- ----------------------------------------------
-- Table structure for Stun4j Stf framework
-- (Copied from stun4j-stf-core project)
-- ----------------------------------------------
--drop table if exists stn_stf;
create table stn_stf (
  id bigint(20) unsigned not null,
  callee json not null,
  st varchar(2) not null,
  is_dead char(1) not null,
  is_locked char(1) not null,
  retry_times tinyint(3) unsigned not null,
  timeout_at bigint(20) unsigned not null,
  ct_at bigint(20) unsigned not null,
  up_at bigint(20) unsigned not null,
  primary key(id),
  key idx_timeout_at (timeout_at) using btree
);

-- -----------------------------------------------
-- Table structure for a Simplified Business Scenario(Transfer transaction)
-- -----------------------------------------------
-- ----------------------------
-- Table structure for account
-- ----------------------------
--drop table if exists acct;
create table acct (
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
--drop table if exists req;
create table req (
  id varchar(255) not null,
  body json default null,
  ct_at datetime default null,
  primary key (id)
);

-- ----------------------------
-- Table structure for transaction
-- ----------------------------
--drop table if exists tx;
create table tx (
  id bigint(20) unsigned not null,
  req_id varchar(255) not null,
  body json not null,
  st varchar(2) not null,
  ct_at datetime not null,
  up_at datetime not null,
  primary key (id),
  unique key uk_req_id (req_id)
);

-- ----------------------------
-- Table structure for account operation
-- ----------------------------
--drop table if exists acct_op;
create table acct_op (
  id bigint(20) unsigned not null,
  acct_no bigint(20) unsigned not null,
  amt_dt decimal(19,2) unsigned not null,
  decr_incr char(1) not null,
  tx_id bigint(20) unsigned not null,
  ct_at datetime not null,
  primary key (id),
  unique key uk_tx_id_acct_no (tx_id, acct_no)
);