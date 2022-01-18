package cn.miozus.gulimall.order.interceptor;

import cn.miozus.common.constant.AuthServerConstant;
import cn.miozus.common.vo.MemberRespVo;
import com.alibaba.nacos.common.utils.Objects;
import lombok.SneakyThrows;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * 结算清单拦截器
 * <p>
 * 只允许登录用户通过
 * 在本地线程变量中封装登录/临时用户信息，通过浏览器的 cookie 作为媒介
 *
 * @author miao
 * @date 2022/01/04
 */
public class LoginUserInterceptor implements HandlerInterceptor {

    /**
     * 让其他服务共享
     */
    public static ThreadLocal<MemberRespVo> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        return releaseLoginUserOnly(request, response);
    }

    @SneakyThrows
    private boolean releaseLoginUserOnly(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession();
        MemberRespVo loginUser = (MemberRespVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (Objects.isNull(loginUser)) {
            session.setAttribute("msg", "请登录再试");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
        threadLocal.set(loginUser);
        return true;

    }
}
