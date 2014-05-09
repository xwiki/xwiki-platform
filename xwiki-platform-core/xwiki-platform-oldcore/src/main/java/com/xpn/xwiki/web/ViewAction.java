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

import org.apache.struts.action.ActionForward;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.template.WikiTemplateRenderer;

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

    @Override
    public ActionForward execute(XWikiContext xcontext) throws Exception
    {
        // TODO: improve that to not even block the first request
        if (isInitializing(xcontext)) {
            // Display initialization template
            renderInit(xcontext);

            // Initialization template was displayed, stop here.
            return null;
        } else {
            return super.execute(xcontext);
        }
    }

    private void renderInit(XWikiContext xcontext) throws IOException, ComponentLookupException
    {
        String content =
            Utils.getComponent(WikiTemplateRenderer.class).render("/templates/init.wiki", "init", Syntax.XHTML_1_0);

        xcontext.getResponse().setStatus(503);
        xcontext.getResponse().setContentType("text/html; charset=UTF-8");
        xcontext.getResponse().setContentLength(content.length());
        xcontext.getResponse().getWriter().write(content);
        xcontext.getResponse().flushBuffer();

        xcontext.setFinished(true);
    }

    @Override
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

    @Override
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
