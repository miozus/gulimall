package cn.miozus.gulimall.ware.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 做签证官
 *
 * @author miao
 * @date 2021/09/25
 */
@Data
public class PurchaseDoneVo {

    @NotNull
    private Long id; // 123采购单id
    private List<PurchaseDoneItemVo> items; //完成/失败的需求详情


}
