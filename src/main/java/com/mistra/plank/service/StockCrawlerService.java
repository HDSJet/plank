package com.mistra.plank.service;

import com.mistra.plank.model.entity.DailyIndex;
import com.mistra.plank.model.entity.StockInfo;

import java.util.List;

public interface StockCrawlerService {

    List<StockInfo> getStockList();

    DailyIndex getDailyIndex(String code);

    List<DailyIndex> getDailyIndex(List<String> codeList);

    List<DailyIndex> getDailyIndexFromEastMoney();

    List<DailyIndex> getHistoryDailyIndexs(String code);

    String getHistoryDailyIndexsString(String code);

    String getHistoryDailyIndexsStringFrom163(String code, int year, int season);

}
