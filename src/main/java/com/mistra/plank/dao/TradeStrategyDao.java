package com.mistra.plank.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mistra.plank.model.entity.TradeStrategy;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface TradeStrategyDao extends BaseMapper<TradeStrategy> {

    List<TradeStrategy> getAll();

}
