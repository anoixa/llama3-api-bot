package moe.imtop1.chatbot.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * 工具类
 * @author anoixa
 */
public class ToolUtils {
    public static String CORE = "(\\d+)\\s+";
    private static final String[][] ESCAPES_MARKDOWN = {
            {"\\", "\\\\"},
            {"_", "\\_"},
            {"*", "\\*"},
            {"[", "\\["},
            {"]", "\\]"},
            {"(", "\\("},
            {")", "\\)"},
            {"~", "\\~"},
            {"`", "\\`"},
            {"#", "\\#"},
            {"+", "\\+"},
            {"-", "\\-"},
            {"=", "\\="},
            {"{", "\\{"},
            {"}", "\\}"},
            {"|", "\\|"},
            {".", "\\."}
    };


    /**
     * 判断字符串中是否含有某字符
     * @param str1 待检测的字符串
     * @param str2 需要检测存在的字符串
     * @return boolean
     */
    public static boolean containsSlash(String str1, String str2) {
        return str1.contains(str2);
    }

    /**
     * 获取当前时间
     * @return 当前时间字符串
     */
    public static String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC+8"));

        return sdf.format(new Date());
    }

    /**
     * 转义 Markdown 特殊字符
     *
     * @param input 输入的字符串
     * @return 转义后的字符串
     */
    public static String escapeMarkdown(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        for (String[] escape : ESCAPES_MARKDOWN) {
            result = result.replace(escape[0], escape[1]);
        }
        return result;
    }

    public static String escape(String s) {
        if (s == null) {
            return null;
        }
        List<String> list = Arrays.asList("#", "!", "*", "_", "~", "+", "-", ">", "<", "[", "]", "`", ".", "^", "=", "{", "}", "(", ")");
        for (String l : list) {
            s = s.replace(l, "\\" + l);
        }
        return s;
    }

}
