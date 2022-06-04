# 补偿式工作流使用说明
## Stf的哲学
**补偿式工作流**是Stf非常有意思的一个特性，它是一种简约的DSL，用以编排业务在异常下的补偿动作。同时，它又不是典型意义上的工作流，因为Stf希望程序的主控权是能够握于开发者手中的，**主控方**在coder（有匠心的工程师），**辅控方**可以是flow（冰冷的低代码）。

更多的介绍，后续还会撰文阐述。

下文将主要结合[stun4j-stf-boot-sample](https://github.com/stun4j/stun4j-stf/tree/main/stun4j-stf-sample/stun4j-stf-boot-sample)工程，对补偿式工作流的使用进行说明。
## 基本语法
来看[sample中的配置片段](https://github.com/stun4j/stun4j-stf/blob/main/stun4j-stf-sample/stun4j-stf-boot-sample/src/main/resources/stfs/bizMultiStep-flow.conf)，如下：
```yml
stfs {
  local-vars { #允许自定义本地变量
    dp = com.stun4j.stf.sample.boot.domain
  }
  actions { #动作定义(亦即,方法定义)
    acceptReq { #动作名,即方法名
      args = [{use-in:{class:${dp}.Req}}] #方法参数 use-in表示入参类型为com.stun4j.stf.sample.boot.domain.Req,dp变量简化了表达
    }
    step1Tx {
      args = [{invoke-on-in:{method:getId, class:Long}}, {invoke-on-in:{method:getReqId, class:String}}] #invoke-on-in表示入参取值会通过施加在入参对象上的反射来获得,method和class是反射的必要元素,其义自现
      #此处要额外说明的是,一些内置类型可以不给出类的全限定名
      #目前Stf支持的主要是java.lang下的一些包装类型,如："Boolean", "Byte", "Character", "Double", "Float", "Integer", "Long", "Short", "String"
    }
    step2Tx {
      args = [{use-in:{class:${dp}.Tx}}]
    }
    endTx {
      args = [{use-in:{class:${dp}.Tx}}]
    }
    sendNotification {
      oid = bizApp #oid是指方法的对象id,在spring容器中就是bean的id,目前还不支持静态方法的调用
      args = [{use-in:{class:String}}]
      timeout = 10s #自定义方法调用的超时时间,目前仅支持'秒'
    }
  }
  forwards { #流程定义(亦即,上下游方法链)
    acceptReq.to = step1Tx #acceptReq的 下一个动作(下游方法) 是step1Tx
    step1Tx.to = step2Tx
    step2Tx.to = endTx
    endTx.to = sendNotification
  }
}
```
再看[另外一个配置](https://github.com/stun4j/stun4j-stf/blob/main/stun4j-stf-sample/stun4j-stf-boot-sample/src/main/resources/stfs/bizOnTop-flow.conf)
```yml
stfs {
  global { #全局定义区块
    oid = entry
  }
  actions {
    handle {
      oid = bizOrphanStep
      args = [{use-in:{class:com.stun4j.stf.sample.boot.domain.Req}}]
      timeout = 15s
    }
  }
  forwards {
    index.to = handle #因为global区块中配置了oid，所以index方法的oid就是entry了，而handle这个action定义了自己的oid，就不会沿袭global中的定义了
  }
}
```
## 配置大于规约
### 文件路径相关
一般只需将工作流相关的配置文件，放置于工程**classpath下的stfs目录**，就会进行加载和读取。当然也可以额外进行指定，参考[此处](https://github.com/stun4j/stun4j-stf/tree/main/stun4j-stf-spring-boot-starter#21-%25E8%25A1%25A5%25E5%2581%25BF%25E5%25BC%258F%25E5%25B7%25A5%25E4%25BD%259C%25E6%25B5%2581-%25E7%259B%25B8%25E5%2585%25B3%25E9%2585%258D%25E7%25BD%25AE)。
### 文件名相关
如果在一个stf-flow配置中，没有为相应的action配置oid，而一个action又必须具备oid，否则是无法调用的，据此，给出oid的定位顺序，如下：
`配置文件名前缀 -> flow配置中'global区块'中定义的oid -> flow配置中'action区块'中定义的oid`
右侧默认继承左侧，可override左侧，具体请看[sample工程相关配置](https://github.com/stun4j/stun4j-stf/tree/main/stun4j-stf-sample/stun4j-stf-boot-sample/src/main/resources/stfs)。

[< 回索引](../README.md)