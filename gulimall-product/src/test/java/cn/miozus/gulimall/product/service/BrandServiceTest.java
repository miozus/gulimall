package cn.miozus.gulimall.product.service;

import cn.miozus.gulimall.product.entity.BrandEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BrandServiceTest {

    @Autowired
    BrandService brandService;
    BrandEntity brandEntity = new BrandEntity();

    @Test
    void add() {

        brandEntity.setDescript("");
        brandEntity.setName("鸿蒙");

        brandService.save(brandEntity);
        System.out.println("保存成功");
    }

    @Test
    void updateById() {
        brandEntity.setBrandId(6L);
        brandEntity.setDescript("HarmonyOS2");
        brandService.updateById(brandEntity);
        System.out.println("修改成功");
    }

    @Test
    void queryObject(){
        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1L));
        System.out.println("list = " + list);
    }
}