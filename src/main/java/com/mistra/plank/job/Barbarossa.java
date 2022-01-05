package com.mistra.plank.job;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mistra.plank.config.PlankConfig;
import com.mistra.plank.mapper.ClearanceMapper;
import com.mistra.plank.mapper.DailyRecordMapper;
import com.mistra.plank.mapper.DragonListMapper;
import com.mistra.plank.mapper.HoldSharesMapper;
import com.mistra.plank.mapper.StockMapper;
import com.mistra.plank.mapper.TradeRecordMapper;
import com.mistra.plank.pojo.Clearance;
import com.mistra.plank.pojo.DailyRecord;
import com.mistra.plank.pojo.DragonList;
import com.mistra.plank.pojo.HoldShares;
import com.mistra.plank.pojo.Stock;
import com.mistra.plank.pojo.TradeRecord;
import com.mistra.plank.pojo.enums.ClearanceReasonEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 巴巴罗萨计划
 *
 * @author mistra@future.com
 * @date 2021/11/19
 */
@Slf4j
@Component
public class Barbarossa implements CommandLineRunner {

    private final StockMapper stockMapper;
    private final DailyRecordMapper dailyRecordMapper;
    private final ClearanceMapper clearanceMapper;
    private final TradeRecordMapper tradeRecordMapper;
    private final HoldSharesMapper holdSharesMapper;
    private final DragonListMapper dragonListMapper;
    private final PlankConfig plankConfig;
    private final DailyRecordProcessor dailyRecordProcessor;
    private final DragonListProcessor dragonListProcessor;
    private final StockProcessor stockProcessor;

    public static final HashMap<String, String> STOCK_MAP = new HashMap<>();

    /**
     * 总金额
     */
    public static BigDecimal BALANCE = new BigDecimal(1000000);
    /**
     * 可用金额
     */
    public static BigDecimal BALANCE_AVAILABLE = new BigDecimal(1000000);

    public Barbarossa(StockMapper stockMapper, DailyRecordMapper dailyRecordMapper, ClearanceMapper clearanceMapper,
                      TradeRecordMapper tradeRecordMapper, HoldSharesMapper holdSharesMapper,
                      DragonListMapper dragonListMapper, PlankConfig plankConfig, DailyRecordProcessor dailyRecordProcessor,
                      DragonListProcessor dragonListProcessor, StockProcessor stockProcessor) {
        this.stockMapper = stockMapper;
        this.dailyRecordMapper = dailyRecordMapper;
        this.clearanceMapper = clearanceMapper;
        this.tradeRecordMapper = tradeRecordMapper;
        this.holdSharesMapper = holdSharesMapper;
        this.dragonListMapper = dragonListMapper;
        this.plankConfig = plankConfig;
        this.dailyRecordProcessor = dailyRecordProcessor;
        this.dragonListProcessor = dragonListProcessor;
        this.stockProcessor = stockProcessor;
    }

    @Override
    public void run(String... args) throws Exception {
        List<Stock> stocks = stockMapper.selectList(new QueryWrapper<Stock>()
                .notLike("name", "%ST%")
                .notLike("name", "%st%")
                .notLike("name", "%A%")
                .notLike("name", "%C%")
                .notLike("name", "%N%")
                .notLike("name", "%U%")
                .notLike("name", "%W%")
                .notLike("code", "%BJ%")
                .notLike("code", "%688%")
        );
        stocks.forEach(stock -> STOCK_MAP.put(stock.getCode(), stock.getName()));
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>一共加载[{}]支股票！", stocks.size());
        BALANCE = new BigDecimal(plankConfig.getFunds());
        BALANCE_AVAILABLE = BALANCE;
        this.collectData();
//        this.barbarossa();
    }

    @Scheduled(cron = "0 0 17 * * ? ")
    private void collectData() throws Exception {
//        stockProcessor.run();
//        dragonListProcessor.run();
        dailyRecordProcessor.run();
        Thread.sleep(10000);
        analyze();
    }

    /**
     * 分析首板一进二，二板二进三胜率
     */
    private void analyze() {
        HashSet<String> stock = new HashSet<>();
        String strDateFormat = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
        Date date = new DateTime(plankConfig.getAnalyzeTime()).withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0)
                .withMillisOfSecond(0).toDate();
        //首板一进二胜率
        HashMap<String, BigDecimal> oneToTwo = new HashMap<>(64);
        //二板二进三胜率
        HashMap<String, BigDecimal> twoToThree = new HashMap<>(64);
        //三板三进四胜率
        HashMap<String, BigDecimal> threeToFour = new HashMap<>(64);
        //四板四进无胜率
        HashMap<String, BigDecimal> fourToFive = new HashMap<>(64);
        List<DailyRecord> dailyRecords = dailyRecordMapper.selectList(new QueryWrapper<DailyRecord>()
                .ge("date", date));
        Map<String, List<DailyRecord>> dateListMap = dailyRecords.stream().collect(Collectors.groupingBy(dailyRecord -> sdf.format(dailyRecord.getDate())));
        //昨日首板
        HashMap<String, Double> yesterdayOne = new HashMap<>(64);
        //昨日二板
        HashMap<String, Double> yesterdayTwo = new HashMap<>(64);
        //昨日三板
        HashMap<String, Double> yesterdayThree = new HashMap<>(64);
        //昨日四板
        HashMap<String, Double> yesterdayFour = new HashMap<>(64);
        do {
            List<DailyRecord> records = dateListMap.get(sdf.format(date));
            if (CollectionUtils.isNotEmpty(records)) {
                //今日首板
                HashMap<String, Double> todayOne = new HashMap<>(64);
                //今日二板
                HashMap<String, Double> todayTwo = new HashMap<>(32);
                //今日三板
                HashMap<String, Double> todayThree = new HashMap<>(16);
                //今日四板
                HashMap<String, Double> todayFour = new HashMap<>(16);
                //今日五板
                HashMap<String, Double> todayFive = new HashMap<>(16);
                for (DailyRecord dailyRecord : records) {
                    double v = dailyRecord.getIncreaseRate().doubleValue();
                    String name = dailyRecord.getName();
                    String code = dailyRecord.getCode();
                    if ((!code.contains("SZ30") && v > 9.7 && v < 11) || (code.contains("SZ30") && v > 19.7 && v < 21)) {
                        if (yesterdayFour.containsKey(name)) {
                            // 昨日的四板，今天继续板，进阶5板
                            todayFive.put(dailyRecord.getName(), v);
                        } else if (yesterdayThree.containsKey(name)) {
                            // 昨日的三板，今天继续板，进阶4板
                            todayFour.put(dailyRecord.getName(), v);
                        } else if (yesterdayTwo.containsKey(name)) {
                            // 昨日的二板，今天继续板，进阶3板
                            todayThree.put(dailyRecord.getName(), v);
                        } else if (yesterdayOne.containsKey(name)) {
                            //昨日首板，今天继续板，进阶2板
                            todayTwo.put(dailyRecord.getName(), v);
                        } else if (!yesterdayOne.containsKey(name)) {
                            //昨日没有板，今日首板
                            todayOne.put(dailyRecord.getName(), v);
                        }
                    }
                }
                if (yesterdayOne.size() > 0) {
                    //一进二成功率
                    oneToTwo.put(sdf.format(date), new BigDecimal(todayTwo.size()).divide(new BigDecimal(yesterdayOne.size()), 2, BigDecimal.ROUND_HALF_UP));
                }
                if (yesterdayTwo.size() > 0) {
                    //二进三成功率
                    twoToThree.put(sdf.format(date), new BigDecimal(todayThree.size()).divide(new BigDecimal(yesterdayTwo.size()), 2, BigDecimal.ROUND_HALF_UP));
                }
                if (yesterdayThree.size() > 0) {
                    //三进四成功率
                    threeToFour.put(sdf.format(date), new BigDecimal(todayFour.size()).divide(new BigDecimal(yesterdayThree.size()), 2, BigDecimal.ROUND_HALF_UP));
                }
                if (yesterdayFour.size() > 0) {
                    //四进五成功率
                    fourToFive.put(sdf.format(date), new BigDecimal(todayFive.size()).divide(new BigDecimal(yesterdayFour.size()), 2, BigDecimal.ROUND_HALF_UP));
                }
                log.info("\n{}日\n二板:{}\n三板:{}\n四板:{}\n五板:{}", sdf.format(date), new ArrayList<>(todayTwo.keySet()),
                        new ArrayList<>(todayThree.keySet()), new ArrayList<>(todayFour.keySet()), new ArrayList<>(todayFive.keySet()));
                stock.addAll(todayThree.keySet());
                stock.addAll(todayFour.keySet());
                stock.addAll(todayFive.keySet());
                yesterdayOne.clear();
                yesterdayOne.putAll(todayOne);
                yesterdayTwo.clear();
                yesterdayTwo.putAll(todayTwo);
                yesterdayThree.clear();
                yesterdayThree.putAll(todayThree);
                yesterdayFour.clear();
                yesterdayFour.putAll(todayFour);
            }
            date = DateUtils.addDays(date, 1);
        } while (date.getTime() < System.currentTimeMillis());
        log.info("当前分析时间段所有上榜股票:{}", stock);
        double one = 0d;
        for (Map.Entry<String, BigDecimal> entry : oneToTwo.entrySet()) {
            log.info("一进二胜率：日期:{},胜率:{}", entry.getKey(), entry.getValue());
            one += entry.getValue().doubleValue();
        }
        double two = 0d;
        for (Map.Entry<String, BigDecimal> entry : twoToThree.entrySet()) {
            log.info("二进三胜率：日期:{},胜率:{}", entry.getKey(), entry.getValue());
            two += entry.getValue().doubleValue();
        }
        double three = 0d;
        for (Map.Entry<String, BigDecimal> entry : threeToFour.entrySet()) {
            log.info("三进四胜率：日期:{},胜率:{}", entry.getKey(), entry.getValue());
            three += entry.getValue().doubleValue();
        }
        double four = 0d;
        for (Map.Entry<String, BigDecimal> entry : fourToFive.entrySet()) {
            log.info("四进五胜率：日期:{},胜率:{}", entry.getKey(), entry.getValue());
            four += entry.getValue().doubleValue();
        }
        if (oneToTwo.size() > 0) {
            log.info("首板>一进二平均胜率：{}", new BigDecimal(one).divide(new BigDecimal(oneToTwo.size()), 2, BigDecimal.ROUND_HALF_UP));
        }
        if (twoToThree.size() > 0) {
            log.info("二板>二进三平均胜率：{}", new BigDecimal(two).divide(new BigDecimal(twoToThree.size()), 2, BigDecimal.ROUND_HALF_UP));
        }
        if (threeToFour.size() > 0) {
            log.info("三板>三进四平均胜率：{}", new BigDecimal(three).divide(new BigDecimal(threeToFour.size()), 2, BigDecimal.ROUND_HALF_UP));
        }
        if (fourToFive.size() > 0) {
            log.info("四板>四进五平均胜率：{}", new BigDecimal(four).divide(new BigDecimal(fourToFive.size()), 2, BigDecimal.ROUND_HALF_UP));
        }
    }

    /**
     * 巴巴罗萨计划
     */
    private void barbarossa() throws Exception {
        Date date = new Date(plankConfig.getBeginDay());
        do {
            this.barbarossa(date);
            date = DateUtils.addDays(date, 1);
        } while (date.getTime() < System.currentTimeMillis());
    }

    private void barbarossa(Date date) {
        int week = DateUtil.dayOfWeek(date);
        if (week < 7 && week > 1) {
            // 工作日
            List<Stock> stocks = this.checkCanBuyStock(date);
            if (CollectionUtils.isNotEmpty(stocks) && BALANCE_AVAILABLE.intValue() > 10000) {
                this.buyStock(stocks, date);
            }
            this.sellStock(date);
        }
    }

    /**
     * 检查可以买的票
     * 首板或者2板  10日涨幅介于10-22%
     * 计算前8天的振幅在15%以内
     *
     * @return List<String>
     */
    private List<Stock> checkCanBuyStock(Date date) {
        List<DragonList> dragonLists = dragonListMapper.selectList(new QueryWrapper<DragonList>()
                .ge("date", date).le("date", date).ge("price", 6).le("price", 100)
                .notLike("name", "%ST%")
                .notLike("name", "%st%")
                .notLike("name", "%A%")
                .notLike("name", "%C%")
                .notLike("name", "%N%")
                .notLike("name", "%U%")
                .notLike("name", "%W%")
                .notLike("code", "%BJ%")
                .notLike("code", "%688%"));
        if (CollectionUtils.isEmpty(dragonLists)) {
            return null;
        }
        List<DailyRecord> dailyRecords = new ArrayList<>();
        for (DragonList dragonList : dragonLists) {
            Page<DailyRecord> page = dailyRecordMapper.selectPage(new Page<>(1, 30), new QueryWrapper<DailyRecord>()
                    .eq("code", dragonList.getCode()).le("date", date).ge("date", DateUtils.addDays(date, -30))
                    .orderByDesc("date"));
            if (page.getRecords().size() > 10) {
                dailyRecords.addAll(page.getRecords().subList(0, 10));
            }
        }
        Map<String, List<DailyRecord>> map = dailyRecords.stream().collect(Collectors.groupingBy(DailyRecord::getCode));
        List<String> stockCode = new ArrayList<>();
        for (Map.Entry<String, List<DailyRecord>> entry : map.entrySet()) {
            // 近8日涨幅
            BigDecimal eightRatio = entry.getValue().get(0).getClosePrice().divide(entry.getValue().get(8).getClosePrice(), 2);
            // 近3日涨幅
            BigDecimal threeRatio = entry.getValue().get(0).getClosePrice().divide(entry.getValue().get(3).getClosePrice(), 2);
            // 前3个交易日大跌的也排除
            if (eightRatio.doubleValue() <= 1.22 && eightRatio.doubleValue() >= 1.1 && threeRatio.doubleValue() < 1.22 &&
                    entry.getValue().get(0).getIncreaseRate().doubleValue() > 0.04 && entry.getValue().get(1).getIncreaseRate().doubleValue() > -0.04 &&
                    entry.getValue().get(2).getIncreaseRate().doubleValue() > -0.04) {
                stockCode.add(entry.getKey());
            }
        }
        dragonLists = dragonLists.stream().filter(dragonList -> stockCode.contains(dragonList.getCode())).collect(Collectors.toList());
        dragonLists = dragonLists.stream().sorted((a, b) -> b.getBuy().compareTo(a.getBuy())).collect(Collectors.toList());
        List<Stock> result = new ArrayList<>();
        for (DragonList dragonList : dragonLists) {
            result.add(stockMapper.selectOne(new QueryWrapper<Stock>().eq("code", dragonList.getCode())));
        }
        return result;
    }

    private void buyStock(List<Stock> stocks, Date date) {
        for (Stock stock : stocks) {
            List<HoldShares> holdShares = holdSharesMapper.selectList(new QueryWrapper<>());
            if (holdShares.size() >= plankConfig.getFundsPart()) {
                log.info("仓位已打满！");
                return;
            }
            Page<DailyRecord> selectPage = dailyRecordMapper.selectPage(new Page<>(1, 5), new QueryWrapper<DailyRecord>()
                    .eq("code", stock.getCode())
                    .ge("date", date)
                    .le("date", DateUtils.addDays(date, 12))
                    .orderByAsc("date"));
            if (selectPage.getRecords().size() < 2) {
                continue;
            }
            DailyRecord dailyRecord = selectPage.getRecords().get(1);
            double openRatio = (selectPage.getRecords().get(1).getOpenPrice().subtract(selectPage.getRecords().get(0).getClosePrice()))
                    .divide(selectPage.getRecords().get(0).getClosePrice(), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
            if (openRatio > -0.02 && openRatio < plankConfig.getBuyPlankRatioLimit().doubleValue() && BALANCE_AVAILABLE.intValue() > 10000) {
                // 低开2个点以下不买
                HoldShares one = holdSharesMapper.selectOne(new QueryWrapper<HoldShares>().eq("code", stock.getCode()));
                if (Objects.isNull(one)) {
                    int money = BALANCE.intValue() / plankConfig.getFundsPart();
                    money = Math.min(money, BALANCE_AVAILABLE.intValue());
                    int number = money / dailyRecord.getOpenPrice().multiply(new BigDecimal(100)).intValue();
                    double cost = number * 100 * dailyRecord.getOpenPrice().doubleValue();
                    BALANCE_AVAILABLE = BALANCE_AVAILABLE.subtract(new BigDecimal(cost));
                    HoldShares holdShare = HoldShares.builder().buyTime(DateUtils.addHours(dailyRecord.getDate(), 9)).code(stock.getCode()).name(stock.getName())
                            .cost(dailyRecord.getOpenPrice()).fifteenProfit(false).number(number * 100)
                            .profit(new BigDecimal(0)).currentPrice(dailyRecord.getOpenPrice()).rate(new BigDecimal(0))
                            .buyPrice(dailyRecord.getOpenPrice()).buyNumber(number * 100).build();
                    holdSharesMapper.insert(holdShare);
                    TradeRecord tradeRecord = new TradeRecord();
                    tradeRecord.setName(holdShare.getName());
                    tradeRecord.setCode(holdShare.getCode());
                    tradeRecord.setDate(DateUtils.addHours(dailyRecord.getDate(), 9));
                    tradeRecord.setMoney((int) (number * 100 * dailyRecord.getOpenPrice().doubleValue()));
                    tradeRecord.setReason("买入" + holdShare.getName() + number * 100 + "股，花费" + cost + "元，当前可用余额" +
                            BALANCE_AVAILABLE.intValue());
                    tradeRecord.setBalance(BALANCE.setScale(2, BigDecimal.ROUND_HALF_UP));
                    tradeRecord.setAvailableBalance(BALANCE_AVAILABLE.setScale(2, BigDecimal.ROUND_HALF_UP));
                    tradeRecord.setPrice(dailyRecord.getOpenPrice());
                    tradeRecord.setNumber(number * 100);
                    tradeRecord.setType(0);
                    tradeRecordMapper.insert(tradeRecord);
                }
            }
        }
    }

    /**
     * 减仓或清仓股票
     *
     * @param date 日期
     */
    private void sellStock(Date date) {
        List<HoldShares> holdShares = holdSharesMapper.selectList(new QueryWrapper<>());
        if (CollectionUtils.isNotEmpty(holdShares)) {
            for (HoldShares holdShare : holdShares) {
                if (!DateUtils.isSameDay(holdShare.getBuyTime(), date) && holdShare.getBuyTime().getTime() < date.getTime()) {
                    Page<DailyRecord> selectPage = dailyRecordMapper.selectPage(new Page<>(1, 20), new QueryWrapper<DailyRecord>()
                            .eq("code", holdShare.getCode())
                            .ge("date", DateUtils.addDays(date, -plankConfig.getDeficitMovingAverage() - 9))
                            .le("date", date)
                            .orderByDesc("date"));
                    List<DailyRecord> dailyRecords = selectPage.getRecords().subList(0, 5);
                    // 今日数据明细
                    DailyRecord todayRecord = dailyRecords.get(0);
                    // 5日均线价格
                    OptionalDouble average = dailyRecords.stream().mapToDouble(dailyRecord -> dailyRecord.getClosePrice().doubleValue()).average();
                    if (average.isPresent() && (todayRecord.getLowest().doubleValue() <= average.getAsDouble())) {
                        // 跌破均线，清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.BREAK_POSITION, date, average.getAsDouble());
                        continue;
                    }

                    // 盘中最低收益率
                    double profitLowRatio = todayRecord.getLowest().subtract(holdShare.getBuyPrice()).divide(holdShare.getBuyPrice(), 2).doubleValue();
                    if (profitLowRatio < plankConfig.getDeficitRatio().doubleValue()) {
                        // 跌破止损线，清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.BREAK_LOSS_LINE, date,
                                holdShare.getBuyPrice().doubleValue() * (1 + plankConfig.getDeficitRatio().doubleValue()));
                        continue;
                    }

                    if (holdShare.getFifteenProfit() && profitLowRatio <= plankConfig.getProfitClearanceRatio().doubleValue()) {
                        // 收益回撤到10个点止盈清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.TAKE_PROFIT, date, holdShare.getBuyPrice().doubleValue() * 1.1);
                        continue;
                    }

                    // 盘中最高收益率
                    double profitHighRatio = todayRecord.getHighest().subtract(holdShare.getBuyPrice()).divide(holdShare.getBuyPrice(), 2).doubleValue();
                    if (profitHighRatio >= plankConfig.getProfitUpperRatio().doubleValue()) {
                        // 收益25% 清仓
                        this.clearanceStock(holdShare, ClearanceReasonEnum.PROFIT_UPPER, date,
                                holdShare.getBuyPrice().doubleValue() * (1 + plankConfig.getProfitUpperRatio().doubleValue()));
                    } else if (profitHighRatio >= plankConfig.getProfitQuarterRatio().doubleValue()) {
                        // 收益20% 减至1/4仓
                        this.reduceStock(holdShare, ClearanceReasonEnum.POSITION_QUARTER, date, todayRecord,
                                holdShare.getBuyPrice().doubleValue() * (1 + plankConfig.getProfitQuarterRatio().doubleValue()));
                    } else if (profitHighRatio >= plankConfig.getProfitHalfRatio().doubleValue()) {
                        // 收益15% 减半仓
                        this.reduceStock(holdShare, ClearanceReasonEnum.POSITION_HALF, date, todayRecord,
                                holdShare.getBuyPrice().doubleValue() * (1 + plankConfig.getProfitHalfRatio().doubleValue()));
                    }

                    // 持股超过8天 清仓
                    if (Days.daysBetween(new LocalDate(holdShare.getBuyTime().getTime()), new LocalDate(date.getTime())).getDays() > plankConfig.getClearanceDay()) {
                        this.clearanceStock(holdShare, ClearanceReasonEnum.TEN_DAY, date, todayRecord.getOpenPrice().add(todayRecord.getClosePrice()).doubleValue() / 2);
                    }
                }
            }
        }
    }

    /**
     * 减仓股票
     *
     * @param holdShare           持仓记录
     * @param clearanceReasonEnum 清仓原因
     * @param date                时间
     * @param sellPrice           清仓价格
     */
    private void reduceStock(HoldShares holdShare, ClearanceReasonEnum clearanceReasonEnum, Date date, DailyRecord todayRecord, double sellPrice) {
        if (holdShare.getNumber() <= 0) {
            holdSharesMapper.delete(new QueryWrapper<HoldShares>().eq("id", holdShare.getId()));
            return;
        }
        // 卖出数量
        int number = holdShare.getNumber() <= 100 ? 100 : holdShare.getNumber() / 2;
        // 卖出金额
        double money = number * sellPrice;
        // 本次卖出部分盈利金额
        BigDecimal profit = new BigDecimal(number * (sellPrice - holdShare.getBuyPrice().doubleValue()));

        // 可用金额
        BALANCE_AVAILABLE = BALANCE_AVAILABLE.add(new BigDecimal(money));
        TradeRecord tradeRecord = new TradeRecord();
        tradeRecord.setName(holdShare.getName());
        tradeRecord.setCode(holdShare.getCode());
        tradeRecord.setDate(date);
        tradeRecord.setMoney((int) money);
        tradeRecord.setReason("减仓" + holdShare.getName() + number + "股，卖出金额" + (int) money + "元，当前可用余额" + BALANCE_AVAILABLE.intValue() +
                "，减仓原因" + clearanceReasonEnum.getDesc());
        tradeRecord.setBalance(BALANCE.setScale(2, BigDecimal.ROUND_HALF_UP));
        tradeRecord.setAvailableBalance(BALANCE_AVAILABLE.setScale(2, BigDecimal.ROUND_HALF_UP));
        tradeRecord.setPrice(new BigDecimal(sellPrice));
        tradeRecord.setNumber(number);
        tradeRecord.setType(1);
        tradeRecordMapper.insert(tradeRecord);
        if (holdShare.getNumber() - number == 0) {
            holdSharesMapper.delete(new QueryWrapper<HoldShares>().eq("id", holdShare.getId()));
            return;
        }
        holdShare.setNumber(holdShare.getNumber() - number);
        holdShare.setCost(holdShare.getBuyPrice().multiply(new BigDecimal(holdShare.getBuyNumber())).subtract(profit).divide(new BigDecimal(number), 2));
        holdShare.setProfit(holdShare.getProfit().add(profit));
        holdShare.setFifteenProfit(true);
        holdShare.setCurrentPrice(todayRecord.getClosePrice());
        holdShare.setRate(todayRecord.getClosePrice().subtract(holdShare.getBuyPrice()).divide(holdShare.getBuyPrice(), 2));
        holdSharesMapper.updateById(holdShare);
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>减仓{},目前盈利{}元!", holdShare.getName(), holdShare.getProfit().add(profit).intValue());
    }

    /**
     * 清仓股票
     *
     * @param holdShare           持仓记录
     * @param clearanceReasonEnum 清仓原因
     * @param date                时间
     * @param sellPrice           清仓价格
     */
    private void clearanceStock(HoldShares holdShare, ClearanceReasonEnum clearanceReasonEnum, Date date, double sellPrice) {
        if (holdShare.getNumber() <= 0) {
            holdSharesMapper.delete(new QueryWrapper<HoldShares>().eq("id", holdShare.getId()));
            return;
        }
        // 卖出金额
        double money = holdShare.getNumber() * sellPrice;
        // 本次卖出部分盈利金额
        BigDecimal profit = new BigDecimal(holdShare.getNumber() * (sellPrice - holdShare.getBuyPrice().doubleValue()));
        // 总盈利
        profit = holdShare.getProfit().add(profit);
        // 总资产
        BALANCE = BALANCE.add(profit);
        // 可用金额
        BALANCE_AVAILABLE = BALANCE_AVAILABLE.add(new BigDecimal(money));
        TradeRecord tradeRecord = new TradeRecord();
        tradeRecord.setName(holdShare.getName());
        tradeRecord.setCode(holdShare.getCode());
        tradeRecord.setDate(date);
        tradeRecord.setMoney((int) money);
        tradeRecord.setReason("清仓" + holdShare.getName() + holdShare.getNumber() + "股，卖出金额" + (int) money + "元，当前可用余额" + BALANCE_AVAILABLE.intValue()
                + "，清仓原因" + clearanceReasonEnum.getDesc());
        tradeRecord.setBalance(BALANCE.setScale(2, BigDecimal.ROUND_HALF_UP));
        tradeRecord.setAvailableBalance(BALANCE_AVAILABLE.setScale(2, BigDecimal.ROUND_HALF_UP));
        tradeRecord.setPrice(new BigDecimal(sellPrice));
        tradeRecord.setNumber(holdShare.getNumber());
        tradeRecord.setType(1);
        tradeRecordMapper.insert(tradeRecord);
        String strDateFormat = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
        Clearance clearance = new Clearance();
        clearance.setCode(holdShare.getCode());
        clearance.setName(holdShare.getName());
        clearance.setCostPrice(holdShare.getBuyPrice());
        clearance.setNumber(holdShare.getBuyNumber());
        clearance.setPrice(new BigDecimal(sellPrice));
        clearance.setRate(profit.divide(new BigDecimal(holdShare.getBuyNumber() * holdShare.getBuyPrice().doubleValue()), 2));
        clearance.setProfit(profit);
        clearance.setReason("清仓" + holdShare.getName() + "总计盈亏" + profit.intValue() + "元，清仓原因:" +
                clearanceReasonEnum.getDesc() + "建仓日期" + sdf.format(holdShare.getBuyTime()));
        clearance.setDate(date);
        clearance.setBalance(BALANCE.setScale(2, BigDecimal.ROUND_HALF_UP));
        clearance.setAvailableBalance(BALANCE_AVAILABLE.setScale(2, BigDecimal.ROUND_HALF_UP));
        clearance.setDayNumber(Days.daysBetween(new LocalDate(holdShare.getBuyTime().getTime()), new LocalDate(date.getTime())).getDays());
        clearanceMapper.insert(clearance);
        holdSharesMapper.delete(new QueryWrapper<HoldShares>().eq("id", holdShare.getId()));
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>清仓{},总共盈利{}元!总资产:[{}]", holdShare.getName(), profit.intValue(), BALANCE.intValue());
    }

}
