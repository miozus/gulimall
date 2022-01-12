package cn.miozus.gulimall.search.service.impl;

import cn.miozus.common.to.es.SkuEsModel;
import cn.miozus.common.utils.R;
import cn.miozus.gulimall.search.config.ElasticsearchConfig;
import cn.miozus.gulimall.search.constant.EsConstant;
import cn.miozus.gulimall.search.feign.ProductFeignService;
import cn.miozus.gulimall.search.service.MallSearchService;
import cn.miozus.gulimall.search.vo.AttrResponseVo;
import cn.miozus.gulimall.search.vo.SearchParam;
import cn.miozus.gulimall.search.vo.SearchResult;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.alibaba.nacos.common.utils.Objects;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 商城搜索服务impl
 */

/**
 * 商城搜索服务impl
 *
 * @author miao
 * @date 2021/10/23
 */
@Service
public class MallSearchServiceImpl implements MallSearchService {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Override
    public SearchResult search(SearchParam param) {
        // 1️⃣ 动态构建需要查询的 DSL 语句
        SearchResult result;

        // 2️⃣ 拼装 ES 检索请求（RestFul）：复杂的 JSON 体
        SearchRequest request = buildSearchRequest(param);
        try {
            SearchResponse response = client.search(request, ElasticsearchConfig.COMMON_OPTIONS);
            System.out.println("response = " + response);
            // 3️⃣ 分析响应数据，封装格式
            result = buildSearchResult(response, param);
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 建立搜索结果：属性拼接成地址
     *
     * @param response 响应
     * @return {@link SearchResult}
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {

        SearchResult result = new SearchResult();
        SearchHits hits = response.getHits();
        Aggregations aggs = response.getAggregations();

        List<SkuEsModel> products = parseEsProducts(hits, param);
        List<SearchResult.CatalogVo> catalogs = parseEsCatalogVos(aggs);
        List<SearchResult.BrandVo> brands = parseEsBrandVos(aggs);
        List<SearchResult.AttrsVo> attrs = parseEsAttrsVos(aggs);
        List<SearchResult.NavVo> attrNavs = createBreadNavForAttr(param, result);
        attrNavs = addBreadNavForBrand(attrNavs, param, brands);


        result.setProducts(products);
        result.setCatalogs(catalogs);
        result.setBrands(brands);
        result.setAttrs(attrs);
        result.setNavs(attrNavs);
        saveEsPage(param, result, hits);

        // TODO: 8️⃣ 面包屑导航III 分类：不要导航

        return result;
    }

    private List<Long> copyAttrProperty(List<SearchResult.AttrsVo> attrs) {
        return attrs.stream().map(SearchResult.AttrsVo::getAttrId).collect(Collectors.toList());
    }

    /**
     * 7️⃣ 面包屑导航II 品牌：点击后从检索状态区域中消失 （采用将就查询）
     *
     * @param param  param
     * @param brands 品牌
     * @return
     */
    private List<SearchResult.NavVo> addBreadNavForBrand(List<SearchResult.NavVo> nav, SearchParam param, List<SearchResult.BrandVo> brands) {
        if (CollectionUtils.isNotEmpty(param.getBrandId()) ) {
            SearchResult.NavVo vo = new SearchResult.NavVo();
            AtomicReference<String> replace = new AtomicReference<>("");
            String brandNames = brands.stream()
                    .filter(brand -> param.getBrandId().contains(brand.getBrandId()))
                    .map(brand -> {
                        replace.set(parseQueryString(String.valueOf(brand.getBrandId()), "brandId"));
                        return brand.getBrandName();
                    })
                    .reduce("", (partialString, element) -> partialString + ";" + element);
            vo.setNavValue(brandNames);
            vo.setNavName("品牌");
            vo.setNavLink("http://search.gulimall.com/search.html?" + replace);
            if (CollectionUtils.isNotEmpty(nav)){
                nav.add(vo);
            } else {
                nav = Arrays.asList(vo);
            }
        }
        return nav;
    }

    /**
     * 6️⃣ 面包屑导航I 记录和撤回搜索词条
     * （仅限检索属性，attrs=1_其他:安卓&attrs=2_5寸:6寸。不包括关键字，排序，页码，分类等）
     * <p>
     * vo.setNavName(split[0]); // 💡 属性是数字，名字提高可读性
     * b.将就刚才储存的冗余结果，遍历获取 👍
     * c.前端渲染和地址跳转
     * d.重新发请求给 ES 查询 👎（内部解决更快）
     * a.跨服务耦合! 查表 （接下来复习一遍）
     * R 封装了方法可以转换 JSON 格式，实在找不到才用数字
     *
     * @param param param
     * @param result
     * @return {@link List}
     * @see List
     * @see SearchResult.NavVo
     */
    private List<SearchResult.NavVo> createBreadNavForAttr(SearchParam param, SearchResult result) {

        if (CollectionUtils.isNotEmpty(param.getAttrs())) {
            return param.getAttrs().stream().map(attr -> {
                SearchResult.NavVo vo = new SearchResult.NavVo();
                String[] split = attr.split("_");
                vo.setNavValue(split[1]);
                R r = productFeignService.attrInfo(Long.valueOf(split[0]));
                result.getAttrIds().add(Long.valueOf(split[0]));
                if (r.getCode() == 0) {
                    AttrResponseVo data = r.getData("attr", new TypeReference<AttrResponseVo>() {
                    });
                    vo.setNavName(data.getAttrName());
                } else {
                    vo.setNavName(split[0]);
                }
                String replace = parseQueryString(attr, "attrs");
                vo.setNavLink("http://search.gulimall.com/search.html?" + replace);
                return vo;
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 5️⃣ 分页: 总记录数，共计页数（计算），当前页码
     *
     * @param param  param
     * @param result 结果
     * @param hits   支安打
     */
    private void saveEsPage(SearchParam param, SearchResult result, SearchHits hits) {
        long total = hits.getTotalHits().value;
        int totalPages = (int) (total - 1) / EsConstant.PRODUCT_PAGESIZE + 1;
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i < totalPages; i++) {
            pageNavs.add(i);
        }
        result.setTotal(total);
        result.setTotalPages(totalPages);
        result.setPageNum(param.getPageNum());
        result.setPageNavs(pageNavs);
    }

    /**
     * 4️⃣ 聚合属性
     *
     * @param aggs 聚合
     * @return {@link List}
     * @see List
     * @see SearchResult.AttrsVo
     */
    private List<SearchResult.AttrsVo> parseEsAttrsVos(Aggregations aggs) {
        ParsedNested attrAgg = aggs.get("attr_agg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attr_id");
        return attrIdAgg.getBuckets().stream().map(bucket -> {
            SearchResult.AttrsVo vo = new SearchResult.AttrsVo();
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attr_value_agg");
            Long attrIds = bucket.getKeyAsNumber().longValue();
            String attrName = getAggBindKey(bucket, "attr_name_agg");
            List<String> attrValues = attrValueAgg.getBuckets().stream().map(
                    MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList());
            vo.setAttrId(attrIds);
            vo.setAttrName(attrName);
            vo.setAttrValue(attrValues);
            return vo;
        }).collect(Collectors.toList());
    }

    private List<SearchResult.BrandVo> parseEsBrandVos(Aggregations aggs) {
        // 3️⃣ 聚合品牌
        ParsedLongTerms brandAgg = aggs.get("brand_agg");
        return brandAgg.getBuckets().stream().map(bucket -> {
            SearchResult.BrandVo vo = new SearchResult.BrandVo();
            vo.setBrandId(bucket.getKeyAsNumber().longValue());
            vo.setBrandName(getAggBindKey(bucket, "brand_name"));
            vo.setBrandImg(getAggBindKey(bucket, "brand_img"));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 2️⃣ 聚合分类 : 子聚合，一定要手动强制转换（long/string），才能继续访问内部属性；
     * 关联属性只有一个，可以从索引获取key（如名字）
     *
     * @param aggs 聚合
     * @return {@link List}
     * @see List
     * @see SearchResult.CatalogVo
     */
    private List<SearchResult.CatalogVo> parseEsCatalogVos(Aggregations aggs) {

        ParsedLongTerms catalogAgg = aggs.get("catalog_agg");
        return catalogAgg.getBuckets().stream().map(bucket -> {
            SearchResult.CatalogVo vo = new SearchResult.CatalogVo();
            vo.setCatalogId(bucket.getKeyAsNumber().longValue());
            vo.setCatalogName(getAggBindKey(bucket, "catalog_name"));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 1️⃣ 返回所有查询的商品：查询有记录 NPE
     *
     * @param hits  ES 查询结果
     * @param param param
     * @return {@link List}
     * @see List
     * @see SkuEsModel
     */
    private List<SkuEsModel> parseEsProducts(SearchHits hits, SearchParam param) {
        if (hits.getHits() != null && hits.getHits().length > 0) {
            return Arrays.stream(hits.getHits()).map(hit -> {
                SkuEsModel skuEsModel = new SkuEsModel();
                SkuEsModel esModel = JSON.parseObject(hit.getSourceAsString(), SkuEsModel.class);
                BeanUtils.copyProperties(esModel, skuEsModel);
                // 💡 语法高亮
                if (StringUtils.isNotEmpty(param.getKeyword())) {
                    String skuTitle = hit.getHighlightFields().get("skuTitle").getFragments()[0].string();
                    skuEsModel.setSkuTitle(skuTitle);
                }
                return skuEsModel;
            }).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 解析查询字符串
     * <p>
     * 编码不一致问题：中文编码，浏览器对空格的编码和Java不同（加号）
     * a.统一不彻底：包括 httpServletRequest 全部编码 👎，在前端全部是%字符串，所有匹配规则都失效了。
     * b.打补丁：encode 编码后，再翻译回去，httpServletRequest 能识别 ⇒ 匹配规则继续生效
     *
     * @param param 原文
     * @param key   关键词
     * @return {@link String} 替换后 URI 请求参数
     * @see String
     */
    private String parseQueryString(String param, String key) {
        String encode = null;
        try {
            encode = URLEncoder.encode(param, "UTF-8");
            encode = encode.replace("%3B", ";").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return httpServletRequest.getQueryString().replace("&" + key + "=" + encode, "");
    }

    /**
     * 得到聚合的绑定索引为 0 的关键字
     *
     * @param bucket  桶
     * @param aggName 桶内聚合的自定义名字
     * @return {@link String} 索引 0 的 key
     */
    private String getAggBindKey(Terms.Bucket bucket, String aggName) {
        ParsedStringTerms nameAgg = bucket.getAggregations().get(aggName);
        return nameAgg.getBuckets().get(0).getKeyAsString();
    }

    /**
     * 建立搜索请求
     * 因为检索语句方法特别长，业务逻辑应该预留方法位置
     *
     * @return {@link SearchRequest}
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        // 构建 DSL 语句：不断拆分
        SearchSourceBuilder source = new SearchSourceBuilder();
        // 模糊匹配，过滤（按照 属性，分类，品牌，价格区间，库存）
        // 1 外围逻辑很大，将它拿出来，再放进去
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 1.1
        if (StringUtils.isNotEmpty(param.getKeyword())) {
            boolQuery.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        // 1.2.1
        if (Objects.nonNull(param.getCatalog3Id())) {
            boolQuery.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        // 1.2.2
        if (CollectionUtils.isNotEmpty(param.getBrandId())) {
            boolQuery.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        // 1.2.3 内嵌写法 attrs （复杂的放后面，先完成简单的，赶时间）
        if (CollectionUtils.isNotEmpty(param.getAttrs())) {
            // attrs=1_其他:安卓&attrs=2_5寸:6寸
            for (String attr : param.getAttrs()) {
                // attrs=1_其他:安卓
                // 没有一种商品可以同时满足多个条件，所以查询语句应该放在遍历内
                BoolQueryBuilder nestedBoolQuery = QueryBuilders.boolQuery();
                String[] s = attr.split("_");
                String attrId = s[0];
                String[] attrValue = s[1].split(":");
                // 不能一个must解决，就用两个完成
                nestedBoolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                nestedBoolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValue));
                // 每一个必须生成一个 nested 查询
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", nestedBoolQuery, ScoreMode.None);
                boolQuery.filter(nestedQuery);
            }
        }
        // 1.2.4 库存：0 无，1 有（可设置默认值1, 一般查有库存的; 还有第三种情况 0、1 都查）
        if (Objects.nonNull(param.getHasStock())) {
            boolQuery.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }
        // 1.2.5 价格区间，分情况 range ，  skuPrice=1_500/_500/500_
        if (StringUtils.isNotEmpty(param.getSkuPrice())) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 1) {
                // 500_ [500]
                rangeQuery.gte(s[0]);
            } else if ("".equalsIgnoreCase(s[0])) {
                // _500  [, 500]
                rangeQuery.lte(s[1]);
            } else {
                // 1_500 [0, 500]
                rangeQuery.gte(s[0]).lte(s[1]);
            }
            boolQuery.filter(rangeQuery);
        }
        source.query(boolQuery);
        // 2 排序 sort=SkuPrice_asc
        if (StringUtils.isNotEmpty(param.getSort())) {
            String[] s = param.getSort().split("_");
            source.sort(s[0], SortOrder.fromString(s[1]));
        }
        // 3 分页: 从全局索引计算 (第 N 页 - 1 ) * size = from
        if (Objects.nonNull(param.getPageNum())) {
            source.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
            source.size(EsConstant.PRODUCT_PAGESIZE);
        }
        // 4 高亮 : 输入关键字才有高亮
        if (StringUtils.isNotEmpty(param.getKeyword())) {
            HighlightBuilder builder = new HighlightBuilder();
            builder.field("skuTitle");
            builder.preTags("<b style='color:red'>");
            builder.postTags("</b>");
            source.highlighter(builder);
        }
        // 聚合分析
        // 品牌聚合： aggs[ terms, aggs ] ⇒ terms.subAggregation
        TermsAggregationBuilder brandAgg = AggregationBuilders.terms("brand_agg").field("brandId").size(50);
        brandAgg.subAggregation(AggregationBuilders.terms("brand_name").field("brandName").size(1));
        brandAgg.subAggregation(AggregationBuilders.terms("brand_img").field("brandImg").size(1));
        source.aggregation(brandAgg);
        // 分类聚合
        TermsAggregationBuilder catalogAgg = AggregationBuilders.terms("catalog_agg").field("catalogId");
        catalogAgg.subAggregation(AggregationBuilders.terms("catalog_name").field("catalogName").size(20));
        source.aggregation(catalogAgg);
        // 属性聚合：子聚合应该单独写，再两次封装
        NestedAggregationBuilder attrAgg = AggregationBuilders.nested("attr_agg", "attrs");
        TermsAggregationBuilder termsAttrAgg = AggregationBuilders.terms("attr_id").field("attrs.attrId");
        termsAttrAgg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        termsAttrAgg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        attrAgg.subAggregation(termsAttrAgg);
        source.aggregation(attrAgg);
        // 根据网页传来的请求参数构建
        // @Test 查看DSL 语句、返回结果是否正常（Postman & Param & SearchParam）
        System.out.println("source = " + source);
        return new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, source);
    }
}
