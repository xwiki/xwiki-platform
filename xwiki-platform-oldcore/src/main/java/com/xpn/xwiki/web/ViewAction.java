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

import java.io.IOException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Action called when the request URL has the "/view/" string in its path (this is configured in
 * <code>struts-config.xml</code>. It means the request is to display a page in view mode.
 * 
 * @version $Id$
 */
public class ViewAction extends XWikiAction
{
    /**
     * The identifier of the view action.
     * 
     * @todo need an enumerated class for actions.
     */
    public static final String VIEW_ACTION = "view";

    /**
     * {@inheritDoc}
     * 
     * @see XWikiAction#action(com.xpn.xwiki.XWikiContext)
     */
    public boolean action(XWikiContext context) throws XWikiException
    {
        boolean shouldRender = true;

        context.put("action", VIEW_ACTION);

        // Redirect to the ViewrevAction is the URL has a rev parameter (when the user asks to
        // view a specific revision of a document).
        XWikiRequest request = context.getRequest();
        String rev = request.getParameter("rev");
        if (rev != null) {
            String url = context.getDoc().getURL("viewrev", request.getQueryString(), context);
            try {
                context.getResponse().sendRedirect(url);
            } catch (IOException e) {
                e.printStackTrace();
            }
            shouldRender = false;
        }

        return shouldRender;
    }

    /**
     * {@inheritDoc}
     * 
     * @see XWikiAction#render(com.xpn.xwiki.XWikiContext)
     */
    public String render(XWikiContext context) throws XWikiException
    {
        handleRevision(context);
        XWikiDocument doc = (XWikiDocument) context.get("doc");

        String defaultTemplate = doc.getDefaultTemplate();
        if ((defaultTemplate != null) && (!defaultTemplate.equals(""))) {
            return defaultTemplate;
        } else {
            return VIEW_ACTION;
        }
    }
}
