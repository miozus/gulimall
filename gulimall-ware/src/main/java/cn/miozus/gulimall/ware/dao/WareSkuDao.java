package cn.miozus.gulimall.ware.dao;

import cn.miozus.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品库存
 * 
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:20:54
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    /**
     * 添加股票
     *
     * @param skuId  sku id
     * @param wareId 器皿id
     * @param skuNum sku num
     */
    void addStock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);

    /**
     * 得到sku股票
     * @param skuId sku id 仅有一个参数时，可省略 @Param 参数
     * @return long
     */
    Long getSkuStock(@Param("skuId") Long skuId);

    /**
     * 查询某个sku涉及到的所有仓库号
     *
     * @param skuId sku id
     * @return {@link List}<{@link Long}>
     */
    List<Long> queryWareIdsBySkuId(@Param("skuId") Long skuId);

    /**
     * 锁定库存操作：更新锁定数量
     *
     * @param skuId  sku号
     * @param wareId 仓库号
     * @param num    该商品购物数量
     * @return {@link Long}
     */
    Long updateStockLock(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("num") Integer num);

    /**
     * 解锁库存：撤回至上一步状态
     *
     * @param skuId  sku id
     * @param wareId 器皿id
     * @param skuNum sku num
     */
    void updateStockBackToLastStatus(@Param("skuId") Long skuId, @Param("wareId") Long wareId, @Param("skuNum") Integer skuNum);
}
