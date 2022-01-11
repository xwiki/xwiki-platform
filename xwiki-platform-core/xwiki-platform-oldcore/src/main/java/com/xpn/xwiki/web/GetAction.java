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

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Action used to get a resource that is a part of a page loaded asynchronously generally for performance reasons. Like
 * <code>download</code> or <code>skin</code> but for wiki content.
 * <p>
 * This means that by default there is not UI and it's not registered in the statistics since the main page is already
 * logged.
 *
 * @version $Id$
 */
@Component
@Named("get")
@Singleton
public class GetAction extends XWikiAction
{
    /**
     * The identifier of the view action.
     */
    public static final String GET_ACTION = "get";

    @Override
    public boolean action(XWikiContext context) throws XWikiException
    {
        boolean shouldRender = true;

        context.put("action", GET_ACTION);

        return shouldRender;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        handleRevision(context);

        // In case a search engine spider should end up calling the /get/ action, point them to the view action with
        // a Content-Location header. http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.14
        context.getResponse().setHeader("Content-Location", context.getDoc().getURL("view", context));

        return GET_ACTION;
    }

    @Override
    protected boolean supportRedirections()
    {
        return true;
    }
}
