package org.jenkinsci.plugins.lark.dto;

import hudson.model.AbstractBuild;
import hudson.model.ParameterValue;
import hudson.model.ParametersAction;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.lark.NotificationUtil;
import org.jenkinsci.plugins.lark.model.NotificationConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 开始构建的通知信息
 *
 * @author jiaju
 */
public class BuildBeginInfo {

    /**
     * 请求参数
     */
    private final Map<String, Object> params = new HashMap<>();

    /**
     * 预计时间，毫秒
     */
    private Long durationTime = 0L;

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

    public BuildBeginInfo(String projectName, AbstractBuild<?, ?> build, NotificationConfig config) {
        //获取请求参数
        List<ParametersAction> parameterList = build.getActions(ParametersAction.class);
        if (parameterList != null && parameterList.size() > 0) {
            for (ParametersAction p : parameterList) {
                for (ParameterValue pv : p.getParameters()) {
                    this.params.put(pv.getName(), pv.getValue());
                }
            }
        }
        //预计时间
        if (build.getProject().getEstimatedDuration() > 0) {
            this.durationTime = build.getProject().getEstimatedDuration();
        }
        //控制台地址
        StringBuilder urlBuilder = new StringBuilder();
        String jenkinsUrl = NotificationUtil.getJenkinsUrl();
        if (StringUtils.isNotEmpty(jenkinsUrl)) {
            String buildUrl = build.getUrl();
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
    }

    public String toJSONString() {
        //参数组装
        StringBuffer paramBuffer = new StringBuffer();
        params.forEach((key, val) -> {
            paramBuffer.append(key);
            paramBuffer.append("=");
            paramBuffer.append(val);
            paramBuffer.append(", ");
        });
        if (paramBuffer.length() == 0) {
            paramBuffer.append("无");
        } else {
            paramBuffer.deleteCharAt(paramBuffer.length() - 2);
        }

        //耗时预计
        String durationTimeStr = "无";
        if (durationTime > 0) {
            Long l = durationTime / (1000 * 60);
            durationTimeStr = l + "分钟";
        }

        //组装内容
        Map<String, Object> post = new HashMap<>();
        Map<String, Object> zhcn = new HashMap<>();
        List<Object> contents = new ArrayList<>();
        {
            //第一行
            List<Object> list = new ArrayList<>();
            Map<String, Object> objs = new HashMap<>();
            objs.put("tag", "text");
            objs.put("text", String.format("%s【%s】开始构建", topicName, projectName));
            list.add(objs);
            contents.add(list);
        }
        {
            //构建参数
            List<Object> list = new ArrayList<>();
            Map<String, Object> objs = new HashMap<>();
            objs.put("tag", "text");
            objs.put("text", String.format(" >构建参数：%s", paramBuffer));
            list.add(objs);
            contents.add(list);
        }
        {
            //预计用时
            List<Object> list = new ArrayList<>();
            Map<String, Object> objs = new HashMap<>();
            objs.put("tag", "text");
            objs.put("text", String.format(" >预计用时：%s", durationTimeStr));
            list.add(objs);
            contents.add(list);
        }
        if (StringUtils.isNotEmpty(this.consoleUrl)) {
            //预计用时
            List<Object> list = new ArrayList<>();
            Map<String, Object> objs = new HashMap<>();
            objs.put("tag", "a");
            objs.put("text", "[查看控制台]");
            objs.put("href", this.consoleUrl);
            list.add(objs);
            contents.add(list);
        }

        zhcn.put("content", contents);
        post.put("zh_cn", zhcn);
        Map<String, Object> text = new HashMap<>();
        text.put("post", post);
        Map<String, Object> data = new HashMap<>();
        data.put("msg_type", "post");
        data.put("content", text);

        return JSONObject.fromObject(data).toString();
    }


}
