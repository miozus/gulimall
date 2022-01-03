package cn.miozus.gulimall.plugins.component;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

/**
 * 短信组件
 * docs:https://market.aliyun.com/products/57126001/cmapi024822.html
 * 名称	类型	是否必须	描述
 * code	STRING	必选	要发送的验证码
 * phone STRING	必选	接收人的手机号
 * sign	STRING	可选	签名编号【联系客服人员申请，测试请用 1】
 * skin	STRING	必选	模板编号【联系旺旺客服申请，测试请用 1~21
 * appcode  开通服务后 买家中心-查看AppCode
 * host 请求地址 支持http 和 https 及 WEBSOCKET
 * path 请求地址后缀
 * @author miao
 * @date 2021/12/28
 */
@Data
@Component
public class SmsComponent {

    @Value("${spring.alicloud.sms.host}")
    private String host;
    @Value("${spring.alicloud.sms.path}")
    private String path;
    @Value("${spring.alicloud.sms.appcode}")
    private String appcode;
    @Value("${spring.alicloud.sms.sign}")
    private String sign;
    @Value("${spring.alicloud.sms.skin}")
    private String skin;


    public void sendSMS(String phone, String code) {
        String urlSend = host + path + "?code=" + code + "&phone=" + phone + "&sign=" + sign + "&skin=" + skin; // 【5】拼接请求链接
        try {
            URL url = new URL(urlSend);
            HttpURLConnection httpURLCon = (HttpURLConnection) url.openConnection();
            httpURLCon.setRequestProperty("Authorization", "APPCODE " + appcode);// 格式Authorization:APPCODE
            // (中间是英文空格)
            int httpCode = httpURLCon.getResponseCode();
            if (httpCode == 200) {
                String json = read(httpURLCon.getInputStream());
                System.out.println("正常请求计费(其他均不计费)");
                System.out.println("获取返回的json:");
                System.out.print(json);
            } else {
                Map<String, List<String>> map = httpURLCon.getHeaderFields();
                String error = map.get("X-Ca-Error-Message").get(0);
                if (httpCode == 400 && error.equals("Invalid AppCode `not exists`")) {
                    System.out.println("AppCode错误 ");
                } else if (httpCode == 400 && error.equals("Invalid Url")) {
                    System.out.println("请求的 Method、Path 或者环境错误");
                } else if (httpCode == 400 && error.equals("Invalid Param Location")) {
                    System.out.println("参数错误");
                } else if (httpCode == 403 && error.equals("Unauthorized")) {
                    System.out.println("服务未被授权（或URL和Path不正确）");
                } else if (httpCode == 403 && error.equals("Quota Exhausted")) {
                    System.out.println("套餐包次数用完 ");
                } else {
                    System.out.println("参数名错误 或 其他错误");
                    System.out.println(error);
                }
            }

        } catch (MalformedURLException e) {
            System.out.println("URL格式错误");
        } catch (UnknownHostException e) {
            System.out.println("URL地址错误");
        } catch (Exception e) {
            // 打开注释查看详细报错异常信息
            e.printStackTrace();
        }
    }

    /** 读取返回结果 */
    private static String read(InputStream is) throws IOException {
        StringBuffer sb = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        while ((line = br.readLine()) != null) {
            line = new String(line.getBytes(), "utf-8");
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }


}
