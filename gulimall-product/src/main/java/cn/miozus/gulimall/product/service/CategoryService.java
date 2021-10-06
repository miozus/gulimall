package cn.miozus.gulimall.product.service;

import cn.miozus.gulimall.product.vo.Catalog2Vo;
import com.baomidou.mybatisplus.extension.service.IService;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.gulimall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 目录服务
 * 商品三级分类
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-06 23:57:18
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    /**
     * 删除菜单由ids
     *
     * @param asList 正如列表
     */
    void removeMenuByIds(List<Long> asList);

    /**
     * 找到catalogId路径 [parent/child/grandchild]
     *
     * @param catalogId catalog id
     * @return {@link Long[]}
     */
    Long[] findCatalogPath(Long catalogId);

    /**
     * 级联更新
     *
     * @param category 类别
     */
    void updateCascade(CategoryEntity category);

    /**
     * 会使类别
     * @return
     */
    List<CategoryEntity> getLevel1Categories();

    Map<String, List<Catalog2Vo>> getCatalogJson();
}

