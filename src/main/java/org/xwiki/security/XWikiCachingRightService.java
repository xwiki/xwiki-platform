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
 *
 */
package org.xwiki.security;

import com.xpn.xwiki.user.api.XWikiRightService;

import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import com.xpn.xwiki.web.Utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class for plugging in to xwiki.
 * @version $Id: $
 */
public class XWikiCachingRightService implements XWikiRightService
{
    /** Logger. */
    private static final Log LOG = LogFactory.getLog(RightService.class);

    /** The actual right service compoennt. */
    private final RightService rightService = Utils.getComponent(RightService.class);

    @Override
    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        return rightService.checkAccess(action, doc, context);
    }
 
    @Override
    public boolean hasAccessLevel(String right, String username, String docname, XWikiContext context)
        throws XWikiException
    {
        return rightService.hasAccessLevel(right, username, docname, context);
    }

    @Override
    public boolean hasProgrammingRights(XWikiContext context)
    {
        return rightService.hasProgrammingRights(context);
    }

    @Override
    public boolean hasProgrammingRights(XWikiDocument doc, XWikiContext context)
    {
        return rightService.hasProgrammingRights(doc, context);
    }

    @Override
    public boolean hasAdminRights(XWikiContext context)
    {
        return rightService.hasAdminRights(context);
    }

    @Override
    public List<String> listAllLevels(XWikiContext context)
        throws XWikiException
    {
        return rightService.listAllLevels(context);
    }
}