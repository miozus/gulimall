package cn.miozus.gulimall.product.vo;

import lombok.Data;

/**
 * 签证官attr集团关系
 *
 * @author miao
 * @date 2021/09/09
 */
@Data
public class AttrGroupRelationVo {

    // [{"attrId":1,"attrGroupId":2}]
    private Long attrId;
    private Long attrGroupId;
}
