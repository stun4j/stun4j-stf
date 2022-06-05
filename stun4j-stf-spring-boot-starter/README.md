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
### 通过**自动装配**的`StfTxnOps`，在业务代码中使用 **柔性事务**
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
### 通过**自动装配**的`StfExecutorService`，在业务代码中使用 **可靠的异步任务链**
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
### 通过**自动装配**的`StfDelayQueue`，在业务代码中使用 **延时队列**
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
### 2.1 **补偿式工作流** 相关配置
```yml
#略...
stun4j:
  stf: #可选(如不出现,则采用默认配置)
    conf-root-path: <如file:/apps/foo/stfs> #flow配置所处的根路径(可选,默认值: classpath:stfs)
    conf-full-load-order: <如[bizFoo-flow, bizBar-flow]> #flow配置的文件名和 左右或前后 顺序(可选,但一般都需要明确指定,除非你不关心配置间的父子关系,比如,对于具有相同oid的config-block,右侧/后面 文件会覆盖 左侧/前面 文件的定义)
    conf-exclude-filenames: <如[excludeFoo-flow, excludeBar-flow]> #需被排除、不会被加载的flow配置文件名(可选,如不指定,表示均需加载)
#略...
```
更多关于**补偿式工作流**的介绍可参考[这篇](../stun4j-stf-core#%E8%A1%A5%E5%81%BF%E5%BC%8F%E5%B7%A5%E4%BD%9C%E6%B5%81%E4%BD%BF%E7%94%A8%E8%AF%B4%E6%98%8E)
### 2.2 **柔性事务** 相关配置
```yml
#略...
stun4j:
  stf: #可选(如不出现,则采用默认配置)
    transaction: #事务全局属性的相关配置(可选,如不出现,则采用默认配置)
      isolation-level: default #隔离级别(可选,默认值:default,其它取值read-committed,repeatable-read)
      propagation: required #传播机制(可选,默认值:required,其它取值mandatory,nested,never,not-supported,requires-new,supports)
#略...
```
### 2.3 **异步任务链线程池** 相关配置
```yml
#略...
stun4j:
  stf: #可选(如不出现,则采用默认配置)
    default-executor: #开放供业务使用的、默认的、全局任务执行线程池(可选,如不出现,则采用默认配置)
      allow-core-thread-time-out: true #是否允许核心线程超时(可选,默认值:true 其它取值false)
      task-queue-size: 1024 #线程队列size(可选,默认值:1024)
      thread-keep-alive-time-seconds: 60 #线程的保活秒数(可选,默认值:60)
      thread-reject-policy: back-pressure #线程的拒绝策略(可选,默认值:back-pressure,其它取值drop-with-ex-throw,silent-drop,silent-drop-oldest)
#略...
```
### 2.4 **延时队列** 相关配置
```yml
#略...
stun4j:
  stf: #可选(如不出现,则采用默认配置)
    delay-queue: #可选,如不出现,则采用默认配置
      enabled: true #是否启用delay-queue(可选,默认值:true 其它取值false)
#略...
```
### 2.5 **任务引擎** 相关配置
#### 2.5.1 **核心协调者** 相关配置
```yml
#略...
stun4j:
  stf: #可选(如不出现,则采用默认配置)
    job: #可选(如不出现,则采用默认配置)
      manager: #可选(如不出现,则采用默认配置)
        scan-freq-secs: 3 #扫描频率秒数(可选,默认值:3)
        handle-batch-size: 20 #批处理任务数量(可选,默认值:20)
        batch-multiplying-factor: 16 #影响批处理吞吐的一个可调因子(可选,默认值:16)
#略...
```
#### 2.5.2 **队列充填器** 相关配置
```yml
#略...
stun4j:
  stf: #可选(如不出现,则采用默认配置)
    job: #可选(如不出现,则采用默认配置)
      loader: #可选(如不出现,则采用默认配置)
        load-size: 300 #扫描加载任务数最大值的一个因子(可选,默认值:300)
        scan-freq-secs: 3 #扫描频率秒数(可选,默认值:3)
#略...
```
### 2.6 **监控** 相关配置
```yml
#略...
stun4j:
  stf: #可选(如不出现,则采用默认配置)
    monitor: #可选(如不出现,则采用默认配置)
      vm-res-check-enabled: true #是否开启资源检查(可选,默认值:true)
      consider-jvm-mem: false #资源检查是否考察jvm内存(可选,默认值:false)
      consider-sys-load: false #资源检查是否考察系统负载(可选,默认值:false)
      jvm-cpu: #可选(如不出现,则采用默认配置)
        high-factor: 0.65f #高水位阈值(可选,默认值:0.65f,即65%)
      jvm-mem: #可选(如不出现,则采用默认配置)
        high-factor: 0.85f #高水位阈值(可选,默认值:0.85f,即85%)
        include-non-heap: false #jvm内存检查是否考察non-heap(可选,默认值:false)
      sys-load: #可选(如不出现,则采用默认配置)
        high-factor: 0.8f #高水位阈值(可选,默认值:0.8f,即80%)
#略...
```
### 2.7 **运行模式** 相关配置
```yml
#略...
stun4j:
  stf: #可选(如不出现,则采用默认配置)
    run-mode: default #运行模式(可选,默认值:default,其它取值client)
#略...
```
### 2.8 **数据源** 相关配置
```yml
#略...
stun4j:
  stf: #可选(如不出现,则采用默认配置)
    datasource-bean-name: dataSource #数据源在spring容器中的bean名称(可选,默认值:dataSource)
#略...
```
### 2.9 **GUID** 相关配置
Stf及其集群的正确工作必须确保内置的GUID也是正确工作的，开箱即用的GUID策略是基于**本地IP**的，关于这块的详细介绍和讨论可从这个[教程](../../stun4j/stun4j-guid/blob/master/stun4j-guid-spring-boot-starter/README.md#2-applicationyml%E9%85%8D%E7%BD%AE%E8%AF%A6%E8%A7%A3)入手

## 3. 兼容性
支持JDK1.8+，Spring Boot 2.4.0+

[< 回索引](../README.md)