package cn.miozus.gulimall.product.vo;

import lombok.Data;


/**
 * attr签证官
 *
 * @author miao
 * @date 2021/09/07
 */
@Data
//@TableName("pms_attr")
public class AttrVo /*implements Serializable */{
    private static final long serialVersionUID = 1L;

    /**
     * 属性id
     */
    //@TableId
    private Long attrId;
    /**
     * 属性名
     */
    private String attrName;
    /**
     * 是否需要检索[0-不需要，1-需要]
     */
    private Integer searchType;
    /**
     * 属性图标
     */
    private String icon;
    /**
     * 可选值列表[用逗号分隔]
     */
    private String valueSelect;
    /**
     * 属性类型[0-销售属性，1-基本属性
     */
    private Integer attrType;
    /**
     * 启用状态[0 - 禁用，1 - 启用]
     */
    private Long enable;
    /**
     * 所属分类
     */
    private Long catelogId;
    /**
     * 快速展示【是否展示在介绍上；0-否 1-是】，在sku中仍然可以调整
     */
    private Integer showDesc;

    /**
     * attr组id
     */
    private Long attrGroupId;
}
