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
package com.xpn.xwiki.notify;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Deprecated
public class XWikiPageNotification implements XWikiActionNotificationInterface
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiPageNotification.class);

    @Override
    public void notify(XWikiNotificationRule rule, XWikiDocument doc, String action, XWikiContext context)
    {
        try {
            String notifpages = context.getWiki().getXWikiPreference("notification_pages", context);
            if ((notifpages != null) && (!notifpages.equals(""))) {
                String[] notifpages2 = StringUtils.split(notifpages, " ,");
                for (int i = 0; i < notifpages2.length; i++) {
                    notifyPage(notifpages2[i], rule, doc, action, context);
                }
            }
            String xnotif = (context.getRequest() != null) ? context.getRequest().getParameter("xnotification") : null;
            if ((xnotif != null) && (!xnotif.equals(""))) {
                notifyPage(xnotif, rule, doc, action, context);
            }
        } catch (Throwable e) {
            XWikiException e2 =
                new XWikiException(XWikiException.MODULE_XWIKI_NOTIFICATION, XWikiException.ERROR_XWIKI_NOTIFICATION,
                    "Error executing notifications", e);
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(e2.getFullMessage());
            }
        }
    }

    protected void notifyPage(String page, XWikiNotificationRule rule, XWikiDocument doc, String action,
        XWikiContext context)
    {
        XWikiActionNotificationInterface notif = null;
        try {
            XWiki xwiki = context.getWiki();
            XWikiDocument pagedoc = xwiki.getDocument(page, context);
            if (xwiki.getRightService().hasProgrammingRights(pagedoc, context)) {
                notif = (XWikiActionNotificationInterface) xwiki.parseGroovyFromString(pagedoc.getContent(), context);
                notif.notify(rule, doc, action, context);
            }
        } catch (Throwable e) {
            Object[] args = {page};
            XWikiException e2 =
                new XWikiException(XWikiException.MODULE_XWIKI_GROOVY,
                    XWikiException.ERROR_XWIKI_GROOVY_EXECUTION_FAILED,
                    "Error parsing groovy notification for page {0}", e, args);
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error(e2.getFullMessage());
            }
        }
    }
}
