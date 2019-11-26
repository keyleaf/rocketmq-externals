/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.rocketmq.console.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.dingtalk.chatbot.message.LinkMessage;
import com.mamcharge.dto.RobotMessageDTO;
import com.mamcharge.utils.DingTalkChatBotUtil;
import com.mamcharge.utils.DingTalkUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.common.protocol.body.TopicList;
import org.apache.rocketmq.console.model.ConsumerMonitorConfig;
import org.apache.rocketmq.console.model.GroupConsumeInfo;
import org.apache.rocketmq.console.service.ConsumerService;
import org.apache.rocketmq.console.service.MonitorService;
import org.apache.rocketmq.console.service.TopicService;
import org.apache.rocketmq.console.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MonitorTask {
    private Logger logger = LoggerFactory.getLogger(MonitorTask.class);

    @Resource
    private MonitorService monitorService;

    @Resource
    private ConsumerService consumerService;

    @Resource
    private TopicService topicService;

    @Value("${pushAlarmInfo.dingTalkURL}")
    private String DEFAULT_DING_TALK_URL;

    @Value("${server_url}")
    private String SERVER_URL;

    @Scheduled(cron = "0 0/5 * * * ?")
    public void scanProblemConsumeGroup() {
        logger.info("start scanProblemConsumeGroup");
        String dingTalkURL = DEFAULT_DING_TALK_URL;
        for (Map.Entry<String, ConsumerMonitorConfig> configEntry : monitorService.queryConsumerMonitorConfig().entrySet()) {
            GroupConsumeInfo consumeInfo = consumerService.queryGroup(configEntry.getKey());

            ConsumerMonitorConfig consumerMonitorConfig = configEntry.getValue();
            if (consumerMonitorConfig != null && StringUtils.isNotBlank(consumerMonitorConfig.getDingTalkURL())) {
                dingTalkURL = consumerMonitorConfig.getDingTalkURL();
            }

            if (consumeInfo.getCount() < configEntry.getValue().getMinCount()) {
                // 低于阈值
                String alarmInfo = JsonUtil.obj2String(consumeInfo);
                logger.info("低于阈值 consumeInfo {}", alarmInfo);
                pushAlarmInfo(dingTalkURL, "消息量低于阈值: " + alarmInfo);
            }
            if (consumeInfo.getDiffTotal() > configEntry.getValue().getMaxDiffTotal()) {
                // 高于阈值
                String alarmInfo = JsonUtil.obj2String(consumeInfo);
                logger.info("高于阈值 consumeInfo {}", alarmInfo);
                pushAlarmInfo(dingTalkURL, "消息量高于阈值: " + alarmInfo);
            }
        }
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    public void scanDLQ() {
        logger.info("start scanDLQ");
        TopicList topicList = topicService.fetchAllTopicList(true);
        if (topicList == null) {
            return;
        }
        Set<String> allTopicSet =  topicList.getTopicList();
        if (CollectionUtils.isEmpty(allTopicSet)) {
            return;
        }
        List<JSONObject> dlqList = new ArrayList<>();
        for (String topic : allTopicSet) {
            if (topic.startsWith("%DLQ%")) {
                JSONObject dlqJsonObject = new JSONObject();
                dlqJsonObject.put("topic", topic);
                dlqJsonObject.put("statusURL", SERVER_URL + "/topic/stats.query?topic=" + topic);
                dlqList.add(dlqJsonObject);
            }
        }
        if (CollectionUtils.isEmpty(dlqList)) {
            return;
        }
        LinkMessage linkMessage = new LinkMessage();

        pushAlarmInfo(DEFAULT_DING_TALK_URL, "死信队列如下: \n " + JSONObject.toJSONString(dlqList, SerializerFeature.PrettyFormat));
    }

    /**
     * 推送告警消息
     * @param dingTalkURL 钉钉机器人地址
     * @param alarmInfo 告警信息
     */
    private void pushAlarmInfo(String dingTalkURL, String alarmInfo) {
        if (StringUtils.isBlank(alarmInfo)) {
            return;
        }
        if (StringUtils.isBlank(dingTalkURL)) {
            dingTalkURL = DEFAULT_DING_TALK_URL;
        }
        logger.info("dingTalkURL is : {}", dingTalkURL);
        RobotMessageDTO robotMessageDTO = new RobotMessageDTO("环境信息: \n " + SERVER_URL + " \n " + alarmInfo, null);
        DingTalkUtil.sendMessageToDingTalk(dingTalkURL, robotMessageDTO);

    }
    private void pushLinkMessage(String dingTalkURL, LinkMessage linkMessage) {
        try {
            DingTalkChatBotUtil.sendMessageByUrl(dingTalkURL, linkMessage);
        } catch (IOException e) {
            logger.error("推送钉钉告警消息失败", e);
        }

    }


}
