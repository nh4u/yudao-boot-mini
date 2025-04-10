package cn.bitlinks.ems.module.system.service.mail;

import cn.bitlinks.ems.module.system.mq.message.mail.MailSendMessage;

import java.util.Map;

/**
 * 邮件发送 Service 接口
 *
 * @author wangjingyi
 * @since 2022-03-21
 */
public interface MailSendService {
    /**
     * 发送单条邮件给管理后台的用户
     *
     * @param mail 邮箱
     * @param userId 用户编码
     * @param title 邮件标题
     * @param content 邮件内容
     * @param templateId 模板id
     * @param templateCode 邮件模版编码
     * @param templateName 邮件模版名称
     * @return 发送日志编号
     */
    Long sendSingleMailToAdminCustom(String mail, Long userId,
                                     String title, String content,
                                     Long templateId, String templateCode, String templateName);
    /**
     * 发送单条邮件给管理后台的用户
     *
     * @param mail 邮箱
     * @param userId 用户编码
     * @param templateCode 邮件模版编码
     * @param templateParams 邮件模版参数
     * @return 发送日志编号
     */
    Long sendSingleMailToAdmin(String mail, Long userId,
                               String templateCode, Map<String, Object> templateParams);

    /**
     * 发送单条邮件给用户 APP 的用户
     *
     * @param mail 邮箱
     * @param userId 用户编码
     * @param templateCode 邮件模版编码
     * @param templateParams 邮件模版参数
     * @return 发送日志编号
     */
    Long sendSingleMailToMember(String mail, Long userId,
                                String templateCode, Map<String, Object> templateParams);

    /**
     * 发送单条邮件给用户(自定义告警模板)
     *
     * @param mail 邮箱
     * @param userId 用户编码
     * @param userType 用户类型
     * @param title 邮件标题
     * @param content 邮件内容
     * @param templateId 模板id
     * @param templateCode 邮件模版编码
     * @param templateName 模板名称
     * @return 发送日志编号
     */
    Long sendSingleMailCustom(String mail, Long userId, Integer userType, String title,String content,
                              Long templateId,String templateCode,String templateName);

    /**
     * 发送单条邮件给用户
     *
     * @param mail 邮箱
     * @param userId 用户编码
     * @param userType 用户类型
     * @param templateCode 邮件模版编码
     * @param templateParams 邮件模版参数
     * @return 发送日志编号
     */
    Long sendSingleMail(String mail, Long userId, Integer userType,
                        String templateCode, Map<String, Object> templateParams);

    /**
     * 执行真正的邮件发送
     * 注意，该方法仅仅提供给 MQ Consumer 使用
     *
     * @param message 邮件
     */
    void doSendMail(MailSendMessage message);

}
