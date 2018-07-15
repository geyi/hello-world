# 业务逻辑

## 登录
* 用户登录时将用户信息保存到tair缓存中。

## 订单
* 目前使用到的订单状态：
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

* 虚拟余额=真实余额-风险值

* 订单状态变更过程：
	* 下单时订单状态为220（滚球受理成功），230（赛前受理成功）
	* provider接收数据源推送的赛事消息，如果3秒内赔率没有发生改变会把订单状态改为0（订单确认中），否则不做处理。
	* orderItemStatusSchedule定时扫描3秒前状态为220或230的订单，将其状态改为-5（退款中）。
	* BatchUpdateOrderPlugin定时扫描10秒前状态为0的订单，将其状态改为10（未开奖）。并处理状态为-5（退款中）的订单。

* 下注玩法：

|BUY_CODE |AUTO_ID  |OPT_NAME |CLIENT_PROPERTIES                                                                                            |
|---------|---------|---------|-------------------------------------------------------------------------------------------------------------|
|1        |37973077 |Lay      |{"marketName":"test_x","inning":"","codeName":"Lay","returnMoney":"5,000","overs":"","runs":"","wickets":""} |
|2        |37973077 |Back     |{"marketName":"test_x","inning":"","codeName":"Back","returnMoney":"30","overs":"","runs":"","wickets":""}   |

1.**BUY_CODE：1代表主队赢，2代表客队赢**
2.**OPT_NAME：Lay代表输，Back代表赢**
3.**当BUY_CODE为1时，说明用户买的是主队赢。又因为OPT_NAME为Lay，表示输，所以CLIENT_PROPERTIES的marketName应该为客队队名。与crc_main数据库中t_local_market表中的market_description字段相同**

|event_id |market_id |specifiers       |market_description |
|---------|----------|-----------------|-------------------|
|11490226 |4         |site_market_id=1 |test_x             |

* 字段含义
	* item_money				下注金额
	* prize_money				返奖金额
	* PREPAID_MONEY			本单产生的预支金额
	* remain_prepaid_money	本单使用了其他单预支金额
>预支金额只能使用其它订单产生的预支金额


session


***MatchListSchedule联赛列表刷新***
从t_match_temp表中移除type为1的

获取所有的联赛信息
联赛中没有比赛的联赛被放入removeLeague
查不到赛事信息的比赛被放入removeMatchList
更新MongoDB缓存

从数据库中查询出比赛列表（一场比赛只属于某一联赛），某一比赛的数据如下：
{away_name=FIRE2, update_time=2018-05-10T12:32:06.000+0530, event_id=11490238, home_name=BBBQ1, tournament_name=agfgsf, format=T20, match_time=1526149800000, match_status=1, tournament_id=454}
遍历
根据联赛id从MongoDB中查询出联赛下所有比赛的信息，某一联赛的比赛在MongoDB保存的数据如下：
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
找出与event_id对应的比赛，更新MongoDB中的比赛数据


***MatchResultSchedule比赛结果刷新***
同上
***ScoreSchedule比赛比分刷新***
同上


queryValidateCode获取某一用户最后一次获取的验证码信息及一天内该帐号获取的验证码次数。


***推送***
Your prediction winning ${MONEY} coins have been credited to your account. // 下注
Your cricket quiz bet wining ${MONEY} coins have been release to your account // 竞猜
Your bought ${MONEY} coins successfully , please refresh to update // 充值
You successfully invited one friend and got ${MONEY} free coins. 
You used invitation code and got ${MONEY} free coins in your account.Go share with more friends and get more bonus.