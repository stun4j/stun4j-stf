# stun4j-stf-spring-boot-starter使用说明

## 1. 快速开始
### 使用`@EnableStf`注解
在使用`@SpringBootApplication`注解的地方，使用`@EnableStf`注解，如下：
```java
@SpringBootApplication
@EnableStf
public class SampleApplication {
  public static void main(String[] args) {
    SpringApplication.run(SampleApplication.class, args);
  }
}
```
### 通过**自动装配**的`StfTxnOps`，在用户（业务）代码中使用***柔性事务***
```java
@Service
public class BizService {
  @Autowired
  private StfTxnOps txnOps;
  //略...
  public OutObj step(ParamObj param) {
    //略...
    return txnOps.executeWithFinalResult(() -> new OutObj(param), out -> st -> {
      //(如需修改out对象，尽可能在当前闭包内完成)，略...
      //执行本地事务操作，略...
    });
  }
}
```
### 通过**自动装配**的`StfExecutorService`，在用户（业务）代码中使用***可靠的异步任务链***
```java
@Service
public class AppService {
  @Autowired
  private StfExecutorService stfExec;
  @Autowired
  private BizService svc;
  //略...
  public void startBiz(ParamObj startParam) {
    CompletableFuture.supplyAsync(() -> svc.step1(startParam), stfExec).thenApplyAsync(svc::step2, stfExec);
    
    //等价于如下代码：
    //stfExec.execute(() -> {
      //Step2Param step2Param = svc.step1(startParam);
      //stfExec.execute(() -> svc.step2(step2Param));
    //});    
  }
}
```
### 通过**自动装配**的`StfDelayQueue`，在用户（业务）代码中使用***延时队列***
```java
@Service
public class BizService {
  @Autowired
  private StfDelayQueue queue;
  
  //spring容器中taskObjId就是beanId
  //普通的POJO容器中，需以KV形式自行注册taskObjId和taskObj
  public Long delayRun(String taskObjId, String taskMethodName, int timeoutSeconds, int delaySeconds, Object... taskParams) {
    Long taskNo = queue.offer(taskObjId, taskMethodName, timeoutSeconds, delaySeconds, taskParams);
    return taskNo;
  }
}
```
## 2. `application.yml`配置详解
### 2.1 ***补偿式工作流***相关配置
```yml
#略...
stun4j:
  stf: #可选(如不出现，则采用默认配置)
    conf-root-path: <如file:/apps/foo/stfs> #flow配置所处的根路径(可选,默认值: classpath:stfs)
    conf-full-load-order: <如[bizFoo-flow, bizBar-flow]> #flow配置的文件名和 左右或前后 顺序(可选,但一般都需明确指定,除非你不关心配置的父子关系,比如,对于具有相同oid的config-block,右侧/后面 文件会覆盖 左侧/前面 文件的定义)
    conf-exclude-filenames: <如[excludeFoo-flow, excludeBar-flow]> #需被排除、不会被加载的flow配置文件名(可选,如不指定,表示都需要加载)
#略...
```
更多关于**补偿式工作流**的详细介绍可参考[这篇]()
### 2.2 ***柔性事务***相关配置
### 2.3 ***异步任务链线程池***相关配置
### 2.4 ***延时队列***相关配置
### 2.5 ***任务微内核引擎***相关配置
#### 2.5.1 ***任务监控***相关配置
### 2.6 ***运行模式***相关配置
### 2.7 ***数据源***相关配置
### 2.8 ***GUID***相关配置
Stf及其集群的正确工作需要确保内置的GUID正确工作，开箱即用的GUID策略是基于本地IP的，关于这块的详细介绍和讨论可从这个[教程](https://github.com/stun4j/stun4j-guid/blob/master/stun4j-guid-spring-boot-starter/README.md#2-applicationyml%E9%85%8D%E7%BD%AE%E8%AF%A6%E8%A7%A3)入手

[< 回索引](../README.md)