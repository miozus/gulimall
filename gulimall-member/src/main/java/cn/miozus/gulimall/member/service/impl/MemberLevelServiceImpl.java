package cn.miozus.gulimall.member.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;

import cn.miozus.gulimall.member.dao.MemberLevelDao;
import cn.miozus.gulimall.member.entity.MemberLevelEntity;
import cn.miozus.gulimall.member.service.MemberLevelService;


@Service("memberLevelService")
public class MemberLevelServiceImpl extends ServiceImpl<MemberLevelDao, MemberLevelEntity> implements MemberLevelService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberLevelEntity> page = this.page(
                new Query<MemberLevelEntity>().getPage(params),
                new QueryWrapper<MemberLevelEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryMemberPage(Map<String, Object> params) {

        String key = (String) params.get("key");

        /** SQL 模糊查询
         select * from pms_attr_group
         where catelog_id=?
         and (attr_group_id=key
         or attr_group_name like %key%)
         */
        QueryWrapper<MemberLevelEntity> wrapper = new QueryWrapper<>();
        if (!StringUtils.isBlank(key)) {
            wrapper.and(obj ->
                    obj.eq("attr_group_id", key)
                            .or()
                            .like("attr_group_name", key));
        }
        IPage<MemberLevelEntity> page = this.page(
                new Query<MemberLevelEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);

    }

}