package com.mistra.plank.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 某一行业下对应的全部股票
 * 这里只找出股票，具体的股票信息靠手动去分析，什么基本面啊技术面啊筹码分布啊
 */
public interface StockEastService {
    Map<BigDecimal, String> stock(String industryCode);
}
