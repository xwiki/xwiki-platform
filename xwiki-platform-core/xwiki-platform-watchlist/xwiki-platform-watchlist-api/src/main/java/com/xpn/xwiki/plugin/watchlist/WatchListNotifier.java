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
package com.xpn.xwiki.plugin.watchlist;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.xwiki.script.service.ScriptServiceManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.Object;
import com.xpn.xwiki.plugin.mailsender.MailSenderPlugin;
import com.xpn.xwiki.web.Utils;

/**
 * Utility class used by the watchlist plugin to send notifications to users. The current implementation offers email
 * notifications only.
 * 
 * @version $Id$
 */
public class WatchListNotifier
{
    /**
     * Wiki page which contains the default watchlist email template.
     */
    public static final String DEFAULT_EMAIL_TEMPLATE = "XWiki.WatchListMessage";

    /**
     * XWiki User Class.
     */
    public static final String XWIKI_USER_CLASS = "XWiki.XWikiUsers";

    /**
     * XWiki User Class first name property name.
     */
    public static final String XWIKI_USER_CLASS_FIRST_NAME_PROP = "first_name";

    /**
     * XWiki User Class last name property name.
     */
    public static final String XWIKI_USER_CLASS_LAST_NAME_PROP = "last_name";

    /**
     * XWiki User Class email property.
     */
    private static final String XWIKI_USER_CLASS_EMAIL_PROP = "email";

    /**
     * Sends the email notifying the subscriber that the updatedDocuments have been changed.
     * 
     * @param subscriber user to notify
     * @param events list of events
     * @param emailTemplate email template to use
     * @param previousFireTime last time the notification was fired
     * @param context the XWiki context
     * @throws XWikiException if mail sending fails
     */
    public void sendEmailNotification(String subscriber, List<WatchListEvent> events, String emailTemplate,
        Date previousFireTime, XWikiContext context) throws XWikiException
    {
        // Get user email
        Document subscriberDocument = context.getWiki().getDocument(subscriber, context).newDocument(context);
        Object userObj = subscriberDocument.getObject(XWIKI_USER_CLASS);
        String emailAddr = (String) userObj.getProperty(XWIKI_USER_CLASS_EMAIL_PROP).getValue();
        if (emailAddr == null || emailAddr.length() == 0 || emailAddr.indexOf("@") < 0) {
            // Invalid email
            return;
        }
        
        List<String> modifiedDocuments = new ArrayList<String>();
        for (WatchListEvent event : events) {
            if (!modifiedDocuments.contains(event.getPrefixedFullName())) {
                modifiedDocuments.add(event.getPrefixedFullName());                
            }
        }

        // Prepare email template (wiki page) context
        VelocityContext vcontext = new VelocityContext();
        vcontext
            .put(XWIKI_USER_CLASS_FIRST_NAME_PROP, userObj.getProperty(XWIKI_USER_CLASS_FIRST_NAME_PROP).getValue());
        vcontext.put(XWIKI_USER_CLASS_LAST_NAME_PROP, userObj.getProperty(XWIKI_USER_CLASS_LAST_NAME_PROP).getValue());
        vcontext.put("events", events);
        vcontext.put("xwiki", new com.xpn.xwiki.api.XWiki(context.getWiki(), context));
        vcontext.put("util", new com.xpn.xwiki.api.Util(context.getWiki(), context));
        vcontext.put("msg", context.getMessageTool());
        vcontext.put("modifiedDocuments", modifiedDocuments);
        vcontext.put("previousFireTime", previousFireTime);        
        vcontext.put("context", new Context(context));
        vcontext.put("services", Utils.getComponent(ScriptServiceManager.class));

        // Get wiki's default language (default en)
        String language = context.getWiki().getXWikiPreference("default_language", "en", context);

        // Get mailsenderplugin
        MailSenderPlugin emailService = (MailSenderPlugin) context.getWiki().getPlugin(MailSenderPlugin.ID, context);
        if (emailService == null) {
            return;
        }

        // Get wiki administrator email (default : mailer@xwiki.localdomain.com)
        String sender = context.getWiki().getXWikiPreference("admin_email", "mailer@xwiki.localdomain.com", context);

        // Set email template
        String template = "";
        if (context.getWiki().exists(emailTemplate, context)) {
            template = emailTemplate;
        } else if (context.getWiki().exists(DEFAULT_EMAIL_TEMPLATE, context)) {
            template = DEFAULT_EMAIL_TEMPLATE;
        } else {
            template = context.getMainXWiki() + ":" + DEFAULT_EMAIL_TEMPLATE;
        }

        // Send message from template
        emailService.sendMailFromTemplate(template, sender, emailAddr, null, null, language, vcontext, context);
    }
}
