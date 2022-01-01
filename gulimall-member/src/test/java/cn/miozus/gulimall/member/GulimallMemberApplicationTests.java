package cn.miozus.gulimall.member;

import org.apache.commons.codec.digest.Md5Crypt;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

//@SpringBootTest
class GulimallMemberApplicationTests {
    /**
     * 加密器
     *
     */
    @Test
    void testBCryptPasswordEncoder() {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String s = passwordEncoder.encode("123456");
        System.out.println("s = " + s); // s = $2a$10$rPyTGwJa5eM8O6QGks4wcu.hLB9dk.fxcZgASJ5t6nyp1tIk1dhbC
        String md5 = "$2a$10$rPyTGwJa5eM8O6QGks4wcu.hLB9dk.fxcZgASJ5t6nyp1tIk1dhbC";
        boolean matches = passwordEncoder.matches("123456", md5);
        System.out.println("matches = " + matches); // matches = true
    }

    /**
     * 盐值加密
     *
     */
    @Test
    void testMd5CryptAndSalt() {
        // s = $1$RWBwwya2$JAyX7/5DRcM9d94b2NC9d.
        String s = Md5Crypt.md5Crypt("123456".getBytes());
        System.out.println("s = " + s);
        // s = $1$oSDSyf7/$VICV8QPTqa5Uh/0nyTFgi0
        //s1 = $1$qqqq$9SOIgx5OjCEEGZEutEFaP/
        String s1 = Md5Crypt.md5Crypt("123456".getBytes(), "$1$qqqq");
        System.out.println("s1 = " + s1);
        // s = $1$gFSYjfWG$eEDPF9cnYqgPoyClrN7gs/
        //s1 = $1$qqqq$9SOIgx5OjCEEGZEutEFaP/
        //s2 = $1$qqqqnew$tR4esAUsRvpE56c.TPxRY1
        String s2 = Md5Crypt.md5Crypt("123456".getBytes(), "$1$qqqqnew");
        System.out.println("s2 = " + s2);
        // s = $1$Iy7ecQQn$mqg958dcnQ0qiWYr4Dwfr1
        //s1 = $1$qqqq$9SOIgx5OjCEEGZEutEFaP/
        //s2 = $1$qqqqnew$tR4esAUsRvpE56c.TPxRY1
        //s3 = $1$qqqq$BAB1efJYvkOcy2VBw.qor.
        String s3 = Md5Crypt.md5Crypt("654321".getBytes(), "$1$qqqq");
        System.out.println("s3 = " + s3);
    }

}
