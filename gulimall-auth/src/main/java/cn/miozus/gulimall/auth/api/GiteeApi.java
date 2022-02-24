package cn.miozus.gulimall.auth.api;

import cn.miozus.gulimall.auth.vo.GiteeUserInfo;
import cn.miozus.gulimall.auth.vo.GithubUserInfo;
import cn.miozus.gulimall.auth.vo.SocialUser;
import cn.miozus.common.utils.HttpUtils;
import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2.0 认证服务组件
 *
 * @author miao
 * @date 2021/12/28
 */
@Data
@Component
@Slf4j
public class GiteeApi {

    @Value("${spring.oauth.gitee.redirect-uri}")
    private String redirectURI;
    @Value("${spring.oauth.gitee.host}")
    private String host;
    @Value("${spring.oauth.gitee.path}")
    private String path;
    @Value("${spring.oauth.gitee.api}")
    private String api;
    @Value("${spring.oauth.gitee.client-id}")
    private String clientId;
    @Value("${spring.oauth.gitee.client-secret}")
    private String clientSecret;


    /**
     * 获取令牌
     * uid 需要用令牌查询（额外发送一次 HTTP 请求）
     *
     * @param code 代码
     * @return {@link SocialUser}
     */
    public SocialUser fetchToken(String code) {
        Map<String, String> headers = new HashMap<>(0);
        Map<String, String> query = new HashMap<>(4);
        query.put("grant_type", "authorization_code");
        query.put("client_id", clientId);
        query.put("client_secret", clientSecret);
        query.put("redirect_uri", redirectURI);
        query.put("code", code);
        try {
            HttpResponse response = HttpUtils.doPost(host, path, "POST", headers, query, "");
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                String s = EntityUtils.toString(response.getEntity());
                SocialUser socialUser = JSON.parseObject(s, SocialUser.class);
                GiteeUserInfo giteeUserInfo = fetchUserInfo(socialUser.getAccessToken());
                Long id = giteeUserInfo.getId();
                socialUser.setSocialUid(id);
                return socialUser;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取用户信息
     * 纠结：远程调用好呢，还是直接拷贝？weibo wechat QQ github 每个API 都封装一次？
     *
     * @param accessToken 令牌
     * @return {@link GithubUserInfo}
     */
    public GiteeUserInfo fetchUserInfo(String accessToken) {
        Map<String, String> headers = new HashMap<>(0);
        Map<String, String> query = new HashMap<>(1);
        query.put("access_token", accessToken);
        try {
            HttpResponse response = HttpUtils.doGet(api, "", "GET", headers, query);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                String s = EntityUtils.toString(response.getEntity());
                return JSON.parseObject(s, GiteeUserInfo.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}