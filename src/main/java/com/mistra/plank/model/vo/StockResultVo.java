package com.mistra.plank.model.vo;

import com.mistra.plank.service.impl.EastmoneyStockInfoParserImpl;
import lombok.Data;

import java.util.List;

@Data
public class StockResultVo {

    private StockResultDataVo data;
     @Data
    public class StockResultDataVo {
        private List<StockResultDiffVo> diff;
    }



}