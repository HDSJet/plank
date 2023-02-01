package com.mistra.plank.controller;

import com.mistra.plank.job.Barbarossa;
import com.mistra.plank.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerController {
    /**
     * 筛选最近五天行业资金流入为正的 股票流入资金为正的股票 市值前三的股票
     */
    @Autowired
    CustomerService customerService;

    @PostMapping("/ziJin")
    public void collectData() {
        String period = "5";

        customerService.ziJin(period);
    }
}
