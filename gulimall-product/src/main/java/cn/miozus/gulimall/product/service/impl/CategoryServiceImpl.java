package cn.miozus.gulimall.product.service.impl;

import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.gulimall.product.dao.CategoryDao;
import cn.miozus.gulimall.product.entity.CategoryEntity;
import cn.miozus.gulimall.product.service.CategoryBrandRelationService;
import cn.miozus.gulimall.product.service.CategoryService;
import cn.miozus.gulimall.product.vo.Catalog2Vo;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
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

    @Autowired
    StringRedisTemplate redis;

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    RedissonClient redisson;

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
     * @annotation @CacheEvict ☠ 失效模式：改库后，缓存也删除
     * @annotation @CacheEvict(value="category", allEntries = true) 分区的好处，清空同类型的数据
     * @annotation @Caching(evict = { @xxx,@xxx } 批量操作
     * @param category 类别
     */
    @Override
    @CacheEvict(value="category", allEntries = true)
    @Transactional
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    /**
     * 首页一级分类
     *
     * @return {@link List}
     * @see List
     * @see CategoryEntity
     * @annotation Cacheable 当前方法返回的结果需要缓存：如果缓存中有，方法不调用，否则调用方法，将结果放入缓存（类似读模式）
     *                      每个需要缓存的数据，都要指定名字[缓存的分区（业务类型）]
     *            默认行为
     *              * 自动生成 key := category::SimpleKey []
     *              * 使用JDK序列化缓存的值 value
     *              * 过期时间永不过期（-1）
     *
     *            客制化（需求）
     *              * 指定生成的缓存使用 key （支持SpEL 一个值就用单引号）
     *              * 指定数据的过期时间 ttl （nacos 秒）
     *              * 保存为 JSON 通用格式
     */
    @Override
    @Cacheable(value={"category"}, key="#root.methodName", sync = true)
    public List<CategoryEntity> getLevel1Categories() {
        System.out.println("📦 getLevel1Categories works: add cache");
        return baseMapper.selectList(
                new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    @Override
    @Cacheable(value={"category"}, key="#root.methodName")
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
            return getCatalogJsonFromDbWithSprigCache();
    }

    private Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithSprigCache() {
        return  getCatalogFromDb();
    }


    /**
     * 分布式缓存逻辑
     *
     * @return {@link Map}<{@link Integer}, {@link Object}>
     */
    public Map<String, List<Catalog2Vo>> getCatalogJsonRedis() {
        // 2️⃣ 优化：分布式缓存，共享一个缓存数据源，先看有没有，没有再查库（变相将查询次数降至0）
        // 2️⃣+ 强化：分布式缓存逻辑，应对穿透、雪崩、击穿三大问题
        // 👻 保存虚无，冷却时间
        // ❄ 随机时间
        // 🔒 锁住第一个吃螃蟹的人
        String catalogJson = redis.opsForValue().get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            System.out.println("缓存未命中，将要查询数据库");
            // 🔒 锁住第一个吃螃蟹的人 (锁，放进查询逻辑）
            return getCatalogJsonFromDbWithRedisLock();
        }
        // 序列化与反序列化: 缓存中储存后，读取 JSON 字符串，再逆转为能用的对象，两者相互转化
        // TypeReference 类型构造器是受保护的，使用匿名实现类转化
        System.out.println("缓存命中，直接返回结果");
        return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {
        });
    }

    /**
     * 🔒 redis 分布式锁
     *
     * @return {@link Map}
     * @see Map
     * @see String
     * @see List
     */
    private Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithRedisLock() {
        // 🔒 分布式锁
        // 如果上锁后，程序异常或者机房断电导致无法退出，坑位被占，后来请求无法占领: 死锁
        //  - 设置过期时间，自动删除锁(expire)
        //  - 但过期时间短，或业务超时，会提前把其他线程放进来，自身业务结束时删锁，把闯进来多个线程的共享锁删了：流量泄洪。
        //    - 指定唯一身份证，key = uuid， 只删自己线程的锁
        //    - 锁的自动续期（看门狗 🐕），或者设置超长的过期时间
        // ⚛️ 加锁 + 设置有效期，两者必须是一个原子操作
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redis.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lock)) {
            // 获取锁 >  执行业务
            // ⏳ 锁的自动续期 （设置超长锁时间，也可以其他）
            Map<String, List<Catalog2Vo>> catalogJson;
            try {
                // 🐞 真正查表的日志应该打在里面
                catalogJson = getCatalogJsonFromDb();
            } finally {
                // 解锁: 核对身份证，只删除第一个吃螃蟹的人的锁
                // 但查询 key，发送和返回的时间，是否赶得上锁的自动失效期，走一半路，赶不上：
                //   - 共享值，已经更新成别的人身份证，手上的是失效前的，判断为真，却删除了被更新过的、别人的锁；
                //   ⚛️ 获取值对比 + 对比成功删除，两者必须是一个原子操作
                String lua = "if redis.call('get',KEYS[1]) == ARGV[1] then " +
                        "return redis.call('del',KEYS[1]); " +
                        "else " +
                        "return 0; " +
                        "end; ";
                Long lock1 = redis.execute(new DefaultRedisScript<Long>(lua, Long.class), Arrays.asList("lock"), uuid);
            }
            return catalogJson;
        } else {
            System.out.println("// 未获取锁：等待 200 毫秒, 重试 (类似 synchronized 本地监听，自旋)");
            // 未获取锁：等待 200 毫秒, 重试 (类似 synchronized 本地监听，自旋)
            //sleep(200);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatalogJsonFromDbWithRedisLock();
        }
    }


    /**
     * 🔒 redisson 分布式锁(框架)
     *
     * @return {@link Map}
     * @see Map
     * @see String
     * @see List
     */
    private Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithRedissonLock() {
        // 🔒 分布式锁（框架）
        // 锁的粒度:越细越快，具体缓存的某个数据。（每个商品一把锁，避免互相干扰）
        // 缓存数据一致性
        //  👥 双写模式: 数据库和缓存，同时修改（每次要查一遍）。
        //  ☠ 失效模式: 数据库改完，删除缓存（等待下次主动查找）。
        RLock lock = redisson.getLock("catalogJson-lock");
        // 获取锁 >  执行业务
        // ⏳ 锁的自动续期 （设置超长锁时间，也可以其他）
        Map<String, List<Catalog2Vo>> catalogJson;
        try {
            // 🐞 真正查表的日志应该打在里面
            catalogJson = getCatalogJsonFromDb();
        } finally {
            lock.unlock();
        }
        return catalogJson;
    }

    /**
     * 🔒 本地同步锁
     *
     * @return {@link Map}
     * @see Map
     * @see String
     * @see List
     */
    private Map<String, List<Catalog2Vo>> getCatalogJsonFromDbWithLocalLock() {
        // 🔒 本地同步锁，同一把锁，锁住当前进程，需要这个锁的所有线程
        //    * synchronized(this){} 锁代码块
        //    * public synchronized returnType method(){} 锁方法，写法也行
        //        - 但第二个请求进来，还会继续查询数据库（应该拦截判断上一个人做的缓存是否存在，找不到再查）
        //        - 如果只锁查表，压力测试发现 2 次查表：
        //            - 1️⃣ 号第一次查表结束，先开锁，再放入缓存，此时 redis 储存数据的网络交换、初始化线程池等短时间内，假设30毫秒
        //            - 在30毫秒内，2️⃣ 号加锁进入，也未找到资源，第二次查表，开锁
        //            - 3️⃣ 号加锁进入，找到了 1️⃣ 号资源（可能此时 2️⃣ 号还没放好缓存）
        //            - 所以应该把“结果放入缓存”的逻辑，也加入锁；手动分割时序（已修复）
        //            - 查缓存 > 🔒 原子操作 [确认缓存有无，查表，结果放入缓存] > 方法结束 （最多只查 1 次表）
        //        - SpringBoot 所有组件在容器中都是单例的。百万请求同一把锁(this：当前实例对象）。
        //        - 但分布式场景，每个服务器都是一个实例，9 个服务器，就有 9 把锁（线程），同时抢占资源。
        synchronized (this) {
            return getCatalogJsonFromDb();
        }
    }

    /**
     * 数据库查询首页菜单三级分类 1 次
     * 如有缓存，直接封装返回结果
     *
     * @return {@link Map}<{@link String}, {@link List}<{@link Catalog2Vo}>>
     */
    private Map<String, List<Catalog2Vo>> getCatalogJsonFromDb() {
        // 👣 第一个请求留下的缓存痕迹，找到就不查表了
        String catalogJson = redis.opsForValue().get("catalogJson");
        if (StringUtils.isNotEmpty(catalogJson)) {
            return JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {
            });
        }
        System.out.println("🔒 查询数据库，因为缓存中没有数据");
        Map<String, List<Catalog2Vo>> catalogJsonFromDb = getCatalogFromDb();
        // JSON ：最通用的中间媒介，跨语言、跨平台兼容
        // 首次查询，缓存中放一份数据库查询结果
        String jsonString = JSON.toJSONString(catalogJsonFromDb);
        // 👻 保存虚无，冷却时间 (数量，单位）
        redis.opsForValue().set("catalogJson", jsonString, 1, TimeUnit.DAYS);
        return catalogJsonFromDb;
    }

    private Map<String, List<Catalog2Vo>> getCatalogFromDb() {
        // 1️⃣ 优化：将数据库的查询由三次，降低为一次查全表储存在在一个集合中，多次复用
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);
        // 一级分类：parent_cid 统一查询继承关系
        List<CategoryEntity> lv1s = getParentCid(categoryEntities, 0L);
        return lv1s.stream().collect(Collectors.toMap(key -> key.getCatId().toString(), lv1 -> {
            // 二级分类：每个一级分类，查到其下的二级分类
            List<CategoryEntity> lv2s = getParentCid(categoryEntities, lv1.getCatId());
            List<Catalog2Vo> catalog2Vos = null;
            if (CollectionUtils.isNotEmpty(lv2s)) {
                catalog2Vos = lv2s.stream().map(lv2 -> {
                            // 简化名字，同类型中强调区分; 或者直接匿名 item ，但嵌套容易混同；
                            // 全参构造，简化拷贝值
                            Catalog2Vo catalog2Vo = new Catalog2Vo(
                                    lv1.getCatId().toString(),
                                    // 占位可用""红下划线的提醒，或者null不提醒，先写其他的; 善用 zc zo 折叠或展开代码；
                                    // 超越时空的收集？ > 先赋值null ，最后用 set 补刀；
                                    null,
                                    lv2.getCatId().toString(),
                                    lv2.getName()
                            );
                            // 三级分类
                            List<CategoryEntity> lv3s = getParentCid(categoryEntities, lv2.getCatId());
                            if (CollectionUtils.isNotEmpty(lv3s)) {
                                List<Catalog2Vo.Catalog3Vo> catalog3Vos = lv3s.stream().map(
                                        lv3 -> new Catalog2Vo.Catalog3Vo(
                                                lv2.getCatId().toString(),
                                                lv3.getCatId().toString(),
                                                lv3.getName()
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
     * 得到父母cid
     * 从已知集合内挑出 Cid
     *
     * @param categories 类别
     * @param parentCid  父母cid
     * @return {@link List}<{@link CategoryEntity}>
     */
    public List<CategoryEntity> getParentCid(List<CategoryEntity> categories, Long parentCid) {
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