
package cn.miozus.gulimall.product.web;

import cn.miozus.gulimall.product.entity.CategoryEntity;
import cn.miozus.gulimall.product.service.CategoryService;
import cn.miozus.gulimall.product.vo.Catalog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 指数控制器
 *
 * @author miao
 * @date 2021/10/04
 */
@Controller
public class IndexController {

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redisson;

    @Autowired
    StringRedisTemplate redis;


    /**
     * 访问首页
     * 业务服务: 访问数据库、模板渲染，都会影响速度
     *
     * @param model 模型
     * @return {@link String}
     */
    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model) {
        // 查询一级分类 1️⃣ 访问数据库
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categories();
        // 2️⃣ 模板渲染
        model.addAttribute("categories", categoryEntities);
        return "index";
    }

    /**
     * 获取目录 JSON
     * 业务服务：巨慢的原因 db
     * 优化：启用分布式缓存 Redis
     *
     * @return {@link Map}<{@link Integer}, {@link Object}> 适用[JSON]
     * @Annotation ResponseBody 以JSON 格式返回
     */
    @ResponseBody
    @GetMapping("/index/json/catalog.json")
    public Map<String, List<Catalog2Vo>> getCatalogJson() {
        return categoryService.getCatalogJson();
    }

    /**
     * 简单服务：可以附带两个中间件 Nginx + Gateway
     * 不查数据库等
     * 后面用来测试 redisson 可重入锁（看门狗）
     *
     * @return {@link String}
     */
    @ResponseBody
    @GetMapping("/hello")
    public String hello() {

        // 获取一把锁，只要名字一样，就是一把锁
        RLock lock = redisson.getLock("my-lock");
        // 10 秒自动过期时间, 锁时间到了之后， 不会自动续期
        // 👍 如果设置超时时间，就执行脚本，让 redis 占锁，设定为一样的超时时间；30秒足够了，因为一个业务顶天不能超30秒

        // 👎 lock.lock()
        // 如果未设置超时时间，就使用看门狗默认的30秒; 加锁,阻塞式等待，两个的特性
        // ⏳ 失效期限：加锁的业务只要运行完成，就不会续期，即使不手动解锁，锁默认30秒以后自动删除
        // 🐕 看门狗：锁的自动续期，如果业务超时，运行期间自动给锁加上新的 30 秒
        //    - 如果占锁成功，就会启动一个定时任务，刷新锁的过期时间（新的时间也是看门狗的默认30秒）
        //    - 每隔 1/3 看门狗时间，（10秒）自动续期1次，至满时间。
        try {
            System.out.println("🔒 加锁成功，执行业务" + Thread.currentThread().getId());
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 解锁
            System.out.println("🔓 解锁了 " + Thread.currentThread().getId());
            lock.unlock();
        }
        return "hello";
    }

    /**
     * 读
     * 测试读写锁
     *
     * @return {@link String}
     */
    @ResponseBody
    @GetMapping("/read")
    public String readValue() {
        RReadWriteLock rwLock = redisson.getReadWriteLock("myRWLock");
        String s = "";
        try {
            rwLock.readLock().lock();
            s = redis.opsForValue().get("writeValue");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rwLock.readLock().unlock();
        }
        return s;
    }

    /**
     * 写
     *
     * @return {@link String}
     */
    @ResponseBody
    @GetMapping("/write")
    public String writeValue() {
        RReadWriteLock rwLock = redisson.getReadWriteLock("myRWLock");
        String s = "";
        try {
            /**
             *          ✍🏻                    💬
             *   💬     等待读锁释放           并发读只会记录所有读锁，同时加锁成功，相当于无锁
             *   ✍🏻     阻塞                  等待写锁释放
             *
             *   只要有写的存在，都必须等待（先后排队）
             */
            // 按名加锁，读锁给读方法，写锁给写方法。多读一写。
            // 保证一定能读到最新数据
            // 修改期间，写锁是互斥锁（私有的/独享/排它），读锁是共享锁，写锁没释放，读就必须等待。
            rwLock.writeLock().lock();
            s = UUID.randomUUID().toString();
            Thread.sleep(30000);
            redis.opsForValue().set("writeValue", s);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            rwLock.writeLock().unlock();
        }
        return s;
    }


    /**
     * 闭锁（成员）
     * 场景：度假了，班级的人要走光了
     *
     * @param id id
     * @return {@link String}
     */
    @ResponseBody
    @GetMapping("/vacation/{id}")
    public String goVacation(@PathVariable("id") Long id) {
        RCountDownLatch latch = redisson.getCountDownLatch("school");
        latch.countDown();
        ;
        return "class " + id + " all classmates have gone";
    }


    /**
     * 闭锁（管理员）
     * 假设场景：每个班级都走光了（每走一次就减少一次），全部走光时，才能关门
     *
     * @return {@link String}
     */
    @ResponseBody
    @GetMapping("/latch")
    public String closeDoor() {

        RCountDownLatch latch = redisson.getCountDownLatch("school");
        latch.trySetCount(3);
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "count down(done), latch is close finally";
    }

    /**
     * 信号量（申请资源）
     * 模拟场景：停车位够不够, 阻塞直到有车位的控制
     * 可用来做限流操作（每个服务1万请求，先获取信号量，等别人释放才能处理）
     * @return {@link String}
     */
    @ResponseBody
    @GetMapping("/semaphore")
    public String park() {
        RSemaphore semaphore = redisson.getSemaphore("semaphore");
            // 获取一个信号（获取成功就阻塞，升级版synchronized），占位;一直等待，直到有空位
            //semaphore.acquire();
            boolean b = semaphore.tryAcquire();
            // 如果 b ，（非阻塞运行）执行任务；否则返回错误，访问流量大, 请稍等
            return "acquire semaphore => " + b;
    }

    /**
     * 信号量（腾出空位）
     *
     * @return {@link String}
     * @see String
     */
    @ResponseBody
    @GetMapping("/go")
    public String go() {
        RSemaphore semaphore = redisson.getSemaphore("semaphore");
        // 释放一个车位，空位 （才能去申请停车）
        semaphore.release();
        return "release";
    }


}