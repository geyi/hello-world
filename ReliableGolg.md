# ReliableGolg

## 硬链接与软链接
* 硬链接可由命令 link 或 ln 创建，如下是对文件 oldfile 创建硬链接：
	* link oldfile newfile
	* lk oldfile newfile

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


## Druid
Druid内置提供了用于监控的StatFilter、日志输出的Log系列Filter、防御SQL注入攻击的WallFilter。  
Druid内置提供一个StatFilter，用于统计监控信息。  
```
<bean id="statFilter" class="com.alibaba.druid.filter.stat.StatFilter">
	<!-- 不合并查询sql -->
	<property name="mergeSql" value="false"/>
	<!-- 记录查询时间超过3秒的sql -->
	<property name="slowSqlMillis" value="3000"/>
</bean>
```

WallFilter用于防御SQL注入攻击。  
`<bean id="wallFilter" class="com.alibaba.druid.wall.WallFilter"/>`
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

## RabbitMQ
* [ 队列模型 ](http://www.rabbitmq.com/getstarted.html)  
***virtual host相当于数据库***


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

* simple队列是一一对应的，在实际开发中，生产者发送消息是毫不费力的，而消费者一般是要跟业务相结合的，消费者接收到消息之后需要花费时间处理，这时队列就会积压很多消息。
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


## ReaderInterceptor / WriterInterceptor
Interceptors (implementations of ReaderInterceptor / WriterInterceptor) are executed only if request/response entity is available. In your case this means that only WriterInterceptor is being executed since you're sending entity (an instance of FooObj) to the client from your resource method. If you had a POST method that receives an input from user your ReaderInterceptor would be invoked as well.

In case you need to modify the request even if no entity is present use ContainerRequestFilter / ContainerResponseFilter.

See JAX-RS 2.0 spec for more info.


## Zookeeper客户端Curator
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


## SQL优化
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

## log4j
日志记录器（Logger）的行为等级分为：OFF、FATAL、ERROR、WARN、INFO、DEBUG、ALL或者您定义的级别。Log4j建议只使用四个级别，优先级从高到低分别是 ERROR、WARN、INFO、DEBUG。

自定义日志输出颜色：{FATAL=bright red, ERROR=bright red, WARN=bright yellow, INFO=bright green, DEBUG=bright cyan, TRACE=bright black}


## Linux
* 查看日志文件600行至1000行的内容：sed -n "600,1000p" error_gm_push_2018-05-08.log
* 查看日志文件前100行的内容：head -100 error_gm_push_2018-05-08.log

#### find命令：
> -type：文件类型 -d -f 
> -size：文件大小 +1G
> -mtime：修改时间 -7

#### 查询CPU占用排名前20的进程：
```
ps -aux | sort -rnk 3 | head -20
ps -aux | sort -rnk 3 | head -20 | awk '{print $1":"$2":"$3}' | column -t -s:
```

* 查询内存占用排名前20的进程：
`ps -aux | sort -rnk 4 | head -20`


## git command
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

14. git log
	* 查看各个分支当前所指的对象：git log --oneline --decorate
	* 输出提交历史、各个分支的指向以及项目的分支分叉情况：git log --oneline --decorate --graph --all
15. 打标签
```
git tag -a PRD_20180615_1129 -m ''
git push origin PRD_20180615_1129
```


## redis配置
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


## 如何快速定位JVM中消耗CPU最多的线程
1. 使用top -Hp <pid>来查看进程中所有线程的 CPU 消耗情况
2. 通过 jstack <pid> 的输出查看各个线程栈
	
输出的线程栈信息中的 nid 就是 pid，它是十六进制的，我们将消耗 CPU 最高的线程18250，转成十六进制0x474A，然后从上面的线程栈里找到nid=0x474A的线程，其栈为：
"Busiest Thread" #28 prio=5 os_prio=0 tid=0x00007fb91498d000 nid=0x474a runnable [0x00007fb9065fe000] java.lang.Thread.State: RUNNABLE at Test$2.run(Test.java:18)

## JAVA

### ThreadLocal
1. ThreadLocal 不是用于解决共享变量的问题的，也不是为了协调线程同步而存在，而是为了方便每个线程处理自己的状态而引入的一个机制。这点至关重要。
2. 每个Thread内部都有一个ThreadLocal.ThreadLocalMap类型的成员变量，该成员变量用来存储实际的ThreadLocal变量副本。（ThreadLocalMap的key是ThreadLocal实例本身，value是真正需要存储的Object。）
3. ThreadLocal并不是为线程保存对象的副本，它仅仅只起到一个索引的作用。它的主要目的是为每一个线程隔离一个类的实例，这个实例的作用范围仅限于线程内部。


## MySQL
使用select…for update会把数据给锁住，不过我们需要注意一些锁的级别，MySQL InnoDB默认行级锁。行级锁都是基于索引的，如果一条SQL语句用不到索引是不会使用行级锁的，会使用表级锁把整张表锁住，这点需要注意。


## 安装python3
```
yum -y groupinstall "Development tools"
yum -y install openssl-devel bzip2-devel expat-devel gdbm-devel readline-devel sqlite-devel
./configure --prefix=/usr/local/python3
make
make install

ln -s /usr/local/python3/bin/python3 /usr/bin/python3
ln -s /usr/local/python3/bin/pip3 /usr/bin/pip3
```


## 安装spacy
```
pip3 install -U spacy
python3 -m spacy validate
```

## 数据库创建索引的依据
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


## Nginx配置
* 反向代理websocket服务：
```
upstream chat_server {
    server 192.168.77.151:9999;
}

location /chat/ws {
    proxy_buffering off;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_pass http://chat_server;
}
```

