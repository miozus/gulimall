package cn.miozus.gulimall.product.demo;

/**
 * 同步演示
 *
 * @author miao
 * @date 2021/10/13
 */
public class SynchronizeDemo {

        public void method() {
            synchronized (this) {
                System.out.println("synchronized 代码块");
            }
        }

}
