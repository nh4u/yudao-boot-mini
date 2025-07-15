package cn.bitlinks.ems.module.system.framework.mail;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlFormatter {

    public static String formatHtml(String template) {
        if (StrUtil.isBlank(template)) return "";

        // 1. 定义正则表达式，匹配 <table> 标签及其内部的所有内容（包括换行）
        Pattern tablePattern = Pattern.compile("<table[^>]*>.*?</table>", Pattern.DOTALL);
        Matcher tableMatcher = tablePattern.matcher(template);

        StringBuffer sb = new StringBuffer();
        int lastEnd = 0;

        while (tableMatcher.find()) {
            // 2. 处理 <table> 标签之前的内容 (转义 + 保留换行 + 排除 <a> 标签)
            String textBeforeTable = template.substring(lastEnd, tableMatcher.start());
            String escapedBeforeTable = escapeAndPreserveNewlines(textBeforeTable);
            sb.append(escapedBeforeTable);

            // 3. 直接追加 <table> 标签匹配到的内容（不转义）
            sb.append(tableMatcher.group());

            lastEnd = tableMatcher.end();
        }

        // 4. 处理最后一个 </table> 标签之后的内容 (转义 + 保留换行 + 排除 <a> 标签)
        String textAfterLastTable = template.substring(lastEnd);
        String escapedAfterLastTable = escapeAndPreserveNewlines(textAfterLastTable);
        sb.append(escapedAfterLastTable);

        return sb.toString();
    }

    // 辅助函数：转义 HTML 特殊字符，保留换行符，并排除 <a> 标签
    private static String escapeAndPreserveNewlines(String text) {
        StringBuilder sb = new StringBuilder();
        Pattern linkPattern = Pattern.compile("<a[^>]*>.*?</a>", Pattern.DOTALL);
        Matcher linkMatcher = linkPattern.matcher(text);
        int lastEnd = 0;

        while (linkMatcher.find()) {
            // 处理 <a> 标签之前的内容
            String textBeforeLink = text.substring(lastEnd, linkMatcher.start());
            sb.append(escapeHtml(textBeforeLink));

            // 直接追加 <a> 标签匹配到的内容
            sb.append(linkMatcher.group());

            lastEnd = linkMatcher.end();
        }

        // 处理最后一个 <a> 标签之后的内容
        String textAfterLastLink = text.substring(lastEnd);
        sb.append(escapeHtml(textAfterLastLink));

        return sb.toString();
    }

    // 辅助函数：转义 HTML 特殊字符，但不包括换行符和 <a> 标签
    private static String escapeHtml(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                case '\n':
                    sb.append("<br/>");
                    break;
                case ' ':
                    sb.append("&nbsp;");
                    break;
                case '\t':
                    sb.append("&nbsp;&nbsp;&nbsp;&nbsp;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

}