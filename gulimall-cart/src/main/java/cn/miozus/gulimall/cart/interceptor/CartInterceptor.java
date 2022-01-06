package cn.miozus.gulimall.cart.interceptor;

import cn.miozus.common.constant.AuthServerConstant;
import cn.miozus.common.constant.CartConstant;
import cn.miozus.common.vo.MemberRespVo;
import cn.miozus.gulimall.cart.to.UserInfoTo;
import com.alibaba.cloud.commons.lang.StringUtils;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

/**
 * 购物车拦截器
 *
 * 在本地线程变量中封装登录/临时用户信息，通过浏览器的 cookie 作为媒介
 *
 * @author miao
 * @date 2022/01/04
 */
public class CartInterceptor implements HandlerInterceptor {

    public static ThreadLocal<UserInfoTo> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        return checkLoginStateAndSetTempUserByThreadLocal(request);
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        setCookieAtBrowser(response);
    }

    /**
     * 检查登录状态并设置cookie
     * <p>
     * 执行目标方法之前，判断用户登录状态，封装传递给控制器的目标请求
     * <p>
     * 已登录：session 有痕迹，取之
     * 未登录（首次）：自动（创建）发放临时 user-key ，反馈请求中操作浏览器设置 cookie（有效期一个月)，用于判断是否登录
     * <p>
     * 最后全部放行
     *
     * @param request 请求
     * @return boolean
     */
    private boolean checkLoginStateAndSetTempUserByThreadLocal(HttpServletRequest request) {
        HttpSession session = request.getSession();
        MemberRespVo member = (MemberRespVo) session.getAttribute(AuthServerConstant.LOGIN_USER);

        UserInfoTo userInfo = new UserInfoTo();

        if (member != null) {
            userInfo.setUserId(member.getId());
        }

        Cookie[] cookies = request.getCookies();
        if (ArrayUtils.isNotEmpty(cookies)) {
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                if (name.equalsIgnoreCase(CartConstant.TEMP_USER_COOKIE_NAME)) {
                    userInfo.setUserKey(cookie.getValue());
                    userInfo.setTempUser(true);
                }
            }
        }

        String userKey = userInfo.getUserKey();
        if (StringUtils.isEmpty(userKey)) {
            String uuid = UUID.randomUUID().toString();
            userInfo.setUserKey(uuid);
        }

        threadLocal.set(userInfo);
        return true;
    }

    /**
     * 保存cookie在浏览器
     * <p>
     * 已存在：期限一个月，未修改
     * 未存在：设置新的过期时间，共计 1 次
     *
     * @param response 响应
     */
    private void setCookieAtBrowser(HttpServletResponse response) {
        UserInfoTo userInfoTo = threadLocal.get();
        if (Boolean.FALSE.equals(userInfoTo.isTempUser())) {
            Cookie cookie = new Cookie(CartConstant.TEMP_USER_COOKIE_NAME, userInfoTo.getUserKey());
            cookie.setDomain("gulimall.com");
            cookie.setMaxAge(CartConstant.TEMP_USER_COOKIE_TIMEOUT);
            response.addCookie(cookie);
        }
    }

}
