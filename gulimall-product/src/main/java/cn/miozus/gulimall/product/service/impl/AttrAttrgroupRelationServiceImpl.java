package cn.miozus.gulimall.product.service.impl;

import cn.miozus.gulimall.common.utils.PageUtils;
import cn.miozus.gulimall.common.utils.Query;
import cn.miozus.gulimall.product.dao.AttrAttrgroupRelationDao;
import cn.miozus.gulimall.product.entity.AttrAttrgroupRelationEntity;
import cn.miozus.gulimall.product.service.AttrAttrgroupRelationService;
import cn.miozus.gulimall.product.vo.AttrGroupRelationVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("attrAttrgroupRelationService")
public class AttrAttrgroupRelationServiceImpl extends ServiceImpl<AttrAttrgroupRelationDao, AttrAttrgroupRelationEntity> implements AttrAttrgroupRelationService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrAttrgroupRelationEntity> page = this.page(
                new Query<AttrAttrgroupRelationEntity>().getPage(params),
                new QueryWrapper<AttrAttrgroupRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveRelations(List<AttrGroupRelationVo> vos) {
        List<AttrAttrgroupRelationEntity> relationEntities = vos.stream().map(vo -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(vo, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());

        this.saveBatch(relationEntities);
    }

}