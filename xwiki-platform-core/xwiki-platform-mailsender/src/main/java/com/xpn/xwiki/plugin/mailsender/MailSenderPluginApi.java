/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.plugin.mailsender;

import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.XWiki;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * Plugin that brings powerful mailing capabilities. This is the wrapper accessible from in-document scripts.
 * 
 * @see MailSender
 * @version $Id$
 */
public class MailSenderPluginApi extends PluginApi<MailSenderPlugin> implements MailSender
{
    /**
     * Log object to log messages in this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MailSenderPluginApi.class);

    /**
     * API constructor.
     * 
     * @param plugin The wrapped plugin object.
     * @param context Context of the request.
     * @see PluginApi#PluginApi(com.xpn.xwiki.plugin.XWikiPluginInterface,XWikiContext)
     */
    public MailSenderPluginApi(MailSenderPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    @Override
    public int sendHtmlMessage(String from, String to, String cc, String bcc, String subject, String body,
        String alternative, List<Attachment> attachments)
    {
        Mail email = new Mail();
        email.setSubject(subject);
        email.setFrom(from);
        email.setTo(to);
        email.setCc(cc);
        email.setBcc(bcc);
        email.setTextPart(alternative);
        email.setHtmlPart(body);
        email.setAttachments(attachments);
        return sendMail(email);
    }

    @Override
    public int sendTextMessage(String from, String to, String subject, String message)
    {
        Mail email = new Mail();
        email.setSubject(subject);
        email.setTextPart(message);
        email.setFrom(from);
        email.setTo(to);
        return sendMail(email);
    }

    @Override
    public int sendTextMessage(String from, String to, String cc, String bcc, String subject, String message,
        List<Attachment> attachments)
    {
        Mail email = new Mail();
        email.setSubject(subject);
        email.setTextPart(message);
        email.setFrom(from);
        email.setTo(to);
        email.setCc(cc);
        email.setBcc(bcc);
        email.setAttachments(attachments);
        return sendMail(email);
    }

    @Override
    public int sendRawMessage(String from, String to, String rawMessage)
    {
        Mail email = new Mail();
        email.setFrom(from);
        email.setTo(to);

        getProtectedPlugin().parseRawMessage(rawMessage, email);
        return sendMail(email);
    }

    @Override
    public int sendMessageFromTemplate(String from, String to, String cc, String bcc, String language,
        String documentFullName, VelocityContext vcontext)
    {
        try {
            return getProtectedPlugin().sendMailFromTemplate(documentFullName, from, to, cc, bcc, language, vcontext,
                this.context);
        } catch (Exception e) {
            // If the exception is a null pointer exception there is no message and e.getMessage() is null.
            if (e.getMessage() != null) {
                this.context.put("error", e.getMessage());
            }
            LOGGER.error("sendMessageFromTemplate", e);
            return -1;
        }
    }

    /**
     * Uses an XWiki document to build the message subject and context, based on variables stored in a map. Sends the
     * email.
     * 
     * @param from Email sender
     * @param to Email recipient
     * @param cc Email Carbon Copy
     * @param bcc Email Hidden Carbon Copy
     * @param language Language of the email
     * @param documentFullName Full name of the template to be used (example: XWiki.MyEmailTemplate). The template needs
     *        to have an XWiki.Email object attached
     * @param parameters variables to be passed to the velocity context
     * @return 0 on success, -1 on failure. On failure the error message is stored in the XWiki context under the
     *         "error" key.
     */
    public int sendMessageFromTemplate(String from, String to, String cc, String bcc, String language,
        String documentFullName, Map<String, Object> parameters)
    {
        try {
            return getProtectedPlugin().sendMailFromTemplate(documentFullName, from, to, cc, bcc, language, parameters,
                this.context);
        } catch (Exception e) {
            // If the exception is a null pointer exception there is no message and e.getMessage() is null.
            if (e.getMessage() != null) {
                this.context.put("error", e.getMessage());
            }
            LOGGER.error("sendMessageFromTemplate", e);
            return -1;
        }
    }

    @Override
    public Mail createMail()
    {
        return new Mail();
    }

    @Override
    public int sendMail(Mail mail)
    {
        int result = 0;
        try {
            getProtectedPlugin().sendMail(mail, this.context);
        } catch (Exception e) {
            // If the exception is a null pointer exception there is no message and e.getMessage() is null.
            if (e.getMessage() != null) {
                this.context.put("error", e.getMessage());
            }
            LOGGER.error("Failed to send email [" + mail.toString() + "]", e);
            result = -1;
        }

        return result;
    }

    @Override
    public MailConfiguration createMailConfiguration(XWiki xwiki)
    {
        return new MailConfiguration(xwiki);
    }

    @Override
    public int sendMail(Mail mail, MailConfiguration mailConfiguration)
    {
        int result = 0;
        try {
            getProtectedPlugin().sendMail(mail, mailConfiguration, this.context);
        } catch (Exception e) {
            // If the exception is a null pointer exception there is no message and e.getMessage() is null.
            if (e.getMessage() != null) {
                this.context.put("error", e.getMessage());
            }
            LOGGER.error("Failed to send email [" + mail.toString() + "] using mail configuration ["
                + mailConfiguration.toString() + "]", e);
            result = -1;
        }

        return result;
    }
}
