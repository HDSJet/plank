package com.mistra.plank.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.mistra.plank.service.IndustryKLineService;
import com.mistra.plank.utils.RedisUtil;
import com.mistra.plank.utils.WebUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class IndustryKLineServiceImpl implements IndustryKLineService {
    @Resource
    TaskExecutor taskExecutor;

    @Resource
    WebUtil webUtil;

    @Resource
    RedisUtil redisUtil;

    @Resource
    ObjectMapper objectMapper;

    @Value("${industryKLine.fields1}")
    private String fields1;
    @Value("${industryKLine.fields2}")
    private String fields2;
    @Value("${industryKLine.klt}")
    private String klt;
    @Value("${industryKLine.fqt}")
    private String fqt;
    @Value("${industryKLine.type}")
    private String type;
    @Value("${industryKLine.end}")
    private String end;

    @Override
    public void industryKLine() {
        String industryKLineApi = "https://push2his.eastmoney.com/api/qt/stock/kline/get?fields1=%s&fields2=%s&klt=%s&fqt=%s&secid=%s.%s&end=%s&lmt=%s";

        //从15年股灾后开始算起,2016-02-01
        long limit = LocalDate.parse("2016-02-01").until(LocalDate.now(), ChronoUnit.MONTHS) + 1;

        int currentYear = LocalDate.now().getYear();

        redisUtil.selectDataBase(0);

        Set<String> industrySet = redisUtil.getKeysByString("*");

        redisUtil.selectDataBase(1);

        for (String industryCode : industrySet) {
            taskExecutor.execute(() -> {
                Map<String, String> hashMap = new HashMap<>();
                String formatApi = String.format(industryKLineApi, fields1, fields2, klt, fqt, type, industryCode, end, limit);
                String web = webUtil.getWeb(formatApi);
                try {
                    JsonNode jsonNode = objectMapper.readTree(web);
                    JsonNode node = jsonNode.get("data").get("klines");
                    for (JsonNode kline : node) {
                        String date = kline.asText().split(",")[0];
                        if (date.contains(String.valueOf(currentYear))) {
                            if (hashMap.size() == 0) {
                                System.out.println("this year, new industry, code is: " + industryCode);
                            }
                            break;
                        }

                        StringBuilder key = new StringBuilder();
                        LocalDate localDate = LocalDate.parse(date);
                        key.append(localDate.getYear()).append("-").append(localDate.getMonthValue());

                        double quoteChange = Double.parseDouble(kline.asText().split(",")[1]);

                        hashMap.put(key.toString(), String.valueOf(quoteChange));
                    }
                    redisUtil.setByHash(industryCode, hashMap);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}
