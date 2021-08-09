package cn.miozus.gulimall.coupon.entity;

import cn.miozus.gulimall.coupon.service.MemberPriceService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MemberPriceEntityTest {

    @Autowired
    MemberPriceService memberPriceService;
    MemberPriceEntity memberPriceEntity;


    @Test
    void getMemberPrice() {
        List<MemberPriceEntity> member = memberPriceService.list(new QueryWrapper<MemberPriceEntity>().eq("id", 16L));
        System.out.println(member);

    }
}