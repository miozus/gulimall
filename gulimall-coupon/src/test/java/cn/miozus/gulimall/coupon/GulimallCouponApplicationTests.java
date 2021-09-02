package cn.miozus.gulimall.coupon;

import lombok.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

//@SpringBootTest
class GulimallCouponApplicationTests {

    @Test
    void contextLoads() {
        String a = "aaa";
        String b = "bbb";
        // 不传递
        //String c = b;
        //b = a;
        //a = c;
        swapString(a, b);
        System.out.println("a = " + a);
        System.out.println("b = " + b);

        update(a);
        System.out.println("a = " + a);

    }

    // 传递
    public void swapString(String m, String n) {
        String temp;
        temp = n;
        n = m;
        m = temp;
        System.out.println("m = " + m);
        System.out.println("n = " + n);
    }

    public void update(String i) {

        i = "ccc";
        System.out.println("i = " + i);
    }

    @Test
    public void changeVar() {
        int i = 10; //Variable 'i' initializer '10' is redundant
        i = 20;  // The value 20 assigned to 'i' is never used
        i = 30;
        System.out.println("i = " + i);
    }

    @Test
    public void swapVar() {
        int a = 10, b = 20, temp;
        temp = b;
        b = a;
        a = temp;
        System.out.println("a = " + a);
        System.out.println("b = " + b);
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public class Person implements Cloneable {
        String name;
        Integer age;

        @Override
        @SneakyThrows
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
        }
    }


    /**
     * 深拷贝
     */
    @Test
    @SneakyThrows
    public void deepClone() {
        Person p1 = new Person("张三", 20);
        Person p2 = (Person) p1.clone();
        Person p3 = p1;
        p1.setName("李四");
        System.out.println("p1 = " + p1);
        System.out.println("p2 = " + p2);
        System.out.println("p3 = " + p3);
        System.out.println("------------------------");
        p2.setName("王五");
        System.out.println("p1 = " + p1);
        System.out.println("p2 = " + p2);
        System.out.println("p3 = " + p3);
        System.out.println("------------------------");
        p3.setName("麻子");
        System.out.println("p1 = " + p1);
        System.out.println("p2 = " + p2);
        System.out.println("p3 = " + p3);
        System.out.println("------------------------");
    }

    public void shallowClone() {
        int n1 = 0;
        StringBuilder b = new StringBuilder();
        StringBuffer buf = new StringBuffer();
    }

    /**
     * 交换对象
     */
    @Test
    public void changeRef() {
        Person p1 = new Person("张三", 20);
        Person p2 = new Person("李四", 30);
        Person p3 = p2;
        Person temp;
        temp = p2;
        p2 = p1;
        p1 = temp;
        System.out.println("p1 = " + p1);
        System.out.println("p2 = " + p2);
        System.out.println("p3 = " + p3);
    }

    /**
     * 测试整数包类型
     */
    @Test
    public void testIntegerPackType() {
        Integer i1 = 40;
        Integer i2 = new Integer(40);
        Integer i6 = 40;
        // 2️⃣ 验证堆比较：排除缓存区，这说明封装和新建对象，是两个堆，是两个对象
        System.out.println("i1==i2 : " + (i1 == i2));
        System.out.println("Objects.equals(i1, i2) : " + Objects.equals(i1, i2));
        // 1️ 验证常量池 [-128, 127] 未超出缓存：同个对象
        System.out.println("i1==i6 : " + (i1 == i6));
        System.out.println("Objects.equals(i1, i6) : " + Objects.equals(i1, i6));

        Integer i3 = 1024;
        Integer i4 = new Integer(1024);
        Integer i5 = 1024;
        // 2️⃣ 验证堆比较：排除缓存区，这说明封装和新建对象，是两个堆，是两个对象
        System.out.println("i3==i4 : " + (i3 == i4));
        System.out.println("Objects.equals(i3, i4) : " + Objects.equals(i3, i4));
        // 1️ 验证常量池 [-128, 127] 超出缓存范围，不用缓存区，所以两个对象
        System.out.println("i3==i5 : " + (i3 == i5));
        System.out.println("Objects.equals(i3, i5) : " + Objects.equals(i3, i5));


    }


    /**
     * 测试浮点数
     */
    @Test
    public void testFloat() {
        float a = 1.0f - 0.9f;
        float b = 0.9f - 0.8f;
        System.out.println(a);// 0.100000024
        System.out.println(b);// 0.099999964
        System.out.println("(a == b) : " + (a == b));
        System.out.println("Objects.equals(a,b) : " + Objects.equals(a, b));
    }

    @Test
    public void testPackFloat() {
        Float a = 1.0f - 0.9f;
        Float b = 0.9f - 0.8f;
        System.out.println(a);// 0.100000024
        System.out.println(b);// 0.099999964
        System.out.println("(a == b) : " + (a == b));
        System.out.println("Objects.equals(a,b) : " + Objects.equals(a, b));
    }
    
    @Test
    public void testBasicType(){
        //int i1 =  null;
        int i2 = (Integer) null;
        System.out.println("i2 = " + i2);
    }

    @Test
    public void testArrayType(){
        int[] myArray = {1, 2, 3};
        List myList = Arrays.asList(myArray);
        System.out.println(myList.size());//1
        System.out.println(myList.get(0));//数组地址值
        System.out.println(myList.get(1));//报错：ArrayIndexOutOfBoundsException
        int[] array = (int[]) myList.get(0);
        System.out.println(array[0]);//1

    }

    @Test
    public  void testCollection() {
        String [] s= new String[]{
                "dog", "lazy", "a", "over", "jumps", "fox", "brown", "quick", "A"
        };
        System.out.println("s = " + s);
        System.out.println("s.getClass() = " + s.getClass());
        List<String> list = Arrays.asList(s);
        Collections.reverse(list);
        System.out.println("list = " + list);
        System.out.println("list.getClass() = " + list.getClass());
        String[] strings = list.toArray(new String[0]);
        System.out.println("strings = " + Arrays.toString(strings));
        System.out.println("strings = " + strings);
    }

    @Test
    public void testStringConstPool(){
        String s1 = "abc";
        String s2 = "abc";
        System.out.println("(s1==s2) : " + (s1 == s2));

        String s3 = new String("xyz");
        String s4 = new String("xyz");
        System.out.println("(s3==s4) : " + (s3 == s4));

        String s5 = new StringBuilder().append("ja").append("va1").toString();
        System.out.println("(s5.intern() == s5) : " + (s5.intern() == s5));
    }
}

