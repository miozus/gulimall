package cn.miozus.gulimall.order.interceptor;

import cn.miozus.common.constant.AuthServerConstant;
import cn.miozus.common.vo.MemberRespVo;
import com.alibaba.nacos.common.utils.Objects;
import lombok.SneakyThrows;
import org.springframework.util.AntPathMatcher;
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
    public static ThreadLocal<MemberRespVo> loginUserThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (callBetweenFeignService(request)) {
            return true;
        }
        return releaseLoginUserOnly(request, response);
    }

    private boolean callBetweenFeignService(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean match = new AntPathMatcher().match("/order/order/SN/**", uri);
        if (match) {
            return true;
        }
        return false;
    }

    /**
     * 只释放登录用户
     * 例外：路径匹配，则放行，用于微服务之间调用
     *
     * @param request  请求
     * @param response 响应
     * @return boolean
     */
    @SneakyThrows
    private boolean releaseLoginUserOnly(HttpServletRequest request, HttpServletResponse response) {

        HttpSession session = request.getSession();
        MemberRespVo loginUser = (MemberRespVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (Objects.isNull(loginUser)) {
            session.setAttribute("msg", "请登录再试");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
        LoginUserInterceptor.loginUserThreadLocal.set(loginUser);
        return true;

    }
}
