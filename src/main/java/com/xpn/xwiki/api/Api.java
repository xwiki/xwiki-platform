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

public class Api
{
    protected XWikiContext context;

    public Api(XWikiContext context)
    {
        this.context = context;
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
