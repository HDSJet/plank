package com.mistra.plank.model.param;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * 基金季报持仓导入参数
 *
 * @author mistra@future.com
 * @date 2022/5/7
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FundHoldingsParam {

    /**
     * 东方财富Choice平台导出的Excel
     */
    private MultipartFile file;

    /**
     * 季度 202201
     */
    private Integer quarter;

}
