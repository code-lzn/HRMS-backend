package com.limou.hrms.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * 钉钉群机器人告警工具
 * <p>
 * 消息内容必须包含关键字，否则发送失败。
 */
@Slf4j
public class DingTalkUtil {

    private static final String WEBHOOK_URL =
            "https://oapi.dingtalk.com/robot/send?access_token=216e031cae4933d85c4a2261348e4e4dfdffe04cc04ce398ea10a2f3ee6c2447";

    private static final String KEYWORD = "HRMS";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 发送文本消息 */
    public static boolean sendText(String content) {
        return sendText(content, Collections.emptyList());
    }

    /** 发送文本消息并 @指定手机号 */
    public static boolean sendText(String content, List<String> atMobiles) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", "【" + KEYWORD + "】" + content);
        return doSend("text", payload, atMobiles);
    }

    /** 发送 Markdown 消息 */
    public static boolean sendMarkdown(String title, String text) {
        return sendMarkdown(title, text, Collections.emptyList());
    }

    /** 发送 Markdown 消息并 @指定手机号 */
    public static boolean sendMarkdown(String title, String text, List<String> atMobiles) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("text", "## 【" + KEYWORD + "】" + title + "\n" + text);
        return doSend("markdown", payload, atMobiles);
    }

    @SuppressWarnings("unchecked")
    private static boolean doSend(String msgtype, Map<String, Object> payload, List<String> atMobiles) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", msgtype);
            body.put(msgtype, payload);

            if (atMobiles != null && !atMobiles.isEmpty()) {
                Map<String, Object> at = new HashMap<>();
                at.put("atMobiles", atMobiles);
                body.put("at", at);
            }

            HttpURLConnection conn = (HttpURLConnection) new URL(WEBHOOK_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(MAPPER.writeValueAsBytes(body));
                os.flush();
            }

            int code = conn.getResponseCode();
            if (code == 200) {
                log.info("钉钉消息发送成功: {}", String.valueOf(payload.get("content")));
                return true;
            } else {
                String err = new String(conn.getErrorStream().readAllBytes());
                log.error("钉钉消息发送失败 code={}, body={}", code, err);
                return false;
            }
        } catch (Exception e) {
            log.error("钉钉消息发送异常", e);
            return false;
        }
    }
}
