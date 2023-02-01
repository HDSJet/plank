package com.mistra.plank.tradeapi.request;

public class CrSubmitBatTradeV2Request extends SubmitBatTradeV2Request {

    public CrSubmitBatTradeV2Request(int userId) {
        super(userId);
    }

    @Override
    public String getMethod() {
        return TradeRequestMethod.CrSubmitBatTradeV2.value();
    }

    public static class CrSubmitData extends SubmitData {

        private String Xyjylx = "6";

        public String getXyjylx() {
            return Xyjylx;
        }

        public void setXyjylx(String xyjylx) {
            Xyjylx = xyjylx;
        }

    }

}
