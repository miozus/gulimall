package cn.miozus.gulimall.product.demo;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class AtomicIntegerDemo {


    @Test
    public void main(String[] args) {
        AtomicIntegerTest test = new AtomicIntegerTest();
        int count = test.getCount();
        System.out.println("count = " + count);
    }

    class AtomicIntegerTest {

        public AtomicInteger count = new AtomicInteger(20);
        //使用AtomicInteger之后，不需要对该方法加锁，也可以实现线程安全。

        public void increment() {
            count.incrementAndGet();
        }

        public int getCount() {
            return count.get();
        }
    }

}
