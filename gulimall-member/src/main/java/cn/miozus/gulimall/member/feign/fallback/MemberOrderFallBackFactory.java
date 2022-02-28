package cn.miozus.gulimall.member.feign.fallback;

import cn.miozus.gulimall.common.utils.R;
import cn.miozus.gulimall.member.service.MemberWebService;
import feign.hystrix.FallbackFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 会员订单回落
 *
 * @author miozus
 * @date 2022/02/28
 */
@Component
@Slf4j
public class MemberOrderFallBackFactory implements FallbackFactory<MemberWebService> {
    @Override
    public MemberWebService create(Throwable throwable) {
        log.error("异常原因:{}", throwable.getMessage(), throwable);
        return new MemberWebService() {
            @Override
            public R renderPage(String pageNum) {
                //出现异常，自定义返回内容，保证接口安全
                return R.error(throwable.getMessage());
            }

        };
    }
}
