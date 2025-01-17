package com.mistra.plank.controller;

import com.mistra.plank.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/spider")
public class SpiderController {

    @Resource
    IndustryService industryService;

    @Resource
    IndustryKLineService industryKLineService;

    @Resource
    MonthService monthService;

    @Resource
    StockEastService stockService;

    @Resource
    StockCollectionsService stockCollectionsService;

    @RequestMapping("/index")
    public String index() {
        return "spider";
    }

    @RequestMapping("/industry")
    @ResponseBody
    public String industry() {
        try {
            industryService.industry();
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    @RequestMapping("/industryKLine")
    @ResponseBody
    public String industryKLine() {
        try {
            industryKLineService.industryKLine();
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    @RequestMapping("/month/{current}")
    @ResponseBody
    public List<Map<String, String>> month(@PathVariable("current") String current) {
        List<Map<String, String>> month = monthService.month(current);
        return month;
    }

    @RequestMapping("/stock/{code}")
    @ResponseBody
    public Map<BigDecimal, String> stock(@PathVariable("code") String code) {
        Map<BigDecimal, String> stock = stockService.stock(code);
        return stock;
    }

    @RequestMapping("/stock/collections/{type}")
    @ResponseBody
    public Map<BigDecimal, String> collections(@PathVariable("type") String type) {
        Map<BigDecimal, String> collections = stockCollectionsService.stockCollections(type);
        return collections;
    }

    @RequestMapping("/stock/longtou/")
    @ResponseBody
    public Map<BigDecimal, String> longtou(@PathVariable("type") String type) {
        Map<BigDecimal, String> collections = stockCollectionsService.stockCollections(type);
        return collections;
    }
}
