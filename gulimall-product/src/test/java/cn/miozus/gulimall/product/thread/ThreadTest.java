package cn.miozus.gulimall.product.thread;

import lombok.SneakyThrows;

import java.util.concurrent.*;

/**
 * 线测试
 * thread test
 *
 * @author miao
 * @date 2021/12/23
 */
public class ThreadTest {

    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * 启动线程的四种方式
     * <p>
     * doExtendThread();
     * doImplRunnable();
     * doCallableAndFutureTask();
     * 禁用以上三种（容易内存溢出）。异步任务请交给线程池执行，资源可控:
     * doExecutors();
     * <p>
     * buildMyExecutors();
     * 问：core:7, max:20, queue:50, 100 并发进来，怎么分配的？
     * 答：考察启动线程池执行过程的模型。
     * 7个核心线程立即执行，50个请求在队列等待，线程池内已安排57个请求;
     * 第 58 个请求进来，将核心线程容量扩充至20个（核心线程新增13个正在执行，顺延13个加入队列，也就安排了 70 个请求，线程池外剩余100-70=30 个）
     * 此时，线程池外的 30 个请求执行饱和策略，默认 Abort 丢弃执行 ，并抛出异常，程序中止。 如果不想被抛弃，可以在参数设置饱和策略。
     * <p>
     * doCompletableFuture();
     * doCompletableFutureSupply();
     * doCompletableFutureSupplyWhenCompleteAndExceptionally();
     * doCompletableFutureSupplyHandler();
     * <p>
     * 方法命名规则
     * Async 开新线程，可加入线程池
     * 线程串行化
     * Run 纯路人，无感知前后，本身不提供返回值
     * Accept 接收结果，共用线程，本身不提供返回值
     * Apply 接收结果，本身提供返回值，为下一步做准备
     * ----------------------------
     * doCompletableFutureThenRun();
     * doCompletableFutureThenAcceptAsync();
     * doCompletableFutureThenApplyAsync();
     * <p>
     * 两个任务组合：方法命名规则
     * 获取两个返回结果 返回当前任务的返回值
     * thenCombine ✅  ✅
     * thenAcceptBoth ✅
     * runAfterBoth
     * _Either 只要其中一个执行完成立即发车
     * ---------------------------------
     * doCompletableFutureRunAfterBothAsync();
     * doCompletableFutureThenAcceptBothAsync();
     * doCompletableFutureThenCombineAsync();
     * doCompletableFutureRunAfterEitherAsync();
     * doCompletableFutureAcceptEitherAsync();
     * doCompletableFutureApplyToEitherAsync();
     * <p>
     * 多任务组合：
     * allOf 全部集齐
     * anyOf 只要其一，本身返回最先完成的线程返回值
     * -------------------------
     * doCompletableFutureAllOf();
     * doCompletableFutureAnyOf();
     *
     * @param args arg
     */
    @SneakyThrows
    public static void main(String[] args) {
        System.out.println("main... start...");


        System.out.println("main... end...");
    }

    private static void doCompletableFutureAnyOf() throws InterruptedException, ExecutionException {
        CompletableFuture<Object> fA = doCompletableFutureSupplyFa("[fA] 当前线程:", executor);
        CompletableFuture<Object> fB = doCompletableFutureSupplyFb();
        CompletableFuture<Object> fC = doCompletableFutureSupplyFc();
        CompletableFuture<Object> fAnyOf = CompletableFuture.anyOf(fA, fB, fC);
        System.out.println("fAnyOf.get() = " + fAnyOf.get());
    }

    private static void doCompletableFutureAllOf() throws InterruptedException, ExecutionException {
        CompletableFuture<Object> fA = doCompletableFutureSupplyFa("[fA] 当前线程:", executor);
        CompletableFuture<Object> fB = doCompletableFutureSupplyFb();
        CompletableFuture<Object> fC = doCompletableFutureSupplyFc();
        CompletableFuture<Void> fAllOf = CompletableFuture.allOf(fA, fB, fC);
        System.out.println("fAllOf.get() = " + fAllOf.get());
        System.out.println("fA.get() + fB.get() + fC.get() = " + fA.get() + " " + fB.get() + " " + fC.get());
    }

    private static void doCompletableFutureApplyToEitherAsync() throws InterruptedException, ExecutionException {
        CompletableFuture<Object> fA = doCompletableFutureSupplyFa("[fA] 当前线程:", executor);
        CompletableFuture<Object> fB = doCompletableFutureSupplyFb();
        CompletableFuture<String> fC = fA.applyToEitherAsync(fB, (res) -> {
            System.out.println("[fC] 当前线程, 之前两个线程返回结果: " + res);
            return "value comes from [fC]";
        }, executor);
        System.out.println("fC.get() = " + fC.get());
    }

    /**
     * 异步编程组合两个线程：
     * 只接受任意其中一个线程的返回值（类型必须一样，推荐 Object），本身不返回结果
     */
    private static void doCompletableFutureAcceptEitherAsync() {
        CompletableFuture<Object> fA = doCompletableFutureSupplyFa("[fA] 当前线程:", executor);
        CompletableFuture<Object> fB = doCompletableFutureSupplyFb();
        fA.acceptEitherAsync(fB, (res) -> {
            System.out.println("[fC] 当前线程, 之前两个线程返回结果: " + res);
        }, executor);
    }

    private static void doCompletableFutureRunAfterEitherAsync() {
        CompletableFuture<Object> fA = doCompletableFutureSupplyFa("[fA] 当前线程:", executor);
        CompletableFuture<Object> fB = doCompletableFutureSupplyFb();
        fA.runAfterEitherAsync(fB, () -> {
            System.out.println("[fC] 当前线程");
        }, executor);
    }

    private static void doCompletableFutureThenCombineAsync() throws InterruptedException, ExecutionException {
        CompletableFuture<Object> fA = doCompletableFutureSupplyFa("[fA] 当前线程:", executor);
        CompletableFuture<Object> fB = doCompletableFutureSupplyFb();
        CompletableFuture<String> fC = fA.thenCombineAsync(fB, (resA, resB) -> {
            String methodName = Thread.currentThread().getStackTrace()[1].getMethodName().split("\\$")[1];
            System.out.println("[fC] 当前线程:" + Thread.currentThread().getId() + " 方法名 " + methodName);
            return resA + " & " + resB;
        }, executor);
        System.out.println("fC.get() = " + fC.get());
    }

    private static void doCompletableFutureThenAcceptBothAsync() {
        CompletableFuture<Object> fA = doCompletableFutureSupplyFa("[fA] 当前线程:", executor);
        CompletableFuture<Object> fB = doCompletableFutureSupplyFb();

        fA.thenAcceptBothAsync(fB, (resA, resB) -> {
            String methodName = Thread.currentThread().getStackTrace()[1].getMethodName().split("\\$")[1];
            System.out.println("[fC] 当前线程:" + Thread.currentThread().getId() + " 方法名 " + methodName);
            System.out.println("前两个线程返回值结果 fA:" + resA + " fB:" + resB);
        }, executor);
    }

    private static void doCompletableFutureRunAfterBothAsync() {
        CompletableFuture<Object> fA = doCompletableFutureSupplyFa("[fA] 当前线程:", executor);
        CompletableFuture<Object> fB = doCompletableFutureSupplyFb();

        fA.runAfterBothAsync(fB, () -> {
            System.out.println("[fC] 任务开始");
        }, executor);
    }

    private static CompletableFuture<Object> doCompletableFutureSupplyFb() {
        CompletableFuture<Object> fB = CompletableFuture.supplyAsync(() -> {
            String methodName = Thread.currentThread().getStackTrace()[1].getMethodName().split("\\$")[1];
            System.out.println("[fB] 当前线程:" + Thread.currentThread().getId() + " 方法名 " + methodName);
            proceedThreadBySleep(3000);
            System.out.println("[fB] 缓慢查询商品图片，已完成");
            return "fB商品图片";
        }, executor);
        return fB;
    }

    private static CompletableFuture<Object> doCompletableFutureSupplyFc() {
        CompletableFuture<Object> fB = CompletableFuture.supplyAsync(() -> {
            String methodName = Thread.currentThread().getStackTrace()[1].getMethodName().split("\\$")[1];
            System.out.println("[fC] 当前线程:" + Thread.currentThread().getId() + " 方法名 " + methodName);
            System.out.println("[fC] 查询商品属性");
            return "fC商品属性";
        }, executor);
        return fB;
    }

    private static void proceedThreadBySleep(int duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static CompletableFuture<Object> doCompletableFutureSupplyFa(String x, ExecutorService executor) {
        CompletableFuture<Object> fA = CompletableFuture.supplyAsync(() -> {
            String methodName = Thread.currentThread().getStackTrace()[1].getMethodName().split("\\$")[1];
            System.out.println(x + Thread.currentThread().getId() + " 方法名 " + methodName);
            int i = 10 / 2;
            System.out.println("[fA] 查询商品介绍（运行结果:" + i);
            return i;
        }, executor);
        return fA;
    }

    private static void doCompletableFutureThenApplyAsync() throws InterruptedException, ExecutionException {
        CompletableFuture<Integer> fB = CompletableFuture.supplyAsync(() -> {
            String methodName = Thread.currentThread().getStackTrace()[1].getMethodName().split("\\$")[1];
            System.out.println("当前线程:" + Thread.currentThread().getId() + " 方法名 " + methodName);
            int i = 10 / 2;
            System.out.println("运行结果:" + i);
            return i;
        }, executor).thenApplyAsync(res -> {
            System.out.println("任务 fB 启动了, 结果: " + res);
            return res * 2;
        }, executor);
        System.out.println("fB.get() = " + fB.get());
    }

    private static void doCompletableFutureThenAcceptAsync() {
        CompletableFuture.supplyAsync(() -> {
            String methodName = Thread.currentThread().getStackTrace()[1].getMethodName().split("\\$")[1];
            System.out.println("当前线程:" + Thread.currentThread().getId() + " 方法名 " + methodName);
            int i = 10 / 2;
            System.out.println("运行结果:" + i);
            return i;
        }, executor).thenAcceptAsync(res -> {
            System.out.println("任务 fB 启动了, 结果: " + res);
        }, executor);
    }

    private static void doCompletableFutureThenRunAsync() {
        CompletableFuture.supplyAsync(() -> {
            String methodName = Thread.currentThread().getStackTrace()[1].getMethodName().split("\\$")[1];
            System.out.println("当前线程:" + Thread.currentThread().getId() + " 方法名 " + methodName);
            int i = 10 / 2;
            System.out.println("运行结果:" + i);
            return i;
        }, executor).thenRunAsync(() -> {
            System.out.println("任务 fB 启动了");
        }, executor);
    }

    private static void doCompletableFutureSupply() throws InterruptedException, ExecutionException {
        CompletableFuture<Object> future = doCompletableFutureSupplyFa("当前线程:", executor);
        System.out.println("future.get() = " + future.get());
    }

    private static void doCompletableFutureSupplyHandler() throws InterruptedException, ExecutionException {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            String methodName = Thread.currentThread().getStackTrace()[1].getMethodName().split("\\$")[1];
            System.out.println("当前线程:" + Thread.currentThread().getId() + " 方法名 " + methodName);
            int i = 10 / 4;
            System.out.println("运行结果:" + i);
            return i;
        }, executor).handle((res, thr) -> {
            if (res != null) {
                return res * 2;
            }
            if (thr != null) {
                return 0;
            }
            return 0;
        });
        System.out.println("future = " + future.get());
    }

    /**
     * 异步编排：supply 有返回值，可以管道操作，成功后回调，感知异常和修改结果
     *
     * @throws InterruptedException java.lang. interrupted exception
     * @throws ExecutionException   java.util.concurrent. execution exception
     */
    private static void doCompletableFutureSupplyWhenCompleteAndExceptionally() throws InterruptedException, ExecutionException {
        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
            String methodName = Thread.currentThread().getStackTrace()[1].getMethodName().split("\\$")[1];
            System.out.println("当前线程:" + Thread.currentThread().getId() + " 方法名 " + methodName);
            int i = 10 / 0;
            System.out.println("运行结果:" + i);
            return i;
        }, executor).whenComplete((result, exception) -> {
            System.out.println("异步任务完成。[监听] 结果：" + result + " 异常：" + exception);
        }).exceptionally(throwable -> {
            System.out.println("异步任务完成。[反馈] 感知到异常，手动修改返回结果");
            return 10;
        });
        System.out.println("future = " + future.get());
    }

    /**
     * 异步编排：无返回值
     */
    private static void doCompletableFuture() {
        CompletableFuture.runAsync(() -> {
            String methodName = Thread.currentThread().getStackTrace()[1].getMethodName().split("\\$")[1];
            System.out.println("当前线程:" + Thread.currentThread().getId() + " 方法名 " + methodName);
            int i = 10 / 2;
            System.out.println("运行结果:" + i);
        }, executor);
    }

    private static void buildMyExecutors() {
        int corePoolSize = 10;
        int maximumPoolSize = 200;
        int keepAliveTime = 10;

        ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }

    private static void doExecutors() {
        executor.execute(new ThreadImplRunnable());
    }

    /**
     * 可调用的方法和未来的任务
     *
     * @methhod get 阻塞整个线程执行完成，获取返回结果
     */
    @SneakyThrows
    private static void doCallableAndFutureTask() {
        FutureTask<Integer> futureTask = new FutureTask<>(new ThreadImplCallable());
        new Thread(futureTask).start();
        Integer result = futureTask.get();
        System.out.println("result = " + result);
    }

    private static void doImplRunnable() {
        ThreadImplRunnable runnable = new ThreadImplRunnable();
        new Thread(runnable).start();
    }

    private static void doExtendsThread() {
        ThreadExtendsThread thread = new ThreadExtendsThread();
        thread.start();
    }

    public static class ThreadExtendsThread extends Thread {

        @Override
        public void run() {
            String methodName = Thread.currentThread().getStackTrace()[1].getMethodName().split("\\$")[1];
            System.out.println("当前线程:" + Thread.currentThread().getId() + " 方法名 " + methodName);
            int i = 10 / 2;
            System.out.println("运行结果:" + i);
        }
    }

    public static class ThreadImplRunnable implements Runnable {

        @Override
        public void run() {
            String methodName = Thread.currentThread().getStackTrace()[1].getMethodName().split("\\$")[1];
            System.out.println("当前线程:" + Thread.currentThread().getId() + " 方法名 " + methodName);
            int i = 10 / 2;
            System.out.println("运行结果:" + i);
        }
    }

    public static class ThreadImplCallable implements Callable<Integer> {

        @Override
        public Integer call() throws Exception {
            String methodName = Thread.currentThread().getStackTrace()[1].getMethodName().split("\\$")[1];
            System.out.println("当前线程:" + Thread.currentThread().getId() + " 方法名 " + methodName);
            int i = 10 / 2;
            System.out.println("运行结果:" + i);
            return i;
        }
    }
}
