package org.apache.rocketmq.console.util;

import com.alibaba.fastjson.JSONObject;
import com.mamcharge.utils.HttpClientUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.List;

/**
 * 对接OMP告警的工具类
 * Created by TJ on 2019/8/23.
 */
@Slf4j
public class OMPUtil {

    /**
     * OMP告警接口地址
     */
    private static final String URL = "https://grafana.mamcharge.com/webhook/alarm";

    public static BaseResponse<String> pushAlarmToOMP (OMPAlarmVO ompAlarmVO) {
        if (ompAlarmVO == null) {
            return BaseResponse.error("告警参数不能为空");
        }
        if (StringUtils.isEmpty(ompAlarmVO.getRuleId())) {
            return BaseResponse.error("告警参数 ruleId 不能为空");
        }
        if (StringUtils.isEmpty(ompAlarmVO.getRuleName())) {
            return BaseResponse.error("告警参数 ruleName 不能为空");
        }
        if (StringUtils.isEmpty(ompAlarmVO.getRuleUrl())) {
            return BaseResponse.error("告警参数 ruleUrl 不能为空");
        }
        if (StringUtils.isEmpty(ompAlarmVO.getState())) {
            return BaseResponse.error("告警参数 state 不能为空");
        }
        if (StringUtils.isEmpty(ompAlarmVO.getMessage())) {
            return BaseResponse.error("告警参数 message 不能为空");
        }
        if (StringUtils.isEmpty(ompAlarmVO.getTitle())) {
            return BaseResponse.error("告警参数 title 不能为空");
        }
        try {
            String result = HttpClientUtil.getInstance().sendHttpPost(URL, JSONObject.toJSONString(ompAlarmVO));
            log.info("推送OMP告警成功，responseEntity is : {}", result);
            return BaseResponse.success(result);
        } catch (Exception e) {
            log.error("推送OMP告警消息失败", e);
            return BaseResponse.error(e.getMessage());
        }
    }

    /**
     * 封装OMP告警参数
     */
    @Data
    public static class OMPAlarmVO implements Serializable {

        public OMPAlarmVO(String ruleId, String ruleName, String ruleUrl, String state, String message, String title) {
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.ruleUrl = ruleUrl;
            this.state = state;
            this.message = message;
            this.title = title;
        }

        public OMPAlarmVO() {
        }

        /**
         * 告警规则编号
         * 自定义规则编号，添加应用前缀，例如：FS1000
         * 必传
         */
        private String ruleId;

        /**
         * 告警名称
         * 必传
         */
        private String ruleName;

        /**
         * 告警详细地址
         * 必传
         */
        private String ruleUrl;

        /**
         * 告警状态，alerting：告警中，ok：告警解除
         * 必传
         */
        private String state;

        /**
         * 告警信息
         * 必传
         * 参数 message 一定是json字符串，不是json对象！！！！！！！！！！！！！！！
         */
        private String message;

        /**
         * 告警标题
         * 必传
         */
        private String title;

        /**
         * 告警详细数值
         * 非必传
         */
        private List<EvalMatchVO> evalMatches;

    }

    @Data
    public static class Message {

        /**
         * 应用所属小组
         * 平台一部：用户组、商户组、算法组、运营组、数据组、前端组、设备组、接入组、公共技术组、运维组
         * 必传
         */
        private String group;

        /**
         * 告警级别
         * 级别：一级、二级、三级、四级
         * 必传
         */
        private String level;

        /**
         * 通知手机号
         * 多个手机号，英文逗号“,”隔开；
         * 必传
         */
        private String phone;

        /**
         * 告警消息内容
         * 非必传
         */
        private String content;

        /**
         * 钉钉机器人自定义调用地址
         * 非必传
         */
        private String robot;

        /**
         * 是否需要电话呼叫告警
         * 非必传
         */
        private boolean call = false;

        /**
         * 是否同时呼叫直接负责人
         * 非必传
         */
        private boolean director = false;

    }

    @Data
    public static class EvalMatchVO {

        /**
         * 指标名称
         * 必传
         */
        private String metric;

        /**
         * 指标数值
         * 必传
         */
        private String value;

        /**
         * 指标标签
         * 非必传
         */
        private String tags;

    }

    @Data
    public static class BaseResponse<T> implements Serializable {

        public BaseResponse() {
        }

        /**
         * 响应时间
         */
        private Long responseTime = System.currentTimeMillis();

        private String code;

        private String message;

        private T data;

        public boolean isSuccess() {
            return "0".equals(code);
        }

        public BaseResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public BaseResponse(String code, String message, T data) {
            this.code = code;
            this.message = message;
            this.data = data;
        }

        public static BaseResponse success() {
            return new BaseResponse("0", "成功");
        }

        public static <T> BaseResponse success(T data) {
            return new BaseResponse("0", "成功", data);
        }

        public static BaseResponse error(String code, String message) {
            return new BaseResponse(code, message);
        }

        public static BaseResponse error(String message) {
            return new BaseResponse("1", message);
        }

    }
}
