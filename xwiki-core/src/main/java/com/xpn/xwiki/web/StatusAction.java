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
package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import org.apache.struts.action.ActionForward;

public class StatusAction extends XWikiAction
{
    /**
     * We override {@link XWikiAction#execute(XWikiContext) since we don't want any database calls 
     * at all to happen during the execution of this action since we're displaying statuses about 
     * the database.
     */
    public ActionForward execute(XWikiContext context) throws Exception
    {
        // We only get the main wiki since the database statuses are available from it and getting
        // the other wiki will call the database.
        XWiki.getMainXWiki(context);
    
        String page = Utils.getPage(context.getRequest(), "status");
        Utils.parseTemplate(page, !page.equals("direct"), context);

        return null;
	}
}
