package cn.miozus.gulimall.cart;

import com.alibaba.nacos.common.utils.CollectionUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertTrue;


//@SpringBootTest
class GulimallCartApplicationTests {

    @Test
    void testEmptyList() {
        List<Object> emptyList = Collections.emptyList();
        assertTrue(CollectionUtils.isEmpty(emptyList));

    }

    /**
     * 测试条目设置
     * stream().map：只能传入一个参数
     * forEach：集合默认以 EntrySet 作为单个元素，可以传入两个参数
     */
    @Test
    void testEntrySet() {
        HashMap<Long, String> map = new HashMap<>();
        map.put(1L, "hello");
        map.put(2L, "world");
        map.put(3L, "!");
        map.forEach((k, v) -> System.out.println("k + v = " + k + v));
    }

    /**
     * 测试大十进制加
     *
     * 超级静态的变量：调用者不会改变，必须找到接收者
     * 黄色提示：返回结果（如果不接受）会被忽略
     */
    @Test
    void testBigDecimalAdd() {
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal n30 = new BigDecimal(30);
        amount.add(n30);
        System.out.println("amount1 = " + amount);
        amount.add(n30);
        System.out.println("amount2 = " + amount);
        BigDecimal res = amount.add(n30);
        System.out.println("res = " + res);
        BigDecimal res2 = res.add(n30);
        System.out.println("res2 = " + res2);

        List<BigDecimal> list = Arrays.asList(new BigDecimal(10), new BigDecimal(20), new BigDecimal(30));
        list.stream().forEach(System.out::println);

        BigDecimal zero = BigDecimal.ZERO;
        for (BigDecimal b : list) {
            zero = zero.add(b);
        }
        System.out.println("zero = " + zero);

    }

}
