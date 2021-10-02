package cn.miozus.gulimall.search.config;

import com.alibaba.fastjson.JSON;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.metrics.Avg;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ElasticsearchConfigTest {

    @Autowired
    private RestHighLevelClient client;

    @Value("${elasticsearch.host}")
    public String esHost;
    @Value("${elasticsearch.port}")
    public int esPort;

    @Test
    public void contextLoads() {
        System.out.println("client = " + client); // null 说明未检测到，需要加注解
    }

    @Test
    public void printEsVal() {
        System.out.println("esHost = " + esHost);
        System.out.println("esPort = " + esPort);
    }

    @Test
    public void printValue() {
    }

    /**
     * 插入数据
     */
    @Test
    public void indexData() {
        IndexRequest request = new IndexRequest("posts");
        request.id("1");
        String jsonString = "{" +
                "\"user\":\"kimchy\"," +
                "\"postDate\":\"2013-01-30\"," +
                "\"message\":\"trying out Elasticsearch\"" +
                "}";
        request.source(jsonString, XContentType.JSON);
    }

    @Test
    public void searchData() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        // 指定索引
        searchRequest.indices("bank");
        // 构造检索条件 Query DSL
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("address", "mill"))
                .aggregation(AggregationBuilders.terms("terms_age").field("age").size(10))
                .aggregation(AggregationBuilders.avg("avg_age").field("age"))
                .aggregation(AggregationBuilders.avg("avg_balance").field("balance"));
        //.size(0);
        System.out.println("sourceBuilder = " + sourceBuilder);
        searchRequest.source(sourceBuilder);
        // 返回结果
        SearchResponse response = client.search(searchRequest, ElasticsearchConfig.COMMON_OPTIONS);
        // 分析结果
        System.out.println("response = " + response);
        // 获取所有查到的具体数据
        SearchHits hits = response.getHits();
        SearchHit[] searchHits = hits.getHits();
        for (SearchHit hit : searchHits) {
            // hit.getIndex();hit.getType();hit.getId();  // 支持多种操作
            Account account = JSON.parseObject(hit.getSourceAsString(), Account.class);
            System.out.println("account = " + account);
        }
        // 获取这次检索到的分析结果(聚合)
        Aggregations aggregations = response.getAggregations();
        Terms termsAgeAggregation = aggregations.get("terms_age"); // 转换后的多态类型可进去查看 继承关系，基本都有对应
        for (Terms.Bucket bucket : termsAgeAggregation.getBuckets()) {
            String keyAsString = bucket.getKeyAsString();
            System.out.println("keyAsString = " + keyAsString + "->" + bucket.getDocCount());
        }
        Avg avgBalanceAggregation = aggregations.get("avg_balance");
        System.out.println("avgBalanceAggregation = " + avgBalanceAggregation.getValue());
    }

    @Test
    public void indexKV() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1");
        User user = new User(); // > JSON
        user.setName("李莫愁");
        user.setGender("🚹");
        user.setAge(22);
        String jsonString = JSON.toJSONString(user);
        indexRequest.source(jsonString, XContentType.JSON);
        // 执行保存操作
        IndexResponse index = client.index(indexRequest, ElasticsearchConfig.COMMON_OPTIONS);
        // 提取有效响应数据
        System.out.println("index = " + index);
    }

    @Data
    class User {
        private String name;
        private String gender;
        private Integer age;

    }

    @Data
    static class Account {

        private int account_number;
        private int balance;
        private String firstname;
        private String lastname;
        private int age;
        private String gender;
        private String address;
        private String employer;
        private String email;
        private String city;
        private String state;
    }
}
