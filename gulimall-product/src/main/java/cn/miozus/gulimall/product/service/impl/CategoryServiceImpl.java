package cn.miozus.gulimall.product.service.impl;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;

import cn.miozus.gulimall.product.dao.CategoryDao;
import cn.miozus.gulimall.product.entity.CategoryEntity;
import cn.miozus.gulimall.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
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
     * 找到catelog路径, eg.[2,25,225]
     *
     * @param catelogId catelog id
     * @return {@link Long[]}
     */
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();

        List<Long> parentPath = findParentPath(catelogId, paths);
        Collections.reverse(parentPath);

        return (Long[]) parentPath.toArray(new Long[0]);
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

    // 递归查找所有菜单的子菜单
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