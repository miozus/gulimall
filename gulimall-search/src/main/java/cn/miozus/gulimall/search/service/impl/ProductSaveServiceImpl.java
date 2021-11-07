package cn.miozus.gulimall.search.service.impl;

import cn.miozus.common.to.es.SkuEsModel;
import cn.miozus.gulimall.search.config.ElasticsearchConfig;
import cn.miozus.gulimall.search.constant.EsConstant;
import cn.miozus.gulimall.search.service.ProductSaveService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 产品保存服务impl
 *
 * @author miao
 * @date 2021/10/02
 */
@Service
@Slf4j
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    RestHighLevelClient client;

    @Override
    public boolean publish(List<SkuEsModel> skuEsModels) throws IOException {

        /*
        NOTE：
         es 前置事务:
             建立索引，经常用，所以抽取成常量，放在自己服务里面；
             建立映射关系，（已建好）；
         面向接口编程风格：
             bulk 接口参数 > 调用方法 client.bulk  > 创建构造类 new > 查看类的方法及形参 add(...) > 找素材实现细节；
             形参 接口参数 > 循环上述步骤 🔁
         */
        // 批量保存（逐个保存太慢）
        BulkRequest bulkRequest = new BulkRequest();
        for (SkuEsModel model : skuEsModels) {
            // 构造保存请求
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(model.getSkuId().toString());
            String jsonString = JSON.toJSONString(model);
            indexRequest.source(jsonString, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        BulkResponse response = client.bulk(bulkRequest, ElasticsearchConfig.COMMON_OPTIONS);
        // 统计分析返回结果
        // TODO：错误的处理，还需人工检查，或重新上架几次（可能原因：es提交格式与映射不符合）
        boolean hasFailures = response.hasFailures();
        //if (!hasFailures) {
            List<String> collect = Arrays.stream(response.getItems())
                    .map(BulkItemResponse::getId).collect(Collectors.toList());
            log.info("商品上架完成:{}, 返回数据{}", collect, response);
        //}

        return hasFailures;
    }
}
