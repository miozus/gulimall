package cn.miozus.gulimall.seckill.scheduled;

import cn.miozus.gulimall.common.annotation.Idempotent;
import cn.miozus.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 秒杀定时任务
 *
 * @author miozus
 * @date 2022/03/06
 */
@Slf4j
@Service
public class SeckillSkuSchedule {

    @Autowired
    SeckillService seckillService;

    @Async
    @Scheduled(cron = "0 9 * * * ?")
    @Idempotent("秒杀商品上架加锁")
    public void uploadContinuous3DaysSku() {
        log.info("⏰ uploadContinuous3DaysSku schedule");
        seckillService.uploadContinuous3DaySku();
    }
}
