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
### 通过**自动装配**的`StfTxnOps`，在用户（业务）代码中使用**柔性事务**
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
### 通过**自动装配**的`StfExecutorService`，在用户（业务）代码中使用**异步任务链**
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
### 通过**自动装配**的`StfDelayQueue`，在用户（业务）代码中使用**延时队列**
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
[< 回索引](../README.md)