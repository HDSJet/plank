package com.mistra.plank.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.model.entity.TradeDeal;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
import java.util.List;

@Mapper
public interface TradeDealDao extends BaseMapper<TradeDeal> {

    void add(TradeDeal tradeDeal);

    List<TradeDeal> getByDate(Date date);

}
