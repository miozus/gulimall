package cn.miozus.gulimall.member.service;

import cn.miozus.common.utils.PageUtils;
import cn.miozus.gulimall.member.entity.MemberEntity;
import cn.miozus.gulimall.member.exception.PhoneNumberAlreadyExistsException;
import cn.miozus.gulimall.member.exception.UsernameAlreadyExistsException;
import cn.miozus.gulimall.member.vo.MemberLoginVo;
import cn.miozus.gulimall.member.vo.MemberRegisterVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

/**
 * 会员
 *
 * @author SuDongpo
 * @email miozus@outlook.com
 * @date 2021-08-09 14:13:14
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    /**
     * 注册
     *
     * @param vo vo
     */
    void register(MemberRegisterVo vo);

    /**
     * is email unique
     *
     * @param email 电子邮件
     * @return {@link boolean}
     */
    boolean checkEmailUnique(String email);

    /**
     * 检查用户名
     *
     * @param username 用户名
     * @throws UsernameAlreadyExistsException 用户名存在异常
     */
    void checkUsernameUnique(String username) throws UsernameAlreadyExistsException;

    /**
     * 检查用户名存在
     *
     * @param username 用户名
     * @return boolean
     * @throws UsernameAlreadyExistsException 用户名已经存在异常
     */
    boolean checkUsernameExists(String username) ;

    /**
     * 检查手机独特
     *
     * @param phone 电话
     * @throws PhoneNumberAlreadyExistsException cn.miozus.gulimall.member.exception. phone exist exception
     */
    void checkPhoneUnique(String phone) throws PhoneNumberAlreadyExistsException;

    /**
     * 检查登录
     *
     * @param vo 签证官
     * @return
     */
    MemberEntity login(MemberLoginVo vo);
}

