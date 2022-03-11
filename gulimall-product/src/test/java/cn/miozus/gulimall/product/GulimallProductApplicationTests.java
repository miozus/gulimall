package cn.miozus.gulimall.product;

import cn.miozus.gulimall.product.dao.AttrGroupDao;
import cn.miozus.gulimall.product.service.CategoryService;
import cn.miozus.gulimall.product.service.SkuSaleAttrValueService;
import cn.miozus.gulimall.product.vo.SkuItemVo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;

@Slf4j
@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    CategoryService categoryService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;

    @Test
    public void testGetSaleAttrsBySkuId() {

        List<SkuItemVo.SkuItemSaleAttrVo> saleAttrValueService = skuSaleAttrValueService.querySaleAttrsBySpuId(12L);
        System.out.println("saleAttrValueService = " + saleAttrValueService);
    }

    @Test
    public void testGetAttrGroupWithAttrsBySpuId() {
        List<SkuItemVo.SpuItemGroupAttrVo> res = attrGroupDao.getAttrGroupWithAttrsBySpuId(12L, 225L);
        System.out.println("res = " + res);
    }

    @Test
    public void testStringRedisTemplate() {
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        // 保存，为了不一样，附带随机的字符
        ops.set("hello", "world" + UUID.randomUUID().toString());

        // 保存，为了不一样，附带随机的字符
        System.out.println("ops.get(\"hello\") = " + ops.get("hello"));
    }

    /**
     * @author shuang.kou
     * @createTime 2020年06月15日 17:02:00
     */
    public class Person {
        private Integer age;

        public Person(Integer age) {
            this.age = age;
        }

        public Integer getAge() {
            return age;
        }
    }


    @Test
    public void testmaxSlidingWindow() {
        int[] nums = {1, 3, -1, -3, 5, 3, 6, 7};
        int k = 3;
        System.out.println("maxSlidingWindow(nums, k) = " + Arrays.toString(maxSlidingWindow(nums, k)));
    }

    public int[] maxSlidingWindow(int[] nums, int k) {
        if (k == 1) {
            return nums;
        } else if (k > 1 && k == nums.length) {
            int max = 0;
            for (int i = 0; i < nums.length; i++) {
                max = max > nums[i] ? max : nums[i];
            }
            return new int[]{max};
        } else {
            List<Integer> res = new ArrayList<>();
            Deque<Integer> deque = new LinkedList<>();
            PriorityQueue<Integer> pq = new PriorityQueue<>(k, (o1, o2) -> o2 - o1);
            for (int i = 0; i < nums.length; i++) {
                deque.addFirst(nums[i]);
                if (i < k - 1) {
                    pq.add(nums[i]);
                } else {
                    pq.add(nums[i]);
                    res.add(pq.peek());
                    pq.remove(deque.pollLast());
                }
            }
            return res.stream().mapToInt(i -> i).toArray();
        }
    }

    public int[] maxSlidingWindowPQ8Map(int[] nums, int k) {
        if (k == 1) {
            return nums;
        } else if (k > 1 && k == nums.length) {
            int max = 0;
            for (int i = 0; i < nums.length; i++) {
                max = max > nums[i] ? max : nums[i];
            }
            return new int[]{max};
        } else {
            List<Integer> res = new ArrayList<>();
            HashMap<Integer, Integer> map = new HashMap<>();
            PriorityQueue<Integer> pq = new PriorityQueue<>(k, (o1, o2) -> o2 - o1);
            for (int i = 0; i < nums.length; i++) {
                map.put(i, nums[i]);
                pq.add(nums[i]);
                if (pq.size() == k) {
                    res.add(pq.peek());
                    // 优先队列，只关注热榜，对找末位元素需要迭代器轮询
                    pq.remove(map.get(i - k + 1));
                }
            }
            return res.stream().mapToInt(i -> i).toArray();
        }
    }


    @Test
    public void testIntegerAndint() {
        int aint = 0;
        Integer aInteger = null;
        Integer bInteger = 0;
        Integer cInteger = new Integer(0);

        int bint = 129;
        Integer dInteger = new Integer(129);
        // nboxing of 'aInteger' may produce 'NullPointerException'
        //System.out.println("aint == aInteger = " + (aint == aInteger));
        // Condition 'aint == bInteger' is always 'true' when reached
        System.out.println("aint == bInteger = " + (aint == bInteger));
        System.out.println("aint == cInteger = " + (aint == cInteger));
        System.out.println("bint == dInteger = " + (bint == dInteger));
    }

    @Test
    public void sortPersonByAge() {
        //TreeMap<Person, String> treeMap = new TreeMap<>(new Comparator<Person>() {
        //    @Override
        //    public int compare(Person person0, Person person2) {
        //        int num = person0.getAge() - person2.getAge();
        //        return Integer.compare(num, -1);
        //    }
        //});

        //或匿名内部类写法
        TreeMap<Person, String> treeMap = new TreeMap<>((person0, person2) -> {
            int num = person0.getAge() - person2.getAge();
            return Integer.compare(num, -1);
        });

        treeMap.put(new Person(2), "person1");
        treeMap.put(new Person(17), "person2");
        treeMap.put(new Person(34), "person3");
        treeMap.put(new Person(15), "person4");
        treeMap.entrySet().stream().forEach(personStringEntry -> {
            System.out.println(personStringEntry.getValue());
        });
    }

    @Test
    public void testNextGreaterElement() {
        int[] num1 = {4, 1, 2};
        int[] num2 = {1, 3, 4, 2};
        //int[] res = nextGreaterElement(num1, num2);
        int[] res = nextGreaterElementByStack(num1, num2);
        //int[] res = nextGreaterElementByStack2(num1, num2);
        System.out.println("res = " + Arrays.toString(res));
    }

    public int[] nextGreaterElementByStack2(int[] nums1, int[] nums2) {
        int index;
        int[] res = new int[nums1.length];
        HashMap<Integer, Integer> map = new HashMap<>();
        for (int i = nums2.length - 1; i >= 0; i--) {
            map.put(nums2[i], i);
        }

        Stack stack = new Stack();
        for (int i = nums2.length - 1; i >= map.get(nums1[i]); i--) {
            while (!stack.isEmpty() && (int) stack.peek() <= nums2[i]) {
                stack.pop();
            }
            res[i] = stack.isEmpty() ? -1 : (int) stack.peek();
            stack.push(nums2[i]);
        }
        return res;
    }

    public int[] nextGreaterElementByStack(int[] nums1, int[] nums2) {

        int[] res = new int[nums1.length];
        Map<Integer, Integer> map = nextGreaterHelper(nums2);
        for (int i = 0; i < res.length; i++) {
            res[i] = map.get(nums1[i]);
        }

        return res;
    }

    private Map<Integer, Integer> nextGreaterHelper(int[] nums2) {
        Map<Integer, Integer> map = new HashMap<>();
        Stack<Integer> stack = new Stack<>();
        int[] res = new int[nums2.length];
        for (int i = nums2.length - 1; i >= 0; i--) {
            while (!stack.isEmpty() && stack.peek() <= nums2[i]) {
                stack.pop();
            }
            res[i] = stack.isEmpty() ? -1 : stack.peek();
            map.put(nums2[i], res[i]);
            stack.push(nums2[i]);
        }
        return map;
    }

    public int[] nextGreaterElementIndex(int[] nums1, int[] nums2) {
        HashMap<Integer, Integer> map = new HashMap<>();
        Deque<Integer> stack = new LinkedList<>();
        for (int i = 0; i < nums2.length; i++) {
            while (!stack.isEmpty() && nums2[stack.peek()] < nums2[i]) {
                int j = stack.pop();
                map.put(nums2[j], nums2[i]); // 此时nums2[j]<nums2[i]
            }
            stack.push(i); // 下标入栈
        }
        int[] ans = new int[nums1.length];
        for (int i = 0; i < nums1.length; i++) {
            ans[i] = map.getOrDefault(nums1[i], -1);
        }
        return ans;
    }

    public int[] nextGreaterElement(int[] nums1, int[] nums2) {
        int index;
        int[] res = new int[nums1.length];
        HashMap<Integer, Integer> map = new HashMap<>();

        for (int i = 0; i < nums2.length; i++) {
            map.put(nums2[i], i);
        }

        for (int i = 0; i < nums1.length; i++) {
            if ((index = map.get(nums1[i]) + 1) >= nums2.length) {
                res[i] = -1;
            } else {
                while (index < nums2.length) {
                    res[i] = nums2[index] > nums1[i] ? nums2[index] : -1;
                    if (res[i] != -1) {
                        break;
                    } else {
                        index++;
                    }
                }
            }
        }
        return res;
    }

    @Test
    public void testFindPath() {

        Long[] path = categoryService.findCatalogPath(225L);
        System.out.println("path = " + Arrays.toString(path));
        log.info("子目录完整路径：{}", Arrays.asList(path));
    }

    @Test
    void contextLoads() {
    }

}
