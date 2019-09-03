# ReliableGolg

## FrameWork

### Druid
* Druid内置提供了用于监控的StatFilter、日志输出的Log系列Filter、防御SQL注入攻击的WallFilter。  
* Druid内置提供一个StatFilter，用于统计监控信息。
```
<bean id="statFilter" class="com.alibaba.druid.filter.stat.StatFilter">
	<!-- 不合并查询sql -->
	<property name="mergeSql" value="false"/>
	<!-- 记录查询时间超过3秒的sql -->
	<property name="slowSqlMillis" value="3000"/>
</bean>
```

* WallFilter用于防御SQL注入攻击。
`<bean id="wallFilter" class="com.alibaba.druid.wall.WallFilter"/>`

* 多数据源配置
```
<!-- 自定义一个类扩展AbstractRoutingDataSource抽象类，该类相当于数据源DataSourcer的路由中介，可以实现在项目运行时根据相应key值切换到对应的数据源DataSource上 -->
<bean id="multipleDataSource" class="com.ck.service.common.mybatis.ds.MultipleDataSource">
    <property name="defaultTargetDataSource" ref="dataSource"/>
    <property name="targetDataSources">
        <map>
            <entry key="game" value-ref="dataSource"/>
            <entry key="auth" value-ref="authDataSource"/>
        </map>
    </property>
</bean>
```


### RabbitMQ
* [ 队列模型 ](http://www.rabbitmq.com/getstarted.html)
> virtual host相当于数据库

#### 简单队列：一个生产者对应一个消费者
`P---Q---C`

* 缺点：
	1. 生产者一一对应消费者
	2. 耦合性高，队列名变更，生产者和消费者需要同时变更

#### 工作队列：一个生产者对应多个消费者
```
       |---C1
P---Q---
       |---C2
```
simple队列是一一对应的，在实际开发中，生产者发送消息是毫不费力的，而消费者一般是要跟业务相结合的，消费者接收到消息之后需要花费时间处理，这时队列就会积压很多消息。

* 轮询分发：消费者1和消费者2处理的数据消息数量是一样的，消费者1都是偶数，消费者2都是奇数。这种方式叫做轮询分发，结果就是不管谁忙谁空闲都不会多给一个消息，任务消息总是你一个我一个。
* 公平分发：消费者1比消费者2处理的消息要多。
* 消息应答：
	* autoAck为true（自动确认模式），表示使用自动确认模式。一旦rabbitmq将消息分发给消费者，消息就会从内存中删除。这种情况下，如果杀死正在执行的消费者进程，就会丢失正在处理的消息。
	* autoAck为false（手动确认模式），如果一个消费者挂掉，就会交付给其他消费者。这种情况下，由消费者告诉rabbitmq消息已经处理完成，rabbitmq再去删除内存中的消息。
* 消息持久化：
	* 如果rabbitmq挂了，我们的消息任然会丢失！
	* 注：我们将程序中 durable = false; 改成 durable = true; 是不可以的。因为我们已经定义了一个叫 airing_simple_queue 的队列，该队列是未持久化的，rabbitmq不允许使用不同的参数重新定义一个已存在的队列。

#### 订阅模式：
```
       |---Q---C1
P---X---
       |---Q---C2
```

1. 一个生产者，多个消费者
2. 每个消费者都有自己的队列
3. 生产者没有直接把消费发送给消费者，而是发送给交换机（转换器），交换机再把消息发送给已绑定到该交换机的队列
4. 每个队列都要绑定到交换机上
5. 生产者发送的消息经过交换机到达队列就能实现一个消息被多个消费者消费
***注：交换机没有存储的能力，在rabbitmq里面只有队列有存储能力。***

#### 路由模式：
```
        |---error,info---Q---C1
P---X---
        |---info---Q---C2
```

#### 主题模式：将路由键和某模式匹配
```
       |---goods.add---Q---C1
P---X---
       |---goods.*---Q---C2
```

#### RabbitMQ的消息确认机制（事务 + confirm）:
生产者将消息发送出去之后，消息到底有没有到达rabbitmq服务器，默认情况下是不知道的。

* 两种方式：
	* AMQP实现了事务机制
	* Confirm模式

* 事务机制：
	* txSelect：用于将当前channel设置成transaction模式
	* txCommit：用于提交事务
	* txRollback：回滚事务

* 生产者端Confirm模式的实现原理：
	* 生产者将信道设置成confirm模式，一旦信道进入confirm模式，所有在该信道上面发布的消息都会被指派一个唯一的id（从1开始），一旦消息被投递到所有匹配的队列之后，broker就会发送一个确认给生产者（包含消息的唯一id），这就使得生产者知道消息已经正确到达目的队列了。如果消息和队列是可持久化的，那么确认消息将在消息写入磁盘后发出，broker回传给生产者的确认消息中deliver-tag域包含了确认消息的序列号，此外broker也可以设置basicAck的multiple域，表示到这个序列号之前的所有消息都已经得到了处理。
	* Confirm模式最大的好处在于它是异步的。


### ReaderInterceptor / WriterInterceptor
Interceptors (implementations of ReaderInterceptor / WriterInterceptor) are executed only if request/response entity is available. In your case this means that only WriterInterceptor is being executed since you're sending entity (an instance of FooObj) to the client from your resource method. If you had a POST method that receives an input from user your ReaderInterceptor would be invoked as well.

In case you need to modify the request even if no entity is present use ContainerRequestFilter / ContainerResponseFilter.

See JAX-RS 2.0 spec for more info.


### Zookeeper客户端Curator
#### Leader选举
在分布式计算中，leader elections是很重要的一个功能，这个选举过程：指派一个进程作为组织者，将任务分发给各节点。在任务开始前，哪个节点都不知道谁是leader（领导者）或者coordinator（协调者）。 当选举算法开始执行后，每个节点最终会得到一个唯一的节点作为任务leader。除此之外，选举还经常会发生在leader意外宕机的情况下，新的leader要被选举出来。

在zookeeper集群中，leader负责写操作，然后通过Zab协议实现follower的同步，leader或者follower都可以处理读操作。

Curator有两种leader选举的policy，分别是LeaderSelector和LeaderLatch。

前者是所有存活的客户端不间断的轮流做Leader。后者是一旦选举出Leader，除非有客户端挂掉重新触发选举，否则不会交出领导权。

#### Zookeeper的节点创建模式：
* PERSISTENT：持久化
* PERSISTENT_SEQUENTIAL：持久化并且带序列号
* EPHEMERAL：临时
* EPHEMERAL_SEQUENTIAL：临时并且带序列号

#### LeaderSelector
* LeaderSelector使用的时候主要涉及下面几个类：
	* LeaderSelector
	* LeaderSelectorListener
	* LeaderSelectorListenerAdapter
	* CancelLeadershipException

* 核心类是LeaderSelector，它的构造函数如下：
	* public LeaderSelector(CuratorFramework client, String mutexPath, LeaderSelectorListener listener)
	* public LeaderSelector(CuratorFramework client, String mutexPath, ThreadFactory threadFactory, Executor executor, LeaderSelectorListener listener)

类似LeaderLatch，LeaderSelector必须通过start方法启动：leaderSelector.start(); 一旦启动，当实例取得领导权时你的listener的takeLeadership()方法被调用。而takeLeadership()方法只有领导权被释放时才返回。 当你不再使用LeaderSelector实例时，应该调用它的close方法。

异常处理LeaderSelectorListener类继承ConnectionStateListener。LeaderSelector必须小心连接状态的改变。如果实例成为leader，它应该响应SUSPENDED或LOST。当SUSPENDED状态出现时，实例必须假定在重新连接成功之前它可能不再是leader了。 如果LOST状态出现，实例不再是leader，takeLeadership方法返回。

***重要：推荐处理方式是当收到SUSPENDED或LOST时抛出CancelLeadershipException异常，这会导致LeaderSelector实例中断并取消执行takeLeadership方法的异常。这非常重要，你必须考虑扩展LeaderSelectorListenerAdapter。***


### log4j
日志记录器（Logger）的行为等级分为：OFF、FATAL、ERROR、WARN、INFO、DEBUG、ALL或者您定义的级别。Log4j建议只使用四个级别，优先级从高到低分别是 ERROR、WARN、INFO、DEBUG。

自定义日志输出颜色：{FATAL=bright red, ERROR=bright red, WARN=bright yellow, INFO=bright green, DEBUG=bright cyan, TRACE=bright black}


### SpringBoot
#### 外置配置文件
* Spring程序会按优先级从下面这些路径来加载application.properties配置文件
    * 当前目录下的/config目录
    * 当前目录
    * classpath里的/config目录
    * classpath根目录
* 因此，要外置配置文件就很简单了，在jar所在目录新建config文件夹，然后放入配置文件，或者直接放在配置文件在jar目录

#### MongoDB多数据源配置
* [ 配置 ](https://blog.csdn.net/linhui258/article/details/80790676)



## DataBase

### SQL优化
```
update
	T_ORDER_ITEM i
inner join T_ORDER o on
	i.ORDER_ID = o.ORDER_ID
inner join T_ORDER_MONEY M on
	i.ITEM_ID = m.ITEM_ID
set
	i.ITEM_STATUS = 10,
	m.ITEM_STATUS = 10
where
	unix_timestamp( now())- unix_timestamp( i.CRT_TIME )<= 10
	and i.ITEM_STATUS = 0;
```
-- 优化后
```
update
	T_ORDER_ITEM i
inner join T_ORDER o on
	i.ORDER_ID = o.ORDER_ID
inner join T_ORDER_MONEY M on
	i.ITEM_ID = m.ITEM_ID
set
	i.ITEM_STATUS = 10,
	m.ITEM_STATUS = 10
where
	i.CRT_TIME <![ CDATA [<=]]> DATE_ADD(
		now(),
		interval - 10 second
	)
	and i.ITEM_STATUS = 0;
```

### 数据库创建索引的依据
1. 表的主键、外键必须有索引；
2. 数据量超过300的表应该有索引；
3. 经常与其他表进行连接的表，在连接字段上应该建立索引；
4. 经常出现在Where子句中的字段，特别是大表的字段，应该建立索引；
5. 索引应该建在选择性高的字段上；
6. 索引应该建在小字段上，对于大的文本字段甚至超长字段，不要建索引；
7. 复合索引的建立需要进行仔细分析；尽量考虑用单字段索引代替：
8. 正确选择复合索引中的主列字段，一般是选择性较好的字段；
9. 复合索引的几个字段是否经常同时以AND方式出现在Where子句中？单字段查询是否极少甚至没有？如果是，则可以建立复合索引；否则考虑单字段索引；
10. 如果复合索引中包含的字段经常单独出现在Where子句中，则分解为多个单字段索引；
11. 如果复合索引所包含的字段超过3个，那么仔细考虑其必要性，考虑减少复合的字段；
12. 如果既有单字段索引，又有这几个字段上的复合索引，一般可以删除复合索引；
13. 频繁进行数据操作的表，不要建立太多的索引；
14. 删除无用的索引，避免对执行计划造成负面影响；

以上是一些普遍的建立索引时的判断依据。一言以蔽之，索引的建立必须慎重，对每个索引的必要性都应该经过仔细分析，要有建立的依据。因为太多的索引与不充分、不正确的索引对性能都毫无益处：在表上建立的每个索引都会增加存储开销，索引对于插入、删除、更新操作也会增加处理上的开销。另外，过多的复合索引，在有单字段索引的情况下，一般都是没有存在价值的；相反，还会降低数据增加删除时的性能，特别是对频繁更新的表来说，负面影响更大。

1. 表的某个字段值得离散度越高，该字段越适合选作索引的关键字。主键字段以及唯一性约束字段适合选作索引的关键字，原因就是这些字段的值非常离散。MySQL 在处理主键约束以及唯一性约束时，考虑周全。数据库用户创建主键约束的同时， MySQL 自动创建主索引（ primary index ），且索引名称为 Primary；数据库用户创建唯一性索引时， MySQL 自动创建唯一性索引（ unique index ），默认情况下，索引名为唯一性索引的字段名。
2. 占用存储空间少的字段更适合选作索引的关键字。例如，与字符串相比，整数字段占用的存储空间较少，因此，较为适合选作索引关键字。
3. 存储空间固定的字段更适合选作索引的关键字。与 text 类型的字段相比， char 类型的字段较为适合选作索引关键字。
4. Where 子句中经常使用的字段应该创建索引，分组字段或者排序字段应该创建索引，两个表的连接字段应该创建索引。
5. 更新频繁的字段不适合创建索引，不会出现在 where 子句中的字段不应该创建索引。

* 使用like时会使用索引吗：mysql在使用like查询的时候只有不以%开头的时候，才会使用到索引。

### redis配置
\# TCP listen() backlog.  
\#  
\# In high requests-per-second environments you need an high backlog in order  
\# to avoid slow clients connections issues. Note that the Linux kernel  
\# will silently truncate it to the value of /proc/sys/net/core/somaxconn so  
\# make sure to raise both the value of somaxconn and tcp_max_syn_backlog  
\# in order to get the desired effect.  
tcp-backlog 511

在高并发的环境下，你需要把这个值调高以避免客户端连接缓慢的问题。Linux 内核会一声不响的把这个值缩小成 /proc/sys/net/core/somaxconn 对应的值，所以你要修改这两个值才能达到你的预期。
系统对于特定端口TCP连接采用了backlog队列缓存，默认长度为511，通过tcp-backlog设定，如果redis用于高并发为了防止缓慢连接占用，可适当调大，linux系统默认为128。

\# MAXMEMORY POLICY: how Redis will select what to remove when maxmemory  
\# is reached. You can select among five behaviors:  
\#  
\# volatile-lru -> remove the key with an expire set using an LRU algorithm  
\# allkeys-lru -> remove any key according to the LRU algorithm  
\# volatile-random -> remove a random key with an expire set  
\# allkeys-random -> remove a random key, any key  
\# volatile-ttl -> remove the key with the nearest expire time (minor TTL)  
\# noeviction -> don't expire at all, just return an error on write operations  
\#  
\# Note: with any of the above policies, Redis will return an error on write  
\#       operations, when there are no suitable keys for eviction.  
\#  
\#       At the date of writing these commands are: set setnx setex append  
\#       incr decr rpush lpush rpushx lpushx linsert lset rpoplpush sadd  
\#       sinter sinterstore sunion sunionstore sdiff sdiffstore zadd zincrby  
\#       zunionstore zinterstore hset hsetnx hmset hincrby incrby decrby  
\#       getset mset msetnx exec sort  
\#  
\# The default is:  
\#  
\# maxmemory-policy noeviction

* 内存不足时的数据清除策略，你有5个选择。
	1. volatile-lru：对“过期集合”中的数据采取LRU（近期最少使用）算法。如果对key使用“expire”指令指定了过期时间，那么此key将会被添加到“过期集合”中。将已经过期/LRU的数据优先移除。如果“过期集合”中全部移除仍不能满足内存需求，将OOM。
	2. allkeys-lru：对所有的数据，采用LRU算法。
	3. volatile-random：对“过期集合”中的数据采取“随即选取”算法，并移除选中的K-V，直到“内存足够”为止。如果如果“过期集合”中全部移除全部移除仍不能满足，将OOM。
	4. allkeys-random：对所有的数据，采取“随机选取”算法，并移除选中的K-V，直到“内存足够”为止。
	5. volatile-ttl：对“过期集合”中的数据采取TTL算法（最小存活时间），移除即将过期的数据。
	6. noeviction：不做任何干扰操作，直接返回OOM异常。

### MySQL
* 使用select…for update会把数据给锁住，不过我们需要注意一些锁的级别，MySQL InnoDB默认行级锁。行级锁都是基于索引的，如果一条SQL语句用不到索引是不会使用行级锁的，会使用表级锁把整张表锁住，这点需要注意。
* 显示mysql中的所有进程：mysql -uroot -pYxqm2015_ -e 'show full processlist;'
* 登录mycat管理后台：mysql -u crc_auth -p -h 127.0.0.1 -P 19066
* 显示mycat当前processors的处理情况：show @@processor;
* 显示mycat记录的慢查询：show @@sql.slow;
* 显示mysql当前的状态：mysqladmin -uroot -pYxqm2015_ status
* mysql查询表占用空间大小：
```
select
	concat(round(sum(DATA_LENGTH / 1024 / 1024), 2), 'MB') as data
from
	information_schema.TABLES
where
	table_schema = 'crc_auth'
	and table_name = 't_device_record';
```

### MongoDB
* 分页查询：skip(3).limit(5)
* in查询：db.collection.find( { "field" : { $in : array } } );
* 查询重复列：db.T_Match_Live_Info.aggregate([{ $group: { _id: { e_id: "$e_id" }, uniqueIds: { $addToSet: "$_id" }, count: { $sum: 1 } }}, { $match: { count: { $gt: 1 } }}])


## Linux

### 查看日志文件：
* 查看日志文件600行至1000行的内容：sed -n "600,1000p" error_gm_push_2018-05-08.log
* 查看日志文件前100行的内容：head -100 error_gm_push_2018-05-08.log

### find命令：
> -type：文件类型 -d -f 
> -size：文件大小 +1G
> -mtime：修改时间 -7

### 查询CPU占用排名前20的进程：
```
ps -aux | sort -rnk 3 | head -20
ps -aux | sort -rnk 3 | head -20 | awk '{print $1":"$2":"$3}' | column -t -s:
```

### 查询内存占用排名前20的进程：
`ps -aux | sort -rnk 4 | head -20`

### 硬链接与软链接
* 硬链接可由命令 link 或 ln 创建，如下是对文件 oldfile 创建硬链接：
```
link oldfile newfile
lk oldfile newfile
```

* 若一个 inode 号对应多个文件名，则称这些文件为硬链接。换言之，硬链接就是同一个文件使用了多个别名。硬链接存在以下几点特性：
	1. 文件有相同的 inode 及 data block；
	2. 只能对已存在的文件进行创建；
	3. 不能交叉文件系统进行硬链接的创建；
	4. 不能对目录进行创建，只可对文件创建；
	5. 删除一个硬链接文件并不影响其他有相同 inode 号的文件。

* 若文件用户数据块中存放的内容是另一文件的路径名的指向，则该文件就是软连接。软链接就是一个普通文件，只是数据块内容有点特殊。软链接有着自己的 inode 号以及用户数据块。因此软链接的创建与使用没有类似硬链接的诸多限制：
	1. 软链接有自己的文件属性及权限等；
	2. 可对不存在的文件或目录创建软链接；
	3. 软链接可交叉文件系统；
	4. 软链接可对文件或目录创建；
	5. 创建软链接时，链接计数 i_nlink 不会增加；
	6. 删除软链接并不影响被指向的文件，但若被指向的原文件被删除，则相关软连接被称为死链接（即 dangling link，若被指向路径文件被重新创建，死链接可恢复为正常的软链接）。

### 查看CPU信息
* 逻辑CPU个数：cat /proc/cpuinfo | grep "processor" | wc -l
* 物理CPU个数：cat /proc/cpuinfo | grep "physical id" | sort | uniq | wc -l
* 每个物理CPU中Core的个数：cat /proc/cpuinfo | grep "cpu cores" | wc -l

### 杂项
* linux查看限制：ulimit -a
* 查看网卡带宽：ethtool eth0
* 查询连接数：netstat -an | grep 3306|wc -l
* 同步网络时间：ntpdate -u ntp.api.bz
* aprops是一个可以帮你“发现”其他命令的命令。这条命令使用之后，会根据你的搜索条件为你列出所有符合选项的命令，同时还会附带一些简短的解释。比如你忽然想知道如何将目录的内容给列出来，这时候你就可以输入下面的命令：[oracle@dev4 ~]$ apropos cpu

### VIM
* 格式化
	1. gg跳转到第一行
	2. shift + v转到可视模式
	3. shift + g全选
	4. 按下神奇的=

### yum
* yum -y install vim-common.x86_64 vim-enhanced.x86_64 vim-minimal.x86_64
* yum -y install iptables-services
* yum -y install wget

## Git
工作目录下的每一个文件都不外乎这两种状态：已跟踪或未跟踪。 已跟踪的文件是指那些被纳入了版本控制的文件，在上一次快照中有它们的记录。在工作一段时间后，它们的状态可能处于未修改，已修改或已放入暂存区。

编辑过某些文件之后，由于自上次提交后你对它们做了修改，Git将它们标记为已修改文件。 我们逐步将这些修改过的文件放入（add）暂存区，然后提交（commit）所有暂存了的修改，如此反复。

1. 克隆仓库：git clone <url> <alias>
2. 查看文件状态：git status（git status --short）
3. 暂存已修改文件：git add
4. 查看已暂存和未暂存的修改
	* 查看文件修改了哪些内容（未暂存的文件与已暂存的文件）：git diff
	* 查看已暂存的将要添加到下次提交里的内容：git diff --cached
5. 提交更新
	* 现在的暂存区域已经准备妥当可以提交了。在此之前，请一定要确认还有什么修改过的或新建的文件还没有 git add 过，否则提交的时候不会记录这些还没暂存起来的变化。 这些修改过的文件只保留在本地磁盘。所以，每次准备提交前，先用 git status 看下，是不是都已暂存起来了，然后再运行提交命令 git commit。
6. 跳过使用暂存区域：git commit -a -m ""（新文件好像不能使用git commit -a）
7. 移除文件
	* 从已跟踪文件清单中移除（确切地说，是从暂存区域移除），并连带从工作目录中删除指定的文件：git rm
	* 如果删除之前修改过并且已经放到暂存区域的话，则必须要用强制删除选项 -f
	* 把文件从 Git 仓库中删除（亦即从暂存区域移除），但仍然希望保留在当前工作目录中：git rm --cached <file>
8. 移动文件
```
$ git mv README.md README
相当于
$ mv README.md README
$ git rm README.md
$ git add README
```

9. 重新提交（）：git commit --amend
10. 取消暂存：git reset HEAD <file>
11. 拉取仓库中有，但本地没有的信息：git fetch <url>
	* 必须注意 git fetch 命令会将数据拉取到你的本地仓库 - 它并不会自动合并或修改你当前的工作。 当准备好时你必须手动将其合并入你的工作。
	* 想取回特定分支的更新，可以指定分支名：`$ git fetch <远程主机名> <分支名>`

如果你有一个分支设置为跟踪一个远程分支（阅读下一节与 Git 分支 了解更多信息），可以使用 git pull 命令来自动的抓取然后合并远程分支到当前分支。 这对你来说可能是一个更简单或更舒服的工作流程；默认情况下，git clone 命令会自动设置本地 master 分支跟踪克隆的远程仓库的 master 分支（或不管是什么名字的默认分支）。 运行 git pull 通常会从最初克隆的服务器上抓取数据并自动尝试合并到当前所在的分支。

12. 推送到远程仓库
当你想分享你的项目时，必须将其推送到上游。 这个命令很简单：git push [remote-name] [branch-name]。 当你想要将 master 分支推送到 origin 服务器时（再次说明，克隆时通常会自动帮你设置好那两个名字），那么运行这个命令就可以将你所做的备份到服务器：$ git push origin master

只有当你有所克隆服务器的写入权限，并且之前没有人推送过时，这条命令才能生效。 当你和其他人在同一时间克隆，他们先推送到上游然后你再推送到上游，你的推送就会毫无疑问地被拒绝。 你必须先将他们的工作拉取下来并将其合并进你的工作后才能推送。 阅读 Git 分支 了解如何推送到远程仓库服务器的详细信息。

* 推送一个新的项目
	1. git remote add origin <项目地址>
	2. git push -u origin master

* 取消与仓库的关联：`git remote remove origin`

13. 分支
Git 仓库中有五个对象：三个 blob 对象（保存着文件快照）、一个树对象（记录着目录结构和 blob 对象索引）以及一个提交对象（包含着指向前述树对象的指针和所有提交信息）。

做些修改后再次提交，那么这次产生的提交对象会包含一个指向上次提交对象（父对象）的指针。

Git 的分支，其实本质上仅仅是指向提交对象的可变指针。 Git 的默认分支名字是 master。 在多次提交操作之后，你其实已经有一个指向最后那个提交对象的 master 分支。 它会在每次的提交操作中自动向前移动。

HEAD在 Git 中表示一个指针，指向当前所在的本地分支（译注：将 HEAD 想象为当前分支的别名）。 在本例中，你仍然在 master 分支上。 因为 git branch 命令仅仅创建 一个新分支，并不会自动切换到新分支中去。

新建一个分支并同时切换到那个分支上，你可以运行一个带有 -b 参数的 git checkout 命令：
```
$ git checkout -b iss53
Switched to a new branch "iss53"
```
拉取远程分支：
git checkout -b 本地分支名 origin/远程分支名

删除远程分支：
git push origin --delete 远程分支名

push新的分支：
git push origin dev:dev

14. git log
	* 查看各个分支当前所指的对象：git log --oneline --decorate
	* 输出提交历史、各个分支的指向以及项目的分支分叉情况：git log --oneline --decorate --graph --all

15. 打标签
```
git tag -a PRD_20180615_1129 -m ''
git push origin PRD_20180615_1129
```

* 删除远程标签
`git push origin :refs/tags/标签名`

16. git stash
    * 查看stash：`git stash list`
    * 新增stash：`git stash save '1.6.3'`
	* 删除stash：`git stash drop stash@{0}`
	* 导出stash：`git stash pop`
	* 用stash创建分支：`git stash branch 1.6.3`
	* 查看stash差异：`git stash show`

17. 回滚
    * 回滚到指定commitID：`git checkout <commitID> <filename>`

18. 使用develope分支覆盖当前分支
`git reset --hard origin/develope`

19. 修改远程仓库地址
`git remote set-url origin [url]`

20. 取消commit
`git reset --mixed 6d13671f8364141529a54ca9dcc408e7462afff4`

21. 缓存输入的用户名和密码：
`git config --global credential.helper wincred`

## JAVA

### ThreadLocal
1. ThreadLocal 不是用于解决共享变量的问题的，也不是为了协调线程同步而存在，而是为了方便每个线程处理自己的状态而引入的一个机制。这点至关重要。
2. 每个Thread内部都有一个ThreadLocal.ThreadLocalMap类型的成员变量，该成员变量用来存储实际的ThreadLocal变量副本。（ThreadLocalMap的key是ThreadLocal实例本身，value是真正需要存储的Object。）
3. ThreadLocal并不是为线程保存对象的副本，它仅仅只起到一个索引的作用。它的主要目的是为每一个线程隔离一个类的实例，这个实例的作用范围仅限于线程内部。

### 如何快速定位JVM中消耗CPU最多的线程
1. 使用top -Hp <pid>来查看进程中所有线程的 CPU 消耗情况
2. 通过 jstack <pid> 的输出查看各个线程栈
	
输出的线程栈信息中的 nid 就是 pid，它是十六进制的，我们将消耗 CPU 最高的线程18250，转成十六进制0x474A，然后从上面的线程栈里找到nid=0x474A的线程，其栈为：
"Busiest Thread" #28 prio=5 os_prio=0 tid=0x00007fb91498d000 nid=0x474a runnable [0x00007fb9065fe000] java.lang.Thread.State: RUNNABLE at Test$2.run(Test.java:18)

### 字符集
* 因为ISO-8859-1编码范围使用了单字节内的所有空间，在支持ISO-8859-1的系统中传输和存储其他任何编码的字节流都不会被抛弃。换言之，把其他任何编码的字节流当作ISO-8859-1编码看待都没有问题。这是个很重要的特性，MySQL数据库默认编码是Latin1就是利用了这个特性。ASCII编码是一个7位的容器，ISO-8859-1编码是一个8位的容器。

## Python

### 安装python3
```
yum -y groupinstall "Development tools"
yum -y install openssl-devel bzip2-devel expat-devel gdbm-devel readline-devel sqlite-devel
./configure --prefix=/usr/local/python3
make
make install

ln -s /usr/local/python3/bin/python3 /usr/bin/python3
ln -s /usr/local/python3/bin/pip3 /usr/bin/pip3
```

### 安装spacy
```
pip3 install -U spacy
python3 -m spacy validate
```


## tomcat
```
<Executor
 name="tomcatThreadPool"
 namePrefix="catalina-exec-"
 maxThreads="500"
 minSpareThreads="30"
 maxIdleTime="60000"
 prestartminSpareThreads = "true"
 maxQueueSize = "100"
/>
```

参数解释：

* maxThreads：最大并发数，默认设置 200，一般建议在 500 ~ 800，根据硬件设施和业务来判断
* minSpareThreads：Tomcat 初始化时创建的线程数，默认设置 25
* maxIdleTime：如果当前线程大于初始化线程，那空闲线程存活的时间，单位毫秒，默认60000=60秒=1分钟。
* prestartminSpareThreads：在 Tomcat 初始化的时候就初始化 minSpareThreads 的参数值，如果不等于 true，minSpareThreads 的值就没啥效果了
* maxQueueSize：最大的等待队列数，超过则拒绝请求

```
<Connector
	executor="tomcatThreadPool"
	port="8100"
	protocol="org.apache.coyote.http11.Http11Nio2Protocol"
	connectionTimeout="60000"
	maxConnections="10000"
	redirectPort="8443"
	enableLookups="false"
	acceptCount="100"
	maxPostSize="10485760"
	maxHttpHeaderSize="8192"
	compression="on"
	disableUploadTimeout="true"
	compressionMinSize="2048"
	acceptorThreadCount="2"
	compressableMimeType="application/json"
	URIEncoding="utf-8"
	processorCache="20000"
	tcpNoDelay="true"
	connectionLinger="5"/>
```
参数解释：

* protocol：Tomcat 8 设置 nio2 更好：org.apache.coyote.http11.Http11Nio2Protocol
* protocol：Tomcat 6 设置 nio 更好：org.apache.coyote.http11.Http11NioProtocol
* protocol：Tomcat 8 设置 APR 性能飞快：org.apache.coyote.http11.Http11AprProtocol 更多详情：《Tomcat 8.5 基于 Apache Portable Runtime（APR）库性能优化》（https://renwole.com/archives/361）
* connectionTimeout：Connector接受一个连接后等待的时间(milliseconds)，默认值是60000。
* maxConnections：这个值表示最多可以有多少个socket连接到tomcat上
* enableLookups：禁用DNS查询
* acceptCount：当tomcat起动的线程数达到最大时，接受排队的请求个数，默认值为100。
* maxPostSize：设置由容器解析的URL参数的最大长度，-1(小于0)为禁用这个属性，默认为2097152(2M) 请注意， FailedRequestFilter 过滤器可以用来拒绝达到了极限值的请求。
* maxHttpHeaderSize：http请求头信息的最大程度，超过此长度的部分不予处理。一般8K。
* compression：是否启用GZIP压缩 on为启用（文本数据压缩）off为不启用，force 压缩所有数据
* disableUploadTimeout：这个标志允许servlet容器使用一个不同的,通常长在数据上传连接超时。 如果不指定,这个属性被设置为true,表示禁用该时间超时。
* compressionMinSize：当超过最小数据大小才进行压缩
* acceptorThreadCount：用于接受连接的线程数量。增加这个值在多CPU的机器上,尽管你永远不会真正需要超过2。 也有很多非维持连接,您可能希望增加这个值。默认值是1。
* compressableMimeType：配置想压缩的数据类型
* URIEncoding：网站一般采用UTF-8作为默认编码。
* processorCache：协议处理程序缓存处理器对象以加快性能。此设置指示缓存这​​些对象的数量。 -1意味着无限制，默认是200。如果不使用Servlet 3.0异步处理，则默认使用与maxThreads设置相同的默认值。如果使用Servlet 3.0异步处理，一个很好的默认设置是使用较大的maxThreads和最大预期并发请求数（同步和异步）。
* tcpNoDelay：如果设置为true,TCP_NO_DELAY选项将被设置在服务器套接字,而在大多数情况下提高性能。这是默认设置为true。
* connectionLinger：此连接器使用的套接字在关闭时将停留的秒数 。默认值是-1禁用套接字延迟。


## 压测配置修改总结

### JVM配置
* JAVA_OPTS='-server -Xms3072M -Xmx3072M -XX:MaxPermSize=1024M -XX:+UseConcMarkSweepGC -XX:+UseParNewGC -XX:+ExplicitGCInvokesConcurrentAndUnloadsClasses -XX:+UseStringCache -XX:CMSFullGCsBeforeCompaction=5 -XX:+UseFastAccessorMethods -XX:InitialCodeCacheSize=128m -XX:ReservedCodeCacheSize=128m -XX:CompileThreshold=200 -XX:+UseCMSCompactAtFullCollection -XX:+UseThreadPriorities -Xverify:none -XX:-UseGCOverheadLimit -verbose:gc -Xloggc:/app/gm_provider1/tc/logs/gc.log -XX:+PrintGCDateStamps -XX:+PrintGCDetails'
* JAVA_OPTS="$JAVA_OPTS -Djava.library.path=/app/gm_provider1/tc/lib -agentpath:/app/gm_provider1/jprofiler10/bin/linux-x64/libjprofilerti.so=port=8849"

### Druid数据库连接池配置
* db.auth.maxPoolSize=100
* db.auth.initialPoolSize=50
* db.maxWait=30000
* db.timeBetweenEvictionRunsMillis=60000
* db.minEvictableIdleTimeMillis=1800000
* db.maxPoolPreparedStatementPerConnectionSize=500
* db.addr=jdbc:mysql://192.168.77.151:13306/crc_game?useUnicode=true&characterEncoding=UTF-8
* db.user=crc_game
* db.password=by2015crc_game

<!-- 初始化时建立物理连接的个数 -->
<property name="initialSize" value="${db.initialPoolSize}" />
<!-- 最小连接池数量 -->
<property name="minIdle" value="${db.initialPoolSize}" />
<!-- 最大连接池数量 -->
<property name="maxActive" value="${db.maxPoolSize}" />
<!-- 获取连接时最大等待时间，单位毫秒 -->
<property name="maxWait" value="${db.maxWait}" />
<!-- 1)Destroy线程会检测连接的间隔时间，如果连接空闲时间大于等于minEvictableIdleTimeMillis则关闭物理连接 2)testWhileIdle的判断依据 -->
<property name="timeBetweenEvictionRunsMillis" value="${db.timeBetweenEvictionRunsMillis}" />
<!-- 连接保持空闲而不被驱逐的最长时间 -->
<property name="minEvictableIdleTimeMillis" value="${db.minEvictableIdleTimeMillis}" />
<!-- 用来检测连接是否有效的sql，如果validationQuery为null，testOnBorrow、testOnReturn、testWhileIdle都不会其作用
	 不需要配置validationQuery，如果不配置的情况下会走ping命令，性能更高 -->
<!-- <property name="validationQuery" value="SELECT 1" /> -->
<!-- 单位：秒，检测连接是否有效的超时时间 -->
<!-- <property name="validationQueryTimeout" value="1" /> -->
<!-- 建议配置为true，不影响性能，并且保证安全性。申请连接的时候检测，如果空闲时间大于timeBetweenEvictionRunsMillis，执行validationQuery检测连接是否有效 -->
<!-- <property name="testWhileIdle" value="true" /> -->
<!-- 申请连接时执行validationQuery检测连接是否有效，做了这个配置会降低性能 -->
<!-- <property name="testOnBorrow" value="false" /> -->
<!-- 归还连接时执行validationQuery检测连接是否有效，做了这个配置会降低性能 -->
<!-- <property name="testOnReturn" value="false" /> -->
<!-- 是否缓存preparedStatement，也就是PSCache。PSCache对支持游标的数据库性能提升巨大，比如说oracle。在mysql下建议关闭 -->
<property name="poolPreparedStatements" value="false" />
<!-- 每个连接上PSCache的大小 -->
<!-- <property name="maxPoolPreparedStatementPerConnectionSize" value="${db.maxPoolPreparedStatementPerConnectionSize}" /> -->
<!-- 超过时间限制是否回收 -->
<property name="removeAbandoned" value="false" />
<!-- 超时时间；单位为秒 -->
<property name="removeAbandonedTimeout" value="1200" />
<!-- 关闭abanded连接时输出错误日志 -->
<property name="logAbandoned" value="true" />
<!-- 数据库连接地址 -->
<property name="url" value="${db.addr}" />
<!-- 数据库登录用户名 -->
<property name="username" value="${db.user}" />
<!-- 数据库登录密码 -->
<property name="password" value="${db.password}" />

### MongoDB连接配置
game.mongodb.dbname=ck_game
game.mongodb.username=
game.mongodb.password=
mongodb.replicaSet=192.168.88.101:27017
\# 每个host允许链接的最大链接数
mongodb.connectionsPerHost=100
\# 线程队列数，它以上面connectionsPerHost值相乘的结果就是线程队列最大值
mongodb.threadsAllowedToBlockForConnectionMultiplier=10
\# 链接超时的毫秒数
mongodb.connectTimeout=10000
\# 一个线程等待链接可用的最大等待毫秒数
mongodb.maxWaitTime=5000
\# 链接不能建立时是否重试
mongodb.autoConnectRetry=false
\# 该标志用于控制socket保持活动的功能
mongodb.socketKeepAlive=false
\# socket读写超时时间，推荐为不超时，即0
mongodb.socketTimeout=0
\# 为true表示读写分离
mongodb.slaveok=false
mongodb.writeNumber=10
mongodb.writeTimeout=0
mongodb.writeFsync=true

### MyCat性能调优
* 见MyCat性能调优指南


## 数据结构

### 二叉排序树（Binary Sort Tree）
* 二叉排序树又称二叉查找树。它或者是一棵空树，或者是具有下列性质的二叉树：
1. 若左子树不空，则左子树上所有结点的值均小于它的根结点的值；
2. 若右子树不空，则右子树上所有结点的值均大于它的根结点的值；
3. 左、右子树也分别为二叉排序树；

### 平衡二叉树
* 定义：它或者是一颗空树，或者具有以下性质的二叉树：它的左子树和右子树的深度之差(平衡因子)的绝对值不超过1，且它的左子树和右子树都是一颗平衡二叉树。


## Nginx
* 反向代理websocket服务：
```
upstream chat_server {
    server 192.168.77.151:9999;
}

location /chat/ws {
    proxy_buffering off;
    proxy_http_version 1.1;
	proxy_read_timeout 1800s;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_pass http://chat_server;
}
```