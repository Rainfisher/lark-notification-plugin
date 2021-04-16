package org.jenkinsci.plugins.lark.dto;

import hudson.model.Result;
import hudson.model.Run;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.lark.NotificationUtil;
import org.jenkinsci.plugins.lark.model.NotificationConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * 结束构建的通知信息
 *
 * @author jiaju
 */
public class BuildOverInfo {

    /**
     * 使用时间，毫秒
     */
    private String useTimeString = "";

    /**
     * 本次构建控制台地址
     */
    private final String consoleUrl;

    /**
     * 工程名称
     */
    private final String projectName;

    /**
     * 环境名称
     */
    private String topicName = "";

    /**
     * 执行结果
     */
    private final Result result;

    public BuildOverInfo(String projectName, Run<?, ?> run, NotificationConfig config) {
        //使用时间
        this.useTimeString = run.getTimestampString();
        //控制台地址
        StringBuilder urlBuilder = new StringBuilder();
        String jenkinsUrl = NotificationUtil.getJenkinsUrl();
        if (StringUtils.isNotEmpty(jenkinsUrl)) {
            String buildUrl = run.getUrl();
            urlBuilder.append(jenkinsUrl);
            if (!jenkinsUrl.endsWith("/")) {
                urlBuilder.append("/");
            }
            urlBuilder.append(buildUrl);
            if (!buildUrl.endsWith("/")) {
                urlBuilder.append("/");
            }
            urlBuilder.append("console");
        }
        this.consoleUrl = urlBuilder.toString();
        //工程名称
        this.projectName = projectName;
        //环境名称
        if (config.topicName != null) {
            topicName = config.topicName;
        }
        //结果
        result = run.getResult();
    }

    public String toJSONString() {
        //组装内容
        StringBuilder content = new StringBuilder();
        if (StringUtils.isNotEmpty(topicName)) {
            content.append(this.topicName);
        }
        content.append("【" + this.projectName + "】构建" + getStatus() + "\n");
        content.append(" >构建用时：" + this.useTimeString + "\n");
        if (StringUtils.isNotEmpty(this.consoleUrl)) {
            content.append(" >[查看控制台](" + this.consoleUrl + ")");
        }

        Map<String, Object> text = new HashMap<>();
        text.put("text", content.toString());

        Map<String, Object> data = new HashMap<>();
        data.put("msg_type", "text");
        data.put("content", text);

        return JSONObject.fromObject(data).toString();
    }

    private String getStatus() {
        if (null != result && result.equals(Result.FAILURE)) {
            return "失败!!!\uD83D\uDE2D";
        } else if (null != result && result.equals(Result.ABORTED)) {
            return "中断!!\uD83D\uDE28";
        } else if (null != result && result.equals(Result.UNSTABLE)) {
            return "异常!!\uD83D\uDE41";
        } else if (null != result && result.equals(Result.SUCCESS)) {
            int max = successFaces.length - 1, min = 0;
            int ran = (int) (Math.random() * (max - min) + min);
            return "成功~" + successFaces[ran];
        }
        return "情况未知";
    }

    String[] successFaces = {
            "\uD83D\uDE0A", "\uD83D\uDE04", "\uD83D\uDE0E", "\uD83D\uDC4C", "\uD83D\uDC4D", "(o´ω`o)و", "(๑•̀ㅂ•́)و✧"
    };


}
