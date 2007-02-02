/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Base class for all API Objects. API Objects are the Java Objects that can be manipulated from
 * Velocity or Groovy in XWiki documents.
 *
 * @version $Id: $
 */
public class Api
{
    /**
     * @see #getContext()
     * @todo make this variable private after we agree on it on the mailing list. It'll break
     *       non-core plugins but better do it now rather than after the 1.0 release...
     */
    protected XWikiContext context;

    /**
     * @param context the XWiki Context object
     * @see #getContext()
     */
    public Api(XWikiContext context)
    {
        this.context = context;
    }

    /**
     * Note: This method is protected so that users of this API do not get to see the XWikiContext
     * object which should not be exposed.
     *
     * @return The XWiki Context object containing all information about the current XWiki instance,
     *         including information on the current request and response.
     */
    protected XWikiContext getContext()
    {
        return this.context;
    }

    /**
     * @return true if the current user has the Programming right or false otherwise
     * @deprecated use #hasProgrammingRights() instead
     */
    public boolean checkProgrammingRights()
    {
        return hasProgrammingRights();
    }

    /**
     * @return true if the current user has the Programming right or false otherwise
     */
    public boolean hasProgrammingRights()
    {
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        return xwiki.getRightService().hasProgrammingRights(context);
    }

    /**
     * @return true if the current user has the Admin right or false otherwise
     */
    public boolean hasAdminRights()
    {
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        return xwiki.getRightService().hasAdminRights(context);
    }

    /**
     * @param right the name of the right to verify (eg "programming", "admin", "register", etc)
     * @param docname the document for which to verify the right
     * @return true if the current user has the specified right
     * @exception XWikiException in case of an error finding the document or accessing groups
     *            information
     */
    public boolean hasAccessLevel(String right, String docname) throws XWikiException
    {
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        return xwiki.getRightService().hasAccessLevel(right, context.getUser(), docname, context);
    }
}
