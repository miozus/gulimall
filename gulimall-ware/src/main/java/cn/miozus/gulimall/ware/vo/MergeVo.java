package cn.miozus.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

/**
 * 合并签证官
 *
 * @author miao
 * @date 2021/09/23
 */
@Data
public class MergeVo {
    /**
     * 整单id
     */
    private Long purchaseId;
    /**
     * 合并项集合
     */
    private List<Long> items;
}
