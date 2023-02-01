package com.mistra.plank.service.impl;

import com.alibaba.fastjson.JSON;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.common.util.HttpUtil;
import com.mistra.plank.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CutomerServiceImpl implements CustomerService {

    private final PlankConfig plankConfig;

    public CutomerServiceImpl(PlankConfig plankConfig) {
        this.plankConfig = plankConfig;
    }

    @Override
    public void ziJin(String period) {
        try {
            //找到所有行业code
            String body = HttpUtil.getHttpGetResponseString(plankConfig.getHangYeCodeUrl(), null);
            body = body.substring(body.indexOf("(") + 1, body.indexOf(")"));
            EastmoneyStockInfoParserImpl.StockResultVo stockResultVo = JSON.parseObject(body, EastmoneyStockInfoParserImpl.StockResultVo.class);
            List<String> hangYeCodeList = stockResultVo.getData().getDiff().stream().map(e -> e.getF12()).collect(Collectors.toList());

            //找到所有行业资金流
            String url = plankConfig.getHangYeZiJinLiuUrl().replace("{period}", period);
            body = HttpUtil.getHttpGetResponseString(url, null);
            EastmoneyStockInfoParserImpl.StockResultVo stockResultVo1 = JSON.parseObject(body, EastmoneyStockInfoParserImpl.StockResultVo.class);
            List<EastmoneyStockInfoParserImpl.StockResultDiffVo> hangYeList = stockResultVo1.getData().getDiff().stream().filter(e -> e.getF174() > 0
                    && hangYeCodeList.contains(e.getF12())).sorted((e1, e2) -> {
                return e1.getF174().compareTo(e2.getF174());
            }).limit(5).collect(Collectors.toList());
            log.info(hangYeList.stream().map(e->e.getF14()).collect(Collectors.joining(",")));

            //找出这个板块的前5名个股

            for (EastmoneyStockInfoParserImpl.StockResultDiffVo stockResultDiffVo : hangYeList) {
                String url2 = plankConfig.getGeGuZiJinLiuUrl().replace("{period}", period).replace("{hangYeCode}",stockResultDiffVo.getF12());
                body = HttpUtil.getHttpGetResponseString(url2, null);
                EastmoneyStockInfoParserImpl.StockResultVo stockResultVo2 = JSON.parseObject(body, EastmoneyStockInfoParserImpl.StockResultVo.class);
                List<EastmoneyStockInfoParserImpl.StockResultDiffVo> geGuList = stockResultVo1.getData().getDiff().stream().filter(e -> e.getF174() > 0
                        && hangYeCodeList.contains(e.getF12())).sorted((e1, e2) -> {
                    return e1.getF174().compareTo(e2.getF174());
                }).limit(5).collect(Collectors.toList());
                log.info(geGuList.stream().map(e->e.getF14()).collect(Collectors.joining(",")));

            }




        } catch (Exception e) {
        }
    }
}