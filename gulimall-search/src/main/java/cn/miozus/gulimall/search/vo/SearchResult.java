package cn.miozus.gulimall.search.vo;

import cn.miozus.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索响应数据
 *
 * @author miao
 * @date 2021/10/23
 */
@Data
public class SearchResult {

    /** 查询到所有的商品信息 */
    private List<SkuEsModel> products;

    /** 分页查询 */
    private Long total;
    private Integer pageNum;
    private Integer totalPages;
    private List<Integer> pageNavs;

    /** 品牌：当前查询结果，头部展示栏，涉及所有品牌 */
    private List<AttrsVo> attrs;
    private List<BrandVo> brands;
    private List<CatalogVo> catalogs;

    /** 面包屑导航 */
    private List<NavVo> navs = new ArrayList<>();
    /** 记录已经使用过的属性值: 集合判断是否包含 */
    private List<Long> attrIds = new ArrayList<>();

    // =========== 以上返给页面 ===============

    @Data
    public static class NavVo {
        private String navName;
        private String navValue;
        private String navLink;
    }

    /**
     * sku es attrs
     * public 可访问权限，则可序列化
     * 都是查询得到的属性值，符合检索条件查出来的所有商品（不可能有属性但没有货）
     */
    @Data
    public static class AttrsVo {
        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }

    @Data
    public static class BrandVo {
        private Long brandId;
        private String brandName;
        private String brandImg;
    }

    @Data
    public static class CatalogVo {
        private Long catalogId;
        private String catalogName;
    }

}
