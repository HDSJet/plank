package com.mistra.plank.service.impl;

import com.alibaba.fastjson.JSON;
import com.mistra.plank.common.config.PlankConfig;
import com.mistra.plank.common.util.HttpUtil;
import com.mistra.plank.model.vo.StockResultDiffVo;
import com.mistra.plank.model.vo.StockResultVo;
import com.mistra.plank.service.CustomerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
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
            body = body.substring(body.indexOf("(") + 1, body.indexOf(")")).replaceAll("\"-\"", "0");
            StockResultVo stockResultVo = JSON.parseObject(body, StockResultVo.class);
            List<String> hangYeCodeList = stockResultVo.getData().getDiff().stream().map(e -> e.getF12()).collect(Collectors.toList());

            //找到所有行业资金流
            String url = plankConfig.getHangYeZiJinLiuUrl().replace("{period}", period);
            body = HttpUtil.getHttpGetResponseString(url, null);
            body = body.substring(body.indexOf("(") + 1, body.indexOf(")")).replaceAll("\"-\"", "0");
            StockResultVo stockResultVo1 = JSON.parseObject(body, StockResultVo.class);
            List<StockResultDiffVo> hangYeList = stockResultVo1.getData().getDiff().stream().filter(e -> e.getF164() > 0
                    && hangYeCodeList.contains(e.getF12())).sorted(
                    Comparator.comparing(StockResultDiffVo::getF164, Comparator.nullsFirst(Double::compareTo)).reversed()

            ).limit(5).collect(Collectors.toList());
            log.info("资金流向前五板块:{}", hangYeList.stream().map(e -> e.getF14()).collect(Collectors.joining(",")));

            //找出这个板块的前5名个股
            String stock = "";
            for (StockResultDiffVo stockResultDiffVo : hangYeList) {
                String url2 = plankConfig.getGeGuZiJinLiuUrl().replace("{period}", period).replace("{hangYeCode}", stockResultDiffVo.getF12());
                body = HttpUtil.getHttpGetResponseString(url2, null);
                body = body.substring(body.indexOf("(") + 1, body.indexOf(")")).replaceAll("\"-\"", "0");
                StockResultVo stockResultVo2 = JSON.parseObject(body, StockResultVo.class);
                List<StockResultDiffVo> geGuList = stockResultVo2.getData().getDiff().stream().filter(e -> e.getF164() > 0
                ).sorted(
                        Comparator.comparing(StockResultDiffVo::getF164, Comparator.nullsFirst(Double::compareTo)).reversed()

                ).limit(5).collect(Collectors.toList());
                stock = stock +"\r\n"+ geGuList.stream().map(e -> e.getF12() + "," + e.getF14()).collect(Collectors.joining("\r\n"));
                log.info("板块:{} 资金流入前五股票:{}", stockResultDiffVo.getF14(), geGuList.stream().map(e -> e.getF14()).collect(Collectors.joining(",")));

            }
            log.info(stock);

        } catch (Exception e) {
        }
    }
}