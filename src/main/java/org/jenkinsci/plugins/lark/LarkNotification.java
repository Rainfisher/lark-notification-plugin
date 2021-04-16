package org.jenkinsci.plugins.lark;

import com.arronlong.httpclientutil.exception.HttpProcessException;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.lark.dto.BuildBeginInfo;
import org.jenkinsci.plugins.lark.dto.BuildMentionedInfo;
import org.jenkinsci.plugins.lark.dto.BuildOverInfo;
import org.jenkinsci.plugins.lark.model.NotificationConfig;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.PrintStream;

/**
 * 飞书构建通知
 *
 * @author jiaju
 */
public class LarkNotification extends Publisher implements SimpleBuildStep {

    private String webhookUrl;

    private String mentionedId;

    private boolean failNotify;

    private String projectName;

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    @DataBoundConstructor
    public LarkNotification() {
    }

    /**
     * 开始执行构建
     */
    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
        EnvVars envVars;
        try {
            envVars = build.getEnvironment(listener);
        } catch (Exception e) {
            listener.getLogger().println("读取环境变量异常" + e.getMessage());
            envVars = new EnvVars();
        }
        NotificationConfig config = getConfig(envVars);
        if (StringUtils.isEmpty(config.webhookUrl)) {
            return true;
        }
        this.projectName = build.getProject().getFullDisplayName();
        BuildBeginInfo buildInfo = new BuildBeginInfo(this.projectName, build, config);

        String req = buildInfo.toJSONString();
        listener.getLogger().println("推送通知" + req);

        //执行推送
        push(listener.getLogger(), config.webhookUrl, req, config);
        return true;
    }

    /**
     * 构建结束
     */
    @Override
    public void perform(Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        NotificationConfig config = getConfig(run.getEnvironment(listener));
        if (StringUtils.isEmpty(config.webhookUrl)) {
            return;
        }
        Result result = run.getResult();

        //设置当前项目名称
        if (run instanceof AbstractBuild) {
            this.projectName = run.getParent().getFullDisplayName();
        }

        //构建结束通知
        BuildOverInfo buildInfo = new BuildOverInfo(this.projectName, run, config);

        String req = buildInfo.toJSONString();
        listener.getLogger().println("推送通知" + req);

        //推送结束通知
        push(listener.getLogger(), config.webhookUrl, req, config);
        listener.getLogger().println("项目运行结果[" + result + "]");

        //运行不成功
        if (result == null) {
            return;
        }

        //仅在失败的时候，才进行@
        if (!result.equals(Result.SUCCESS) || !config.failNotify) {
            //没有填写UserId和手机号码
            if (StringUtils.isEmpty(config.mentionedId)) {
                return;
            }

            //构建@通知
            BuildMentionedInfo consoleInfo = new BuildMentionedInfo(run, config);

            req = consoleInfo.toJSONString();
            listener.getLogger().println("推送通知" + req);
            //执行推送
            push(listener.getLogger(), config.webhookUrl, req, config);
        }
    }

    /**
     * 推送消息
     */
    private void push(PrintStream logger, String url, String data, NotificationConfig config) {
        String[] urls;
        if (url.contains(",")) {
            urls = url.split(",");
        } else {
            urls = new String[]{url};
        }
        for (String u : urls) {
            try {
                String msg = NotificationUtil.push(u, data, config);
                logger.println("通知结果" + msg);
            } catch (HttpProcessException e) {
                logger.println("通知异常" + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    /**
     * 读取配置，将当前Job与全局配置整合
     */
    public NotificationConfig getConfig(EnvVars envVars) {
        NotificationConfig config = DESCRIPTOR.getUnsaveConfig();
        if (StringUtils.isNotEmpty(webhookUrl)) {
            config.webhookUrl = webhookUrl;
        }
        if (StringUtils.isNotEmpty(mentionedId)) {
            config.mentionedId = mentionedId;
        }
        config.failNotify = failNotify;
        //使用环境变量
        if (config.webhookUrl.contains("$")) {
            config.webhookUrl = NotificationUtil.replaceMultipleEnvValue(config.webhookUrl, envVars);
        }
        if (config.mentionedId.contains("$")) {
            config.mentionedId = NotificationUtil.replaceMultipleEnvValue(config.mentionedId, envVars);
        }
        return config;
    }

    /**
     * 下面为GetSet方法，当前Job保存时进行绑定
     **/

    @DataBoundSetter
    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    @DataBoundSetter
    public void setMentionedId(String mentionedId) {
        this.mentionedId = mentionedId;
    }

    @DataBoundSetter
    public void setFailNotify(boolean failNotify) {
        this.failNotify = failNotify;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public String getMentionedId() {
        return mentionedId;
    }

    public boolean isFailNotify() {
        return failNotify;
    }
}

