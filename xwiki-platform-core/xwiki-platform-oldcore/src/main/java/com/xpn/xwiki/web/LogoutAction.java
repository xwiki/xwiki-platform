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

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Action for processing logout requests. The actual logout request processing is done before this action is invoked,
 * the URL will trigger the authenticator automatically. This action just cleans up the session and redirects to a view
 * page.
 * 
 * @version $Id$
 */
public class LogoutAction extends XWikiAction
{
    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();

        // Destroy the current session, if any, so that any private data stored in the session won't be accessible by
        // the next user on the same computer
        HttpSession currentSession = request.getSession(false);
        if (currentSession != null) {
            synchronized (currentSession) {
                currentSession.invalidate();
                // Early registration of a new session, so that the client gets to know the new session identifier early
                // A new session is going to be needed after the redirect anyway
                request.getSession(true);
            }
        }

        // Process redirect
        String redirect;
        redirect = context.getRequest().getParameter("xredirect");
        if (StringUtils.isEmpty(redirect)) {
            ModelConfiguration modelDefaults = Utils.getComponent(ModelConfiguration.class);
            DocumentReference doc = new DocumentReference(
                context.getDatabase(),
                modelDefaults.getDefaultReferenceValue(EntityType.SPACE),
                modelDefaults.getDefaultReferenceValue(EntityType.DOCUMENT));
            redirect = context.getWiki().getURL(doc, "view", context);
        }
        sendRedirect(response, redirect);
        return false;
    }
}
