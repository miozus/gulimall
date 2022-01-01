package cn.miozus.auth;

import cn.miozus.common.constant.RegexConstant;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

//@SpringBootTest
class GulimallAuthServerApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void testChinaMobileRegex(){
        String s = "15012341234";
        Matcher matcher = Pattern.compile(RegexConstant.CHINA_MOBILE).matcher(s);
        System.out.println("matcher.matches() = " + matcher.matches());

    }

}
