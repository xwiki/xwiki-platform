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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.plugin.PluginApi;
import org.apache.velocity.VelocityContext;

import java.util.List;

/**
 * Plugin that brings powerful mailing capbilities to XWiki Recipients : to, cc, bcc Text messages
 * HTML messages with attachments Text + HTML messages from XWiki pages templates Send a collection
 * of mails in one call
 *
 * This is the wrapper accessible from in-document scripts.
 *
 * @version $Id: $
 */
public class MailSenderPluginApi extends PluginApi
{
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

    /**
     * Get the MailSenderPlugin
     *
     * @return The MailSenderPlugin
     */
    public MailSenderPlugin getMailSenderPlugin()
    {
        return (MailSenderPlugin) getPlugin();
    }

    /**
     * Sends an HTML mail, with a list of attachments
     *
     * @param to the recipient of the message
     * @param from the sender
     * @param cc carbon copy
     * @param bcc hidden carbon copy
     * @param subject the subject of the message
     * @param body the body content of the mail
     * @param alternative the alternative text offered to the mail client
     * @param attachments List of com.xpn.xwiki.api.Attachment that will be attached to the mail.
     * @return 0 on success, -1 on failure. on failure the error message is stored in XWiki context
     */
    public int sendHtmlMessage(String from, String to, String cc, String bcc, String subject,
        String body, String alternative, List attachments)
    {
        try {
            Mail email = new Mail();
            email.setSubject(subject);
            email.setFrom(from);
            email.setTo(to);
            email.setCc(cc);
            email.setBcc(bcc);
            email.setTextPart(alternative);
            email.setHtmlPart(body);
            email.setAttachments(attachments);
            getMailSenderPlugin().sendMail(email, context);
            return 0;
        } catch (Exception e) {
            context.put("error", e.getMessage());
            getMailSenderPlugin().getLogger().error("sendHtmlMessage", e);
            return -1;
        }
    }

    /**
     * Sends a simple text plain mail
     *
     * @param to the recipient of the message
     * @param from the sender
     * @param subject the subject of the message
     * @param message the body of the message
     * @return 0 on success, -1 on failure. on failure the error message is stored in XWiki context
     */
    public int sendTextMessage(String from, String to, String subject, String message)
    {
        try {
            Mail email = new Mail();
            email.setSubject(subject);
            email.setTextPart(message);
            email.setFrom(from);
            email.setTo(to);

            getMailSenderPlugin().sendMail(email, context);
            return 0;
        } catch (Exception e) {
            context.put("error", e.getMessage());
            getMailSenderPlugin().getLogger().error("sendTextMessage", e);
            return -1;
        }
    }

    /**
     * Sends a simple text plain mail with a list of files attachments
     *
     * @param to the recipient of the message
     * @param from the sender
     * @param cc carbon copy
     * @param bcc hidden carbon copy
     * @param subject the subject of the message
     * @param message the body of the message
     * @param attachments List of com.xpn.xwiki.api.Attachment that will be attached to the mail.
     * @return 0 on success, -1 on failure. on failure the error message is stored in XWiki context
     */
    public int sendTextMessage(String from, String to, String cc, String bcc, String subject,
        String message, List attachments)
    {
        try {
            Mail email = new Mail();
            email.setSubject(subject);
            email.setTextPart(message);
            email.setFrom(from);
            email.setTo(to);
            email.setCc(cc);
            email.setBcc(bcc);
            email.setAttachments(attachments);
            getMailSenderPlugin().sendMail(email, context);
            return 0;
        } catch (Exception e) {
            context.put("error", e.getMessage());
            getMailSenderPlugin().getLogger().error("sendTextMessage", e);
            return -1;
        }
    }

    /**
     * Uses an XWiki document to build the message subject and context, based on variables stored in
     * the VelocityContext. Sends the email.
     *
     * @param from Email sender
     * @param to Email recipient
     * @param cc Email Carbon Copy
     * @param bcc Email Hidden Carbon Copy
     * @param language Language of the email
     * @param documentFullName Full name of the template to be used (example:
     * XWiki.MyEmailTemplate). The template needs to have an XWiki.Email object attached
     * @param vcontext Velocity context passed to the velocity renderer
     * @return True if the email has been sent
     */
    public int sendMessageFromTemplate(String from, String to, String cc, String bcc,
        String language, String documentFullName, VelocityContext vcontext)
    {
        try {
            return getMailSenderPlugin().sendMailFromTemplate(documentFullName, from, to, cc, bcc,
                language, vcontext, context);
        } catch (Exception e) {
            getMailSenderPlugin().getLogger().error("sendMessageFromTemplate", e);
            return -1;
        }
    }
}
