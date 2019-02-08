# 业务逻辑

## 登录
* 用户登录时将用户信息保存到tair缓存中。

## 订单

### 目前使用到的订单状态：
* 订单确认中
* 待开奖
* 结算中
* 订单取消（已退款）
* 订单取消（退款中）
```
{
  "0": "订单确认中",
  "1": "已支付",
  "2": "已开奖",
  "10": "未开奖",
  "100": "结算中",
  "110": "赢",
  "120": "走盘",
  "130": "输半",
  "140": "赢半",
  "210": "已退款",
  "-100": "输",
  "-5": "订单取消(退款中)",
  "-10": "订单取消(已退款)"
}
```

### 订单派奖：
#### 修改订单状态，使其能重新被派奖
```
-- T_ORDER_ITEM
 update
	T_ORDER_ITEM
set
	ITEM_STATUS = 10,
	UPDATE_TIME = now()
where
	ITEM_ID = '9eb7ca2578ab4e848f9fb31d557cf009';


-- T_ORDER_MONEY
 update
	T_ORDER_MONEY
set
	ITEM_STATUS = 10,
	UPDATE_TIME = now()
where
	ITEM_ID = '9eb7ca2578ab4e848f9fb31d557cf009';
-- select * from t_order_money WHERE ITEM_ID='9eb7ca2578ab4e848f9fb31d557cf009';


-- T_ITEM_CONTENT
 update
	T_ITEM_CONTENT
set
	BALANCE_STATUS = 0,
	BALANCE_TIME = now(),
	CONTENT_STATUS = 10
where
	ITEM_ID = '9eb7ca2578ab4e848f9fb31d557cf009';
```
#### 查询t_local_event查询主客队
`select * from crc_main.t_local_event where event_id = '11491366';`
#### 获取需要算奖的订单
```
select
	i.ITEM_ID itemId,
	I.ORDER_ID orderId,
	I.USER_ID userId,
	I.ITEM_STATUS itemStatus,
	I.ITEM_MONEY totalItemMoney,
	C.MATCH_ID matchId,
	C.MATCH_ODDS odds,
	C.CONTENT_ID contentId,
	C.OPT_NAME optName,
	c.PLAY_ID marketId,
	c.CLIENT_PROPERTIES clientProperties,
	c.BUY_CODE outcomeId,
	M.PREDICE_PROFIT prediceProfit,
	(
		M.ITEM_MONEY - I.USERD_PREPAID_MONEY - I.REMAIN_PREPAID_MONEY
	) itemMoney,
	M.ID moneyId,
	M.ACCT_TYPE acctType,
	M.COST_TYPE costType
from
	T_ORDER_ITEM I
inner join T_ITEM_CONTENT C on
	I.ITEM_ID = C.ITEM_ID
inner join T_ORDER_MONEY M on
	I.ITEM_ID = M.ITEM_ID
inner join CRC_MAIN.T_local_MARKET T on
	C.AUTO_ID = T.AUTO_ID
inner join CRC_MAIN.t_local_market_settlement S on
	S.EVENT_ID = T.EVENT_ID
	and S.MARKET_ID = T.MARKET_ID
	and S.SPECIFIERS = T.SPECIFIERS
	and S.RESULT = 1
	and S.outcome_id = C.buy_code
where
	C.MATCH_ID = 11491366
	and C.PLAY_ID = 90
	and C.BUY_CODE = 3
	and S.CERTAINTY = 2
	and I.ITEM_STATUS = 10
	and C.CONTENT_STATUS = 10
order by
	M.ITEM_MONEY desc limit 500;
```

### 虚拟余额=真实余额-风险值

### 订单状态变更过程：
* 下单时订单状态为220（滚球受理成功），230（赛前受理成功）
* provider接收数据源推送的赛事消息，如果3秒内赔率没有发生改变会把订单状态改为0（订单确认中），否则不做处理。
* orderItemStatusSchedule定时扫描3秒前状态为220或230的订单，将其状态改为-5（退款中）。
* BatchUpdateOrderPlugin定时扫描10秒前状态为0的订单，将其状态改为10（未开奖）。并处理状态为-5（退款中）的订单。

### 下注玩法：

|BUY_CODE |AUTO_ID  |OPT_NAME |CLIENT_PROPERTIES                                                                                            |
|---------|---------|---------|-------------------------------------------------------------------------------------------------------------|
|1        |37973077 |Lay      |{"marketName":"test_x","inning":"","codeName":"Lay","returnMoney":"5,000","overs":"","runs":"","wickets":""} |
|2        |37973077 |Back     |{"marketName":"test_x","inning":"","codeName":"Back","returnMoney":"30","overs":"","runs":"","wickets":""}   |

* **BUY_CODE：1代表主队赢，2代表客队赢，3not，4yes**
* **OPT_NAME：Lay代表输，Back代表赢**
* **当BUY_CODE为1时，说明用户买的是主队赢。又因为OPT_NAME为Lay，表示输，所以CLIENT_PROPERTIES的marketName应该为客队队名。与crc_main数据库中t_local_market表中的market_description字段相同**

|event_id |market_id |specifiers       |market_description |
|---------|----------|-----------------|-------------------|
|11490226 |4         |site_market_id=1 |test_x             |

### 字段含义
* item_money				下注金额
* prize_money				返奖金额
* PREPAID_MONEY			本单产生的预支金额
* remain_prepaid_money	本单使用了其他单预支金额
> 预支金额只能使用其它订单产生的预支金额

### 风险值
* 1代表主队赢，2代表客队赢
1. 如果用户买1，即主队赢
2. 产生了两条风险值记录（一个用户一场比赛）1、2。当主队赢时，betwon为预计盈利；当主队输时，betwon为负的下注金额。
3. 之后用户继续下单，继续买1（主队赢）。当主队赢时，betwon为当前betwon的值 + 预计盈利；当主队输时，betwon为当前betwon的值 - 下注金额。

### 下单接口
* 接口日志见20190102笔记

```
{
  "msg": "Successful",
  "msg_code": "0",
  "res_data": {
    "value": {
      "money": {
        "1_0": 100
      },
      "balance_money": 5351553,
      "risk_balance": 5344678,
      "risk_change": -100,
      "market_id": "4",
      "risk_info": "home : 8.0; away : -200.0",
      "userPrepaidMoney": 0,
      "risk_result": -6875,
      "order_id": "8f5035634f564f4cbf055e812aa2dc5c",
      "remainMoney": 100
    }
  }
}
money：当前订单的下注金额
balance_money：用户实际余额
risk_balance：用户可用余额
risk_change：当前下注导致风险值的变化
market_id：玩法id
risk_info：用户当前下注比赛的风险值信息
risk_result：用户最终风险值
order_id：订单号
remainMoney：当前订单的下注金额
```

## 定时任务

### MatchListSchedule联赛列表刷新
* 从t_match_temp表中移除type为1的

* 获取所有的联赛信息
	* 联赛中没有比赛的联赛被放入removeLeague
	* 查不到赛事信息的比赛被放入removeMatchList
	* 更新MongoDB缓存

* 从数据库中查询出比赛列表（一场比赛只属于某一联赛），某一比赛的数据如下：
```
{
  "away_name": "FIRE2",
  "update_time": "2018-05-10T12:32:06.000+0530",
  "event_id": "11490238",
  "home_name": "BBBQ1",
  "tournament_name": "agfgsf",
  "format": "T20",
  "match_time": 1526149800000,
  "match_status": "1",
  "tournament_id": "454"
}
```

* 遍历
* 根据联赛id从MongoDB中查询出联赛下所有比赛的信息，某一联赛的比赛在MongoDB保存的数据如下：

```
{
	"_id" : ObjectId("5af40fece4b093ada5c92a69"),
	"l_id" : "424",
	"l_name" : "xieluo",
	"inplay" : "0",
	"c_time" : ISODate("2018-05-10T09:25:00.128Z"),
	"u_time" : ISODate("2018-05-10T09:25:00.128Z"),
	"list" : [
		{
			"h_name" : "test_z",
			"h_logo" : "http://test.happycric.com:8050/img/null",
			"format" : "T20",
			"e_id" : "11490226",
			"a_logo" : "http://test.happycric.com:8050/img/null",
			"a_name" : "test_x",
			"m_time" : NumberLong("1525977000000")
		}
	]
}
```

* 找出与event_id对应的比赛，更新MongoDB中的比赛数据


### MatchResultSchedule比赛结果刷新
* 同上
### ScoreSchedule比赛比分刷新
* 同上

## 推送
* Your prediction winning ${MONEY} coins have been credited to your account. // 下注
* Your cricket quiz bet wining ${MONEY} coins have been release to your account // 竞猜
* Your bought ${MONEY} coins successfully , please refresh to update // 充值
* You successfully invited one friend and got ${MONEY} free coins. 
* You used invitation code and got ${MONEY} free coins in your account.Go share with more friends and get more bonus.

## 杂项
* queryValidateCode获取某一用户最后一次获取的验证码信息及一天内该帐号获取的验证码次数。

## tips
* tip4是比赛的状态 tip1是投币结果 tip2是比赛进行中 打完第一局的时候会显示哪个队伍领先了多少分之类的 tip3就是赛果
* 希望比赛还没有tip1(投币结果)的时候显示时间，当比赛开始的时候显示tip4，当比赛有tip2的时候展示tip2，比赛结束的时候有tip3展示tip3

## 邮件发送策略
1. 优先选择邮件服务稳定的提供商（可配置）。
2. 当有多个邮件服务提供商时，先使用各个服务商每月的免费限额。
3. 当一个邮件服务商连续3次（可配置）发送邮件失败，则1小时（可配置）内不使用该邮件服务商的服务，转用下一个邮件服务商，以此类推。如果所有服务商发送邮件都失败则将错误消息写入日志，人工介入。
4. 利用Redisson限制并发量

* 例如：亚马逊、mailgun、腾讯的免费限额分别为62000、10000、500（考虑到腾讯的免费限额实际达不到500就出现了邮件发送失败的问题，设置腾讯的阈值为400）

```
CREATE TABLE crc_auth.t_mail_stats (
  `ID` int(11) NOT NULL AUTO_INCREMENT,
  `TYPE` varchar(50) NOT NULL COMMENT '邮件服务商，amazonSES、mailGun、tencentMail',
  `CUR_MONTH` varchar(20) NOT NULL COMMENT '当月日期，格式：yyyy-MM',
  `LIMIT` int(8) DEFAULT 0 COMMENT '每月限额',
  `SUCCESSFUL_COUNT` int(8) DEFAULT 0 COMMENT '成功次数',
  `FAILED_COUNT` int(8) DEFAULT 0 COMMENT '失败次数',
  `CONTINUOUS_FAILED_COUNT` int(8) DEFAULT 0 COMMENT '连续失败次数',
  `UNLOCK_TIME` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '解锁时间',
  `CRT_TIME` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `UPDATE_TIME` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`ID`),
  UNIQUE KEY `uniq_type_month` (`TYPE`, `CUR_MONTH`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8 COMMENT='邮件统计表';
```

* 判断每月免费限额，判断连续发送失败次数（即：解锁时间）

```
{
  "code": "887813",
  "expireTime": 1540195067038,
  "crtTime": 1540194767038,
  "language": "en",
  "id": 403834,
  "event": "REG",
  "type": 1,
  "tagVal": "kumarthirugudu100@gmail.com"
}
code：随机数
expireTime：过期时间
crtTime：创建时间
language：语言
id：t_validate_code表主键
event：REG(注册)，BIND(绑定)，RESET（重置密码）
type：1是邮箱，2是电话
tagVal：收件邮箱
```