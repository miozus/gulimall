package cn.miozus.auth;

import cn.miozus.auth.api.GiteeApi;
import cn.miozus.auth.api.GithubApi;
import cn.miozus.auth.vo.GiteeUserInfo;
import cn.miozus.auth.vo.GithubUserInfo;
import cn.miozus.common.constant.RegexConstant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
class GulimallAuthServerApplicationTests {

    @Autowired
    GiteeApi giteeApi;

    @Autowired
    GithubApi githubApi;

    @Test
    void contextLoads() {
    }

    @Test
    void testFetchGiteeUserInfo(){
        String token = "";
        GiteeUserInfo info = giteeApi.fetchUserInfo(token);
    }
    @Test
    void testFetchGithubUserInfo(){
        String token = "";
        GithubUserInfo info = githubApi.fetchUserInfo(token);
    }

    @Test
    void testChinaMobileRegex(){
        String s = "15012341234";
        Matcher matcher = Pattern.compile(RegexConstant.CHINA_MOBILE).matcher(s);
        System.out.println("matcher.matches() = " + matcher.matches());

    }

}
