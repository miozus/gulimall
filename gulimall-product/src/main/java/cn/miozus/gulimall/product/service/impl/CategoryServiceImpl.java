package cn.miozus.gulimall.product.service.impl;

import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.gulimall.product.dao.CategoryDao;
import cn.miozus.gulimall.product.entity.CategoryEntity;
import cn.miozus.gulimall.product.service.CategoryBrandRelationService;
import cn.miozus.gulimall.product.service.CategoryService;
import cn.miozus.gulimall.product.vo.Catalog2Vo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 类别服务impl
 *
 * @author miao
 * @date 2021/10/05
 */
@Service("categoryService")
@Slf4j
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    private Map<String, Object> cache = new HashMap<>();

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        // 查出所有分类
        List<CategoryEntity> entities = baseMapper.selectList(null);

        // 组装成树形结构
        return entities.stream()
                // catId [1,21]
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .map(menu -> {
                    menu.setChildren(getChildren(menu, entities));
                    return menu;
                })
                .sorted(Comparator.comparingInt(o -> (o.getSort() == null ? 0 : o.getSort())))
                .collect(Collectors.toList());

    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        // todo: check params
        // 逻辑删除（某字段标志显示与否0，1）；物理删除：数据库删除记录
        baseMapper.deleteBatchIds(asList);

    }

    /**
     * 找到catalog路径, eg.[2,25,225]
     *
     * @param catalogId catalog id
     * @return {@link Long[]}
     */
    @Override
    public Long[] findCatalogPath(Long catalogId) {
        List<Long> paths = new ArrayList<>();

        List<Long> parentPath = findParentPath(catalogId, paths);
        Collections.reverse(parentPath);

        return parentPath.toArray(new Long[0]);
    }

    /**
     * 级联更新：所有关联的数据
     *
     * @param category 类别
     */
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    @Override
    public List<CategoryEntity> getLevel1Categories() {
        return baseMapper.selectList(
                new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    /**
     * 获取目录json
     *
     * @return {@link Map}<{@link Integer}, {@link Object}>
     */
    @Override
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        // 1️⃣ 优化：将数据库的查询由三次，降低为一次查全表储存在在一个集合中，多次复用
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        // 封装数据
        // 一级分类：parent_cid 统一查询继承关系
        List<CategoryEntity> firstCategories = getParentCid(categoryEntities, 0L);
        return firstCategories.stream().collect(Collectors.toMap(key -> key.getCatId().toString(), value -> {
            // 二级分类：每个一级分类，查到其下的二级分类
            List<CategoryEntity> secondCategories = getParentCid(categoryEntities, value.getCatId());
            List<Catalog2Vo> catalog2Vos = null;
            if (CollectionUtils.isNotEmpty(secondCategories)) {
                catalog2Vos = secondCategories.stream().map(secondCategory -> {
                            // 简化名字，同类型中强调区分; 或者直接匿名 item ，但嵌套容易混同；
                            // 全参构造，简化拷贝值
                            Catalog2Vo catalog2Vo = new Catalog2Vo(
                                    value.getCatId().toString(),
                                    // 占位可用""红下划线的提醒，或者null不提醒，先写其他的; 善用zc zo折叠或展开代码；
                                    // 超越时空的收集？ > 先赋值null ，最后用 set 补刀；
                                    null,
                                    secondCategory.getCatId().toString(),
                                    secondCategory.getName()
                            );
                            // 三级分类
                            List<CategoryEntity> thirdCategories = getParentCid(
                                    categoryEntities, secondCategory.getCatId()
                            );
                            if (CollectionUtils.isNotEmpty(thirdCategories)) {
                                List<Catalog2Vo.Catalog3Vo> catalog3Vos = thirdCategories.stream().map(
                                        thirdCategory -> new Catalog2Vo.Catalog3Vo(
                                                secondCategory.getCatId().toString(),
                                                thirdCategory.getCatId().toString(),
                                                thirdCategory.getName()
                                        )).collect(Collectors.toList());
                                catalog2Vo.setCatalog3List(catalog3Vos);
                            }
                            return catalog2Vo;
                        }
                ).collect(Collectors.toList());
            }
            // 抽取变量时，生成的复杂嵌套类型，此时可用来修改接口类型、实体类了 Object -> xxx
            return catalog2Vos;
        }));
    }

    /**
     * 从已知集合内挑出 Cid
     *
     * @param categories 类别
     * @param parentCid  父母cid
     * @return {@link List}<{@link CategoryEntity}>
     */
    private List<CategoryEntity> getParentCid(List<CategoryEntity> categories, Long parentCid) {
        return categories.stream()
                .filter(category -> Objects.equals(category.getParentCid(), parentCid))
                .collect(Collectors.toList());
    }

    /**
     * 找到父路径
     * eg.[225, 25, 2]
     *
     * @param catlogId catlog id
     * @param paths    路径
     * @return {@link List}<{@link Long}>
     */
    private List<Long> findParentPath(Long catlogId, List<Long> paths) {
        // current node > parent node
        Long parentId;
        paths.add(catlogId);

        CategoryEntity byId = this.getById(catlogId);
        if ((parentId = byId.getParentCid()) != 0) {
            findParentPath(parentId, paths);
        }
        return paths;
    }

    /**
     * 递归查找所有菜单的子菜单
     *
     * @param root 根
     * @param all  所有
     * @return {@link List}<{@link CategoryEntity}>
     */
    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {

        return all.stream()
                // catId [1,21]
                .filter(categoryEntity -> Objects.equals(categoryEntity.getParentCid(), root.getCatId()))
                // 递归找子菜单，二级的 catId 同时转化成三级的 parentCid
                .map(categoryEntity -> {
                    categoryEntity.setChildren(getChildren(categoryEntity, all));
                    return categoryEntity;
                })
                // 菜单排序
                .sorted(Comparator.comparingInt(o -> (o.getSort() == null ? 0 : o.getSort())))
                //                .sorted((o1, o2) -> (o1.getSort() == null ? 0 : o1.getSort()) - (o2.getSort() == null ? 0 : o2.getSort()))
                .collect(Collectors.toList());
    }
}