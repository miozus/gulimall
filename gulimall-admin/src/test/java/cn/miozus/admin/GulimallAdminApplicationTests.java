package cn.miozus.admin;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

//@SpringBootTest
class GulimallAdminApplicationTests {

    @Test
    void contextLoads() {
    }

    int[] memo;

    @Test
    void testDpInitAmountPlusOne() {
        int[] coins = {1, 2, 5};
        int amount = 11;
        int res = coinChange(coins, amount);
        System.out.println("res = " + res);
    }

    public int coinChange(int[] coins, int amount) {
        memo = new int[amount + 1];
        Arrays.fill(memo,  amount+100);
        return dp(coins, amount);
    }

    int dp(int[] coins, int amount) {
        if (amount == 0) return 0;
        if (amount < 0) return -1;

        if (memo[amount] != amount+100) return memo[amount];
        int res = Integer.MAX_VALUE;
        for (int coin : coins) {
            int subProblem = dp(coins, amount - coin);
            if (subProblem == -1) continue;
            res = Math.min(res, subProblem + 1);

        }
        memo[amount] = (res == Integer.MAX_VALUE) ? -1 : res;
        return memo[amount];

    }


}
