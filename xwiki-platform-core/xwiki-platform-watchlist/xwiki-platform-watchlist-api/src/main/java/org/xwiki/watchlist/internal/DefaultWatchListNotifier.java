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
package org.xwiki.watchlist.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.watchlist.internal.api.WatchListEvent;
import org.xwiki.watchlist.internal.api.WatchListNotifier;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.api.DeprecatedContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.mailsender.MailSenderPlugin;

/**
 * Default implementation for {@link WatchListNotifier}. The current implementation offers email notifications only.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultWatchListNotifier implements WatchListNotifier
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
    public static final String XWIKI_USER_CLASS_EMAIL_PROP = "email";

    /**
     * Context provider.
     */
    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Email service configuration.
     */
    @Inject
    private MailSenderConfiguration mailConfiguration;

    @Inject
    private ScriptServiceManager scriptServiceManager;

    @Override
    public void sendNotification(String subscriber, List<WatchListEvent> events, String templateDocument,
        Date previousFireTime) throws XWikiException
    {
        XWikiContext context = contextProvider.get();

        // Get user email
        XWikiDocument subscriberDocument = context.getWiki().getDocument(subscriber, context);
        BaseObject userObj = subscriberDocument.getObject(XWIKI_USER_CLASS);
        String emailAddr = userObj.getStringValue(XWIKI_USER_CLASS_EMAIL_PROP);
        if (emailAddr == null || emailAddr.length() == 0 || emailAddr.indexOf("@") < 0) {
            // Invalid email
            return;
        }

        List<String> modifiedDocuments = new ArrayList<>();
        for (WatchListEvent event : events) {
            if (!modifiedDocuments.contains(event.getPrefixedFullName())) {
                modifiedDocuments.add(event.getPrefixedFullName());
            }
        }

        // Prepare email template (wiki page) context
        VelocityContext vcontext = new VelocityContext();
        vcontext.put(XWIKI_USER_CLASS_FIRST_NAME_PROP, userObj.getStringValue(XWIKI_USER_CLASS_FIRST_NAME_PROP));
        vcontext.put(XWIKI_USER_CLASS_LAST_NAME_PROP, userObj.getStringValue(XWIKI_USER_CLASS_LAST_NAME_PROP));
        vcontext.put("events", events);
        vcontext.put("xwiki", new com.xpn.xwiki.api.XWiki(context.getWiki(), context));
        vcontext.put("util", new com.xpn.xwiki.api.Util(context.getWiki(), context));
        vcontext.put("msg", context.getMessageTool());
        vcontext.put("modifiedDocuments", modifiedDocuments);
        vcontext.put("previousFireTime", previousFireTime);
        vcontext.put("context", new DeprecatedContext(context));
        vcontext.put("xcontext", new Context(context));
        vcontext.put("services", scriptServiceManager);

        // Get wiki's default language (default en)
        String language = context.getWiki().getXWikiPreference("default_language", "en", context);

        // Get mailsenderplugin
        // FIXME: Use the new mail module instead.
        MailSenderPlugin emailService = (MailSenderPlugin) context.getWiki().getPlugin(MailSenderPlugin.ID, context);
        if (emailService == null) {
            return;
        }

        // Get from email address from the configuration (default : mailer@xwiki.localdomain.com)
        String from = getFromAddress();

        // Set email template
        String template = getTemplateDocument(templateDocument, context);

        // Send message from template
        emailService.sendMailFromTemplate(template, from, emailAddr, null, null, language, vcontext, context);
    }

    private String getFromAddress()
    {
        // Get from email address from the configuration (default : mailer@xwiki.localdomain.com)
        String from = mailConfiguration.getFromAddress();
        if (from == null) {
            from = "mailer@xwiki.localdomain.com";
        }
        return from;
    }

    /**
     * @param templateDocument
     * @param context
     * @return
     */
    private String getTemplateDocument(String templateDocument, XWikiContext context)
    {
        String template = "";
        if (context.getWiki().exists(templateDocument, context)) {
            template = templateDocument;
        } else if (context.getWiki().exists(DEFAULT_EMAIL_TEMPLATE, context)) {
            template = DEFAULT_EMAIL_TEMPLATE;
        } else {
            template = context.getMainXWiki() + ":" + DEFAULT_EMAIL_TEMPLATE;
        }
        return template;
    }
}
