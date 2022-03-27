/**
 * Copyright (c) 2016-2019 人人开源 All rights reserved.
 * <p>
 * https://www.renren.io
 * <p>
 * 版权所有，侵权必究！
 */

package cn.miozus.gulimall.common.utils;

import cn.miozus.gulimall.common.enume.BizCodeEnum;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * r
 * 返回数据
 *
 * @author Mark sunlightcs@gmail.com
 * @date 2022/01/18
 */
public class R extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;


    /**
     * 无参构造：ok() 默认返回响应成功，code是0 而非 HTTP 常见的 200;
     */
    public R() {
        put("code", 0);
        put("msg", "success");
    }

    public static R error() {
        return error(HttpStatus.SC_INTERNAL_SERVER_ERROR, "未知异常，请联系管理员");
    }

    public static R error(String msg) {
        return error(HttpStatus.SC_INTERNAL_SERVER_ERROR, msg);
    }

    public static R error(BizCodeEnum e) {
        R r = new R();
        r.put("code", e.value());
        r.put("msg", e.getMsg());
        return r;
    }

    public static R error(int code, String msg) {
        R r = new R();
        r.put("code", code);
        r.put("msg", msg);
        return r;
    }

    public static R ok(String msg) {
        R r = new R();
        r.put("msg", msg);
        return r;
    }

    public static R ok(Map<String, Object> map) {
        R r = new R();
        r.putAll(map);
        return r;
    }

    public static R ok() {
        return new R();
    }


    @Override
    public R put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    /**
     * 获取代码
     * 因为经常用获取状态, 以下自定义 *
     *
     * @return {@link Integer}
     */
    public Integer getCode() {
        return (Integer) this.get("code");
    }

    /**
     * 获取成功提示
     *
     * @return {@link String}
     */
    public String getMsg() {
        return (String) this.get("msg");
    }

    /**
     * 成功
     *
     * @return {@link Boolean}
     */
    public boolean isOk() {
        return getCode() == 0;
    }

    /**
     * 不成功
     *
     * @return boolean
     */
    public boolean isNotOk() {
        return !isOk();
    }


    /**
     * 集数据
     *
     * @param data 数据
     * @return {@link R}
     */
    public R setData(Object data) {
        this.put("data", data);
        return this;
    }

    /**
     * 获取数据
     * 默认键 "data"
     * alibaba-fastjson 设置要转换的类型
     * R<HashMap<"data",Object> > Object > JSON.String > JSON > ReferenceType
     *
     * @param typeReference 引用类型
     * @return {@link T}
     */
    public <T> T getData(TypeReference<T> typeReference) {
        Object data = this.get("data");
        String s = JSON.toJSONString(data);
        return JSON.parseObject(s, typeReference);
    }

    /**
     * 获取数据
     * 自定义键值： 相当于开箱取票码，如有校验字段必须手动写
     * @param typeReference 引用类
     * @return {@link T}
     */
    public <T> T getData(String key, TypeReference<T> typeReference) {
        Object data = this.get(key);
        String s = JSON.toJSONString(data);
        return JSON.parseObject(s, typeReference);
    }
}

