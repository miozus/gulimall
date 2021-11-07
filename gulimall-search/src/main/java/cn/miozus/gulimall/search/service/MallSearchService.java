package cn.miozus.gulimall.search.service;

import cn.miozus.gulimall.search.vo.SearchParam;
import cn.miozus.gulimall.search.vo.SearchResult;

/**
 * 商城搜索服务
 *
 * @author miao
 * @date 2021/10/23
 */
public interface MallSearchService {
    /**
     * 搜索
     *
     * @param param 所有参数
     * @return {@link Object} 检索结果，页面需要的所有信息
     */
    SearchResult search(SearchParam param);
}
