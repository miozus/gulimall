package cn.miozus.gulimall.search.vo;

import lombok.Data;

import java.util.List;

/**
 * 搜索参数：所有页面可能传输的查询条件
 *
 * @author miao
 * @date 2021/10/23
 */
@Data
public class SearchParam {

    /**
     * 关键字：全文匹配
     */
    private String keyword;
    /**
     * 三级分类Id
     */
    private Long catalog3Id;

    /**
     * 排序: 销量，评分，价格，（升降序）
     */
    private String sort;

    /**
     * 是否有货（默认为有库存）
     */
    private Integer hasStock;

    /**
     * sku价格区间
     */
    private String skuPrice;

    /**
     * 品牌标识：允许多选
     */
    private List<Long> brandId;

    /**
     * 商品属性：允许多选
     */
    private List<String> attrs;

    /**
     * 分页页码（默认初始第一页）
     */
    private Integer pageNum = 1;

}

