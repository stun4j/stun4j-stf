# Stun4J Stf
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

### 柔性事务、延时队列、可编排、自适应批处理、异步任务链、分布式、极简设计、高可用、高性能、易于使用

## 功能特性
* 柔性事务，基于BASE理论，以本地事务为基石，无惧任何失败（超时、宕机、异常等等）
* 延时队列，支持秒级时间精度，高吞吐，良好的水平伸缩性
* 补偿式迷你工作流，简约的DSL编排，主控权牢牢握于开发者手中
* 自适应、响应式的批处理机制，天然背压，内置监控
* 良好的梯度重试机制
* 支持具备血缘关系的异步任务链
* 极简设计，分布式仅依赖DB，无注册中心强需求、高可用、高可靠，无状态、无Leader
* 开箱支持MySQL、PostgreSQL、Oracle三大主流关系型数据库
* 制品为袖珍型jar包，易于使用集成，亦可独立部署

## 核心图解
### 基本原理
![fundamental](https://user-images.githubusercontent.com/24976735/170415176-cb1b92c6-a4e9-414d-9ac0-96e0a73d65b6.png)
### 水平伸缩和高可用
![sha](https://user-images.githubusercontent.com/24976735/170385763-0118e324-4f6d-47da-968d-29fbea7f79fa.png)
### 补偿式工作流
```yml
stfs {
  local-vars {
    dp = com.stun4j.stf.sample.boot.domain
  }
  actions {
    acceptReq {
      args = [{use-in:{class:${dp}.Req}}]
    }
    step1Tx {
      args = [{invoke-on-in:{method:getId, class:Long}}, {invoke-on-in:{method:getReqId, class:String}}]
    }
    step2Tx {
      args = [{use-in:{class:${dp}.Tx}}]
    }
    endTx {
      args = [{use-in:{class:${dp}.Tx}}]
    }
    sendNotification {
      oid = bizApp
      args = [{use-in:{class:String}}]
      timeout = 10s
    }
  }
  forwards {
    acceptReq.to = step1Tx
    step1Tx.to = step2Tx
    step2Tx.to = endTx
    endTx.to = sendNotification
  }
}
```

## 参与
* 报告bugs、给到建议反馈，请提交一个[issue](https://github.com/stun4j/stun4j-stf/issues/new)
* 参与贡献 改进或新功能，请提交pull request并创建一个[issue](https://github.com/stun4j/stun4j-stf/issues/new)以便讨论与进度追踪
* 不吝赐 :star2:

## 感谢
*  异步任务链使用了transmittable-thread-local这个[项目](https://github.com/alibaba/transmittable-thread-local)

## 开源许可协议
本项目采用 **Apache Software License, Version 2.0**