package cn.miozus.gulimall.seckill.exception;

import cn.hutool.json.JSONUtil;
import cn.miozus.gulimall.common.enume.BizCodeEnum;
import cn.miozus.gulimall.common.utils.R;
import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 秒杀块异常处理程序
 *
 * @author Miozus
 * @date 2022/03/30
 */
@Configuration
public class SeckillBlockExceptionHandler implements BlockExceptionHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, BlockException e) throws Exception {
        R error = R.error(BizCodeEnum.TOO_MANY_REQUESTS);
        response.setHeader("Content-Type", "application/json;charset=UTF-8");
        response.getWriter().write(JSONUtil.toJsonStr(error));
    }
}