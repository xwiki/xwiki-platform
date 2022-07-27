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
package com.xpn.xwiki.web;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Action called when the request URL has the "/view/" string in its path (this is configured in
 * <code>struts-config.xml</code>. It means the request is to display a page in view mode.
 *
 * @version $Id$
 */
@Component
@Named("view")
@Singleton
public class ViewAction extends XWikiAction
{
    /**
     * The identifier of the view action.
     */
    public static final String VIEW_ACTION = "view";

    /**
     * Default constructor.
     */
    public ViewAction()
    {
        this.waitForXWikiInitialization = false;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        try {
            handleRevision(context);
        } catch (XWikiException e) {
            if (e.getCode() == XWikiException.ERROR_XWIKI_STORE_HIBERNATE_UNEXISTANT_VERSION) {
                context.put("message", "revisiondoesnotexist");

                return "exception";
            } else {
                throw e;
            }
        }

        XWikiDocument doc = context.getDoc();

        String defaultTemplate = doc.getDefaultTemplate();
        if (StringUtils.isNotEmpty(defaultTemplate)) {
            return defaultTemplate;
        } else {
            return VIEW_ACTION;
        }
    }

    @Override
    protected boolean supportRedirections()
    {
        return true;
    }
}
