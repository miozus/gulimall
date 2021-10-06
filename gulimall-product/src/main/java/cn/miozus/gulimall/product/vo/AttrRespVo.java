package cn.miozus.gulimall.product.vo;

import lombok.Data;

/**
 * attr resp签证官
 *
 * @author miao
 * @date 2021/09/07
 */
@Data
public class AttrRespVo extends AttrVo {

    /*
        "catalogName": "手机/数码/手机", //所属分类名字
        "groupName": "主体", //所属分组名字
    */
    private String catalogName;
    private String groupName;
    private Long[] catalogPath;
}
