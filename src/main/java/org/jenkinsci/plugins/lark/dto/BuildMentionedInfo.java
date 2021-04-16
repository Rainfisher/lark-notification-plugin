package org.jenkinsci.plugins.lark.dto;

import hudson.model.Run;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.lark.model.NotificationConfig;

import java.util.*;

/**
 * `@`用户的通知
 *
 * @author jiaju
 */
public class BuildMentionedInfo {

    /**
     * 通知ID
     */
    private String mentionedId = "";

    public BuildMentionedInfo(Run<?, ?> run, NotificationConfig config) {
        //通知ID
        if (config.mentionedId != null) {
            mentionedId = config.mentionedId;
        }
    }

    public String toJSONString() {
        List<String> mentionedIdList = new ArrayList<>();
        if (StringUtils.isNotEmpty(mentionedId)) {
            String[] ids = mentionedId.split(",");
            mentionedIdList.addAll(Arrays.asList(ids));
        }

        Map<String, Object> post = new HashMap<>();
        Map<String, Object> zhcn = new HashMap<>();
        List<Object> contents = new ArrayList<>();
        for (String id : mentionedIdList) {
            List<Object> list = new ArrayList<>();
            Map<String, Object> objs = new HashMap<>();
            objs.put("tag", "at");
            objs.put("user_id", id);
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
