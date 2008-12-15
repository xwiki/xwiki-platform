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
package com.xpn.xwiki.plugin.webdav.utils;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavSession;
import org.apache.jackrabbit.webdav.DavSessionProvider;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for associating / detaching session information into incoming WebDAV requests.
 * 
 * @version $Id$.
 */
public class XWikiDavSessionProvider implements DavSessionProvider
{
    /**
     * Logger instance.
     */
    private static final Logger logger = LoggerFactory.getLogger(XWikiDavSessionProvider.class);

    /**
     * {@inheritDoc}
     */
    public boolean attachSession(WebdavRequest request) throws DavException
    {
        // Retrieve the workspace name.
        String workspaceName = request.getRequestLocator().getWorkspaceName();
        // Empty workspaceName rather means default (null).
        if (workspaceName != null && "".equals(workspaceName)) {
            workspaceName = null;
        }
        DavSession ds = new XWikiDavSession();
        logger.debug("Attaching session '" + ds + "' to request '" + request + "'");
        request.setDavSession(ds);
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void releaseSession(WebdavRequest request)
    {
        DavSession ds = request.getDavSession();
        if (ds != null && ds instanceof XWikiDavSession) {
            XWikiDavSession session = (XWikiDavSession) ds;
            String[] lockTokens = session.getLockTokens();
            for (String token : lockTokens) {
                session.removeLockToken(token);
            }
            logger.debug("Releasing session '" + ds + "' from request '" + request + "'");
        } else {
            // session is null. nothing to be done.
        }
        request.setDavSession(null);
    }
}
