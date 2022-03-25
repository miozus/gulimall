package cn.miozus.gulimall.seckill.interceptor;

import cn.miozus.gulimall.common.constant.AuthServerConstant;
import cn.miozus.gulimall.common.vo.MemberRespVo;
import com.alibaba.nacos.common.utils.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * 结算清单拦截器
 * <p>
 * 只允许登录用户通过
 * 在本地线程变量中封装登录/临时用户信息，通过浏览器的 cookie 作为媒介
 *
 * @author miao
 * @date 2022/01/04
 */
@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    /**
     * 让其他服务共享
     */
    public static ThreadLocal<MemberRespVo> loginUserThreadLocal = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (isAddToSeckill(request)) {
            return releaseLoginUserOnly(request, response);
        }
        return true;
    }

    private boolean isAddToSeckill(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return new AntPathMatcher().match("/kill/**", uri);
    }

    private boolean callBetweenFeignService(HttpServletRequest request) {
        String uri = request.getRequestURI();
        boolean isMemberAllService = new AntPathMatcher().match("/member/**", uri);
        boolean isFetchSeckillSkus = new AntPathMatcher().match("/currentSeckillSkus", uri);
        boolean isFetchSeckillSku = new AntPathMatcher().match("/sku/seckill/**", uri);
        return isMemberAllService || isFetchSeckillSku || isFetchSeckillSkus;
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
    private boolean releaseLoginUserOnly(HttpServletRequest request, HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession();
        MemberRespVo loginUser = (MemberRespVo) session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (Objects.isNull(loginUser)) {
            session.setAttribute("msg", "请登录再试");
            response.sendRedirect("http://auth.gulimall.com/login.html");
            return false;
        }
        session.removeAttribute("msg");
        LoginUserInterceptor.loginUserThreadLocal.set(loginUser);
        return true;

    }
}
