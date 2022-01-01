package cn.miozus.gulimall.member.service.impl;

import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.gulimall.member.dao.MemberDao;
import cn.miozus.gulimall.member.dao.MemberLevelDao;
import cn.miozus.gulimall.member.entity.MemberEntity;
import cn.miozus.gulimall.member.entity.MemberLevelEntity;
import cn.miozus.gulimall.member.exception.PhoneNumberAlreadyExistsException;
import cn.miozus.gulimall.member.exception.UsernameAlreadyExistsException;
import cn.miozus.gulimall.member.service.MemberService;
import cn.miozus.gulimall.member.vo.MemberLoginVo;
import cn.miozus.gulimall.member.vo.MemberRegisterVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 注册
     * <p>
     * 异常机制：只要字段被注册过，都不通过
     * 加密储存：BCryptPasswordEncoder 加密器托管
     *
     * @param vo 注册页面提交的用户信息表单
     */
    @Override
    public void register(MemberRegisterVo vo) {
        String username = vo.getUsername();
        String phone = vo.getPhone();
        checkUsernameUnique(username);
        checkPhoneUnique(phone);

        MemberEntity entity = new MemberEntity();
        MemberDao dao = this.baseMapper;

        MemberLevelEntity levelEntity = memberLevelDao.queryDefaultLevel();
        Long id = levelEntity.getId();

        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());

        entity.setLevelId(id);
        entity.setUsername(username);
        entity.setMobile(phone);
        entity.setPassword(encode);
        //SimpleDateFormat.getDateInstance().format()

        dao.insert(entity);
    }

    @Override
    public boolean checkEmailUnique(String email) {
        return false;
    }

    @Override
    public void checkUsernameUnique(String username) {
        MemberDao dao = this.baseMapper;
        Integer count = dao.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if (count > 0) {
            throw new UsernameAlreadyExistsException();
        }
    }

    @Override
    public boolean checkUsernameExists(String username) {
        MemberDao dao = this.baseMapper;
        Integer count = dao.selectCount(new QueryWrapper<MemberEntity>().eq("username", username));
        if (count > 0) {
            return true;
        }
        return false;
    }

    @Override
    public void checkPhoneUnique(String phone) {
        MemberDao dao = this.baseMapper;
        Integer count = dao.selectCount(new QueryWrapper<MemberEntity>().eq("mobile", phone));
        if (count > 0) {
            throw new PhoneNumberAlreadyExistsException();
        }
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String account = vo.getAccount();
        String password = vo.getPassword();
        MemberDao dao = this.baseMapper;
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        MemberEntity entity = dao.selectOne(new QueryWrapper<MemberEntity>().eq("username", account).or().eq("mobile", account));
        if (entity==null) {
            return null;
        }
        String encode = entity.getPassword();
        boolean matches = passwordEncoder.matches(password, encode);
        if (matches) {
            return entity;
        }
        return null;
    }

}