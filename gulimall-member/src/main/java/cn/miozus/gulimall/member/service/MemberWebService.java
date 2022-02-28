package cn.miozus.gulimall.member.service;

import cn.miozus.gulimall.common.utils.R;

/**
 * web服务成员
 * 为了降级，提供 AOP 接口
 *
 * @author miozus
 * @date 2022/02/28
 */
public interface MemberWebService {
    /**
     * 渲染页面
     *
     * @param pageNum 页面num
     * @return {@link R}
     */
    R renderPage(String pageNum);
}
