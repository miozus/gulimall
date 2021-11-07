package cn.miozus.gulimall.search.service.impl;

import cn.miozus.common.to.es.SkuEsModel;
import cn.miozus.gulimall.search.config.ElasticsearchConfig;
import cn.miozus.gulimall.search.constant.EsConstant;
import cn.miozus.gulimall.search.service.MallSearchService;
import cn.miozus.gulimall.search.vo.SearchParam;
import cn.miozus.gulimall.search.vo.SearchResult;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    @Override
    public SearchResult search(SearchParam param) {
        // 1️⃣ 动态构建需要查询的 DSL 语句
        SearchResult result;

        // 2️⃣ 拼装 ES 检索请求
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
     * 建立搜索结果
     *
     * @param response 响应
     * @return {@link SearchResult}
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {
        SearchResult result = new SearchResult();
        // 1️⃣ 返回所有查询的商品：查询有记录 NPE
        SearchHits hits = response.getHits();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            List<SkuEsModel> products = Arrays.stream(hits.getHits()).map(hit -> {
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
            result.setProducts(products);
        }
        Aggregations aggs = response.getAggregations();
        // 2️⃣ 聚合分类 : 子聚合，一定要手动强制转换（long/string），才能继续访问内部属性；关联属性只有一个，可以从索引获取key（如名字）
        ParsedLongTerms catalogAgg = aggs.get("catalog_agg");
        List<SearchResult.CatalogVo> catalogs = catalogAgg.getBuckets().stream().map(bucket -> {
            SearchResult.CatalogVo vo = new SearchResult.CatalogVo();
            vo.setCatalogId(bucket.getKeyAsNumber().longValue());
            vo.setCatalogName(getAggBindKey(bucket, "catalog_name"));
            return vo;
        }).collect(Collectors.toList());
        result.setCatalogs(catalogs);
        // 3️⃣ 聚合品牌
        ParsedLongTerms brandAgg = aggs.get("brand_agg");
        List<SearchResult.BrandVo> brands = brandAgg.getBuckets().stream().map(bucket -> {
            SearchResult.BrandVo vo = new SearchResult.BrandVo();
            vo.setBrandId(bucket.getKeyAsNumber().longValue());
            vo.setBrandName(getAggBindKey(bucket, "brand_name"));
            vo.setBrandImg(getAggBindKey(bucket, "brand_img"));
            return vo;
        }).collect(Collectors.toList());
        result.setBrands(brands);
        // 4️⃣ 聚合属性
        ParsedNested attrAgg = aggs.get("attr_agg");
        ParsedLongTerms attrIdAgg = attrAgg.getAggregations().get("attr_id");
        List<SearchResult.AttrsVo> attrs = attrIdAgg.getBuckets().stream().map(bucket -> {
            SearchResult.AttrsVo vo = new SearchResult.AttrsVo();
            ParsedStringTerms attrValueAgg = bucket.getAggregations().get("attr_value_agg");
            List<String> attrValues = attrValueAgg.getBuckets().stream().map(
                    MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList());
            vo.setAttrId(bucket.getKeyAsNumber().longValue());
            vo.setAttrName(getAggBindKey(bucket, "attr_name_agg"));
            vo.setAttrValue(attrValues);
            return vo;
        }).collect(Collectors.toList());
        result.setAttrs(attrs);
        // 5️⃣ 分页: 总记录数，共计页数（计算），当前页码
        long total = hits.getTotalHits().value;
        int totalPages = (int) (total - 1) / EsConstant.PRODUCT_PAGESIZE + 1;
        result.setTotal(total);
        result.setTotalPages(totalPages);
        result.setPageNum(param.getPageNum());
        return result;
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
        if (!Objects.isNull(param.getCatalog3Id())) {
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
        // 1.2.4 库存：0 无，1 有（可设置默认值1, 一般查有库存的）
        if (!Objects.isNull(param.getHasStock())) {
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
        if (!Objects.isNull(param.getPageNum())) {
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
