package com.mistra.plank.strategy.model;

import com.mistra.plank.model.entity.TradeOrder;
import com.mistra.plank.model.vo.trade.TradeRuleVo;
import com.mistra.plank.tradeapi.response.GetDealDataResponse;

import java.util.List;

public class GridStrategyInput extends BaseStrategyInput {

    private List<GetDealDataResponse> dealDataList;
    private List<TradeOrder> tradeOrderList;

    public GridStrategyInput(TradeRuleVo tradeRuleVo) {
        super(tradeRuleVo);
    }

    public List<GetDealDataResponse> getDealDataList() {
        return dealDataList;
    }

    public void setDealDataList(List<GetDealDataResponse> dealDataList) {
        this.dealDataList = dealDataList;
    }

    public List<TradeOrder> getTradeOrderList() {
        return tradeOrderList;
    }

    public void setTradeOrderList(List<TradeOrder> tradeOrderList) {
        this.tradeOrderList = tradeOrderList;
    }

}
