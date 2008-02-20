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
import com.xpn.xwiki.plugin.PluginApi;
import org.apache.velocity.VelocityContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Plugin that brings powerful mailing capabilities.
 *
 * This is the wrapper accessible from in-document scripts.
 *
 * @see MailSender
 * @version $Id: $
 */
public class MailSenderPluginApi extends PluginApi implements MailSender
{
    /**
     * Log object to log messages in this class.
     */
    private static final Log LOG = LogFactory.getLog(MailSenderPluginApi.class);

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
        if (hasProgrammingRights()) {
            return (MailSenderPlugin) getPlugin();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * @see MailSender#sendHtmlMessage(String, String, String, String, String, String, String, java.util.List) 
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
            LOG.error("sendHtmlMessage", e);
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     * @see MailSender#sendTextMessage(String, String, String, String)  
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
            LOG.error("sendTextMessage", e);
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     * @see MailSender#sendTextMessage(String, String, String, String, String, String, java.util.List)   
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
            LOG.error("sendTextMessage", e);
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     * @see MailSender#sendMessageFromTemplate(String, String, String, String, String, String, VelocityContext)   
     */
    public int sendMessageFromTemplate(String from, String to, String cc, String bcc,
        String language, String documentFullName, VelocityContext vcontext)
    {
        try {
            return getMailSenderPlugin().sendMailFromTemplate(documentFullName, from, to, cc, bcc,
                language, vcontext, context);
        } catch (Exception e) {
            LOG.error("sendMessageFromTemplate", e);
            return -1;
        }
    }
}
