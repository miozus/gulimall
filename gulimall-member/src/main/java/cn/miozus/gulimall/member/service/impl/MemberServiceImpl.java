package cn.miozus.gulimall.member.service.impl;

import cn.miozus.common.utils.HttpUtils;
import cn.miozus.common.utils.PageUtils;
import cn.miozus.common.utils.Query;
import cn.miozus.gulimall.member.dao.MemberDao;
import cn.miozus.gulimall.member.dao.MemberLevelDao;
import cn.miozus.gulimall.member.entity.MemberEntity;
import cn.miozus.gulimall.member.entity.MemberLevelEntity;
import cn.miozus.gulimall.member.exception.PhoneNumberAlreadyExistsException;
import cn.miozus.gulimall.member.exception.UsernameAlreadyExistsException;
import cn.miozus.gulimall.member.service.MemberService;
import cn.miozus.gulimall.member.vo.*;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;


@Service("memberService")
@Slf4j
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

        dao.insert(entity);
    }
    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String account = vo.getAccount();
        String password = vo.getPassword();
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        MemberDao dao = this.baseMapper;
        MemberEntity entity = dao.selectOne(new QueryWrapper<MemberEntity>().eq("username", account).or().eq("mobile", account));
        if (entity == null) {
            return null;
        }
        String encode = entity.getPassword();
        boolean matches = passwordEncoder.matches(password, encode);
        if (matches) {
            return entity;
        }
        return null;
    }

    /**
     * 登录和注册
     * <p>
     * 已注册：数据库中更新令牌，并返回含最新令牌的对象
     * 未注册：使用令牌中搜集相关字段信息,添加新用户
     * <p>
     * 令牌：有效期会，所以登陆时也要更新
     * dao.updateById 需要空表单（对象）设置id编号
     *
     * @param socialUser 社交用户
     * @return {@link MemberEntity}
     */
    @Override
    public MemberEntity login(SocialUser socialUser) {
        MemberDao dao = this.baseMapper;
        MemberEntity entity = dao.selectOne(new QueryWrapper<MemberEntity>().eq(
                "social_uid", socialUser.getSocialUid()
        ));
        if (entity != null) {
            return updateTokenAndGetMemberEntity(socialUser, dao, entity);
        }
        MemberEntity register = registerByToken(socialUser);
        dao.insert(register);
        return register;
    }

    /**
     * 注册令牌
     * 网络请求可能失败，最后都要保存关键的令牌信息
     * JSON.parseObject()，可以转化成类，也可以操作 JSON 获取字段
     *
     * @param socialUser 社会用户
     * @return {@link MemberEntity}
     */
    private MemberEntity registerByToken(SocialUser socialUser) {
        String token = socialUser.getAccessToken();
        MemberGiteeUserInfo info = fetchUserInfo(token);

        MemberEntity register = new MemberEntity();
        MemberLevelEntity levelEntity = memberLevelDao.queryDefaultLevel();
        Long levelId = levelEntity.getId();

        register.setLevelId(levelId);
        register.setHeader(info.getAvatar_url());
        register.setEmail(info.getEmail());
        register.setNickname(info.getName());

        register.setSocialUid(String.valueOf(info.getId()));
        register.setAccessToken(socialUser.getAccessToken());
        register.setExpiresIn(socialUser.getExpiresIn());
        return register;
    }

    public MemberGiteeUserInfo fetchUserInfo(String accessToken) {
        String giteeApi = "https://gitee.com/api/v5/user";
        Map<String, String> headers = new HashMap<>(0);
        Map<String, String> query = new HashMap<>(1);
        query.put("access_token", accessToken);
        try {
            HttpResponse response = HttpUtils.doGet(giteeApi, "", "GET", headers, query);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                String s = EntityUtils.toString(response.getEntity());
                return JSON.parseObject(s, MemberGiteeUserInfo.class);
            } else {
                log.info("拉取用户信息失败： {}", statusCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 更新令牌,反馈成员实体
     * 获取 MyBatis 自动生成的 id 才能更新
     *
     * @param socialUser 社会用户
     * @param dao        刀
     * @param entity     实体
     * @return {@link MemberEntity}
     */
    private MemberEntity updateTokenAndGetMemberEntity(SocialUser socialUser, MemberDao dao, MemberEntity entity) {
        MemberEntity update = new MemberEntity();
        String accessToken = socialUser.getAccessToken();
        String expiresIn = socialUser.getExpiresIn();
        Long id = entity.getId();

        update.setId(id);
        update.setAccessToken(accessToken);
        update.setExpiresIn(expiresIn);
        dao.updateById(update);

        entity.setAccessToken(accessToken);
        entity.setExpiresIn(expiresIn);
        return entity;
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

}