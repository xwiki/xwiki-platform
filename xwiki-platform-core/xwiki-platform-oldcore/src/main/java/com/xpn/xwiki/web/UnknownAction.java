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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Action used when the url given to struts is not recognised as a valid action. Forwards to the action specified in
 * xwiki.cfg parameter: xwiki.unknownActionResponse or exception if xwiki.unknownActionResponse is not defined.
 *
 * @version $Id$
 */
public class UnknownAction extends XWikiAction
{
    @Override
    protected String getName()
    {
        return "unknown";
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        String defaultAction = context.getWiki().Param("xwiki.unknownActionResponse", "exception");
        // Set the action in the context because code which uses $xcontext.getAction()
        // should get the desired action instead of "unknown"
        context.setAction(defaultAction);
        return defaultAction;
    }
}
