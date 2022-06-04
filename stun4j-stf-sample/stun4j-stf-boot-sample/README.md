# Sample使用说明
stun4j-stf-boot-sample是一个spring-boot的maven工程，下文将进行说明。
## 1. 数据初始化
Sample工程是一个简化了的**转账交易场景**，唯一需要初始化的是一系列模拟的交易账户。
### 1.1 Schema导入
DDL位于工程目录`src/test/resources/schema`下，按需导入即可。
这里面既包含了Sample业务场景的表结构、也包含了Stf框架所需的表结构。
### 1.2 Data导入
Data的生成需要运行一个程序，它会通过JDBC生成一些模拟的账户，见`src/main/com/stun4j/stf/sample/boot/utils/mock_data/AccountGen.java`。
## 2. 是否开启`fresh-start`
每次sample运行，可能会生成一些运行数据，如果你希望每次运行都从一个干净的的环境开始，那么你可以在java启动的**命令行参数**中加入`--fresh-start=true`，这样就会在每次sample的启动阶段清除sample的历史运行数据（**不会清除初始化生成的账户数据**）。
## 3. 关于**内置Mock机制**的一些说明
Sample为了演示方便，内置了一些mock机制，主要用来模拟异常，比如模拟`AppService#sendNotification`会超时`3`次，模拟`BizServiceOrphanStep#handle`会产生异常`1`次，这几个**mock计数**都实现为了**分布式计数器**。
具体请看`com.stun4j.stf.sample.boot.utils.mock_data`这个包。
## 4. 演示入口
Sample的演示入口都是一些**rest api**，api的命名、类的命名都直接揭示了演示意图，这块看代码会比文字说明更清晰，其中关键代码也都附上了注释。
入口代码位于`com.stun4j.stf.sample.boot.facade`这个包下。
