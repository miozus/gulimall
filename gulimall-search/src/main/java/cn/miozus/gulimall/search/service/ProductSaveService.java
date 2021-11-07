package cn.miozus.gulimall.search.service;

import cn.miozus.common.to.es.SkuEsModel;

import java.io.IOException;
import java.util.List;

/**
 * 搜索服务
 *
 * @author miao
 * @date 2021/10/02
 */
public interface ProductSaveService {

    /**
     * 发布
     *
     * @param skuEsModels sku es模型
     * @throws IOException ioexception
     */
    boolean publish(List<SkuEsModel> skuEsModels) throws IOException;
}
