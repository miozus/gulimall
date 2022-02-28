package cn.miozus.gulimall.order.interceptor;

import cn.miozus.gulimall.common.constant.AuthServerConstant;
import cn.miozus.gulimall.common.vo.MemberRespVo;
import com.alibaba.nacos.common.utils.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class LoginUserInterceptor implements HandlerInterceptor {

    /**
     * 让其他服务共享
     */
    public static ThreadLocal<MemberRespVo> threadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (callBetweenFeignServiceOrNotifyPay(request)) {
            return true;
        }
        return releaseLoginUserOnly(request, response);
    }

    /**
     * 无需登录的路径
     *
     * @param request 请求
     * @return boolean
     */
    private boolean callBetweenFeignServiceOrNotifyPay(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean isNotifyUrl = new AntPathMatcher().match("/notify/pay", uri);
        boolean isOrderFeignService = new AntPathMatcher().match("/order/order/SN/**", uri);
        return isNotifyUrl || isOrderFeignService ;
    }

    /**
     * 只释放登录用户
     * 例外：路径匹配，则放行，用于微服务之间调用
     * attribute 设置过一次，却没有清空，所以手动赋值为空
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
            log.info("订单服务-登录拦截器：可能未登录;也可能熔断导致Feign请求头丢失");
            session.setAttribute("msg", "请登录再试");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
        session.removeAttribute("msg");
        LoginUserInterceptor.threadLocal.set(loginUser);
        return true;

    }
}
