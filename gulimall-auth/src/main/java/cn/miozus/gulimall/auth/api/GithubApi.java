package cn.miozus.gulimall.auth.api;

import cn.miozus.gulimall.auth.vo.GithubUserInfo;
import cn.miozus.gulimall.auth.vo.SocialUser;
import cn.miozus.common.utils.HttpUtils;
import com.alibaba.fastjson.JSON;
import com.google.common.base.Splitter;
import lombok.Data;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2.0 认证服务组件
 * @author miao
 * @date 2021/12/28
 */
@Data
@Component
public class GithubApi {

    @Value("${spring.oauth.github.redirect-uri}")
    private String redirectURI;
    @Value("${spring.oauth.github.host}")
    private String host;
    @Value("${spring.oauth.github.path}")
    private String path;
    @Value("${spring.oauth.github.api}")
    private String api;
    @Value("${spring.oauth.github.client-id}")
    private String clientId;
    @Value("${spring.oauth.github.client-secret}")
    private String clientSecret;


    public SocialUser fetchToken(String code) {
        Map<String, String> headers = new HashMap<>(0);
        Map<String, String> query = new HashMap<>(4);
        query.put("client_id", clientId);
        query.put("client_secret", clientSecret);
        query.put("redirect_uri", redirectURI);
        query.put("code", code);
        try {
            HttpResponse response = HttpUtils.doPost(host, path, "POST", headers, query, "");
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                String s = EntityUtils.toString(response.getEntity());
                Map<String, String> map = Splitter.on("&").withKeyValueSeparator("=").split(s);
                SocialUser user = new SocialUser();
                String accessToken = map.get("access_token");
                GithubUserInfo githubUserInfo = fetchUserInfo(accessToken);
                user.setAccessToken(accessToken);
                user.setSocialUid(githubUserInfo.getId());
                return user;
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
     * @param accessToken 访问令牌
     * @return {@link GithubUserInfo}
     */
    public GithubUserInfo fetchUserInfo(String accessToken) {
        Map<String, String> headers = new HashMap<>(1);
        headers.put("Authorization", "token " + accessToken);
        Map<String, String> query = new HashMap<>(0);
        try {
            HttpResponse response = HttpUtils.doGet(api, "", "GET", headers, query);
            String s = EntityUtils.toString(response.getEntity());
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                return JSON.parseObject(s, GithubUserInfo.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}