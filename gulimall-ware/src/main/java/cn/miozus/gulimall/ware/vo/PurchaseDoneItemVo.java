package cn.miozus.gulimall.ware.vo;

import lombok.Data;

/**
 * 完成项目
 *
 * @author miao
 * @date 2021/09/25
 */
@Data
public class PurchaseDoneItemVo {

    /**
     * 项id
     */
    private Long itemId;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 完成或失败的需求详情
     */
    private String reason;
}
