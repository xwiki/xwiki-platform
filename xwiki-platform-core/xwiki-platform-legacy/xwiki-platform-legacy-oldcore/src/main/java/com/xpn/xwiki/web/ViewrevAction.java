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
 * @version $Id$
 * @deprecated since 13.0, it's redundant with {@link ViewAction} which should be preferred
 */
@Component
@Named("viewrev")
@Singleton
public class ViewrevAction extends XWikiAction
{
    /**
     * Default constructor.
     */
    public ViewrevAction()
    {
        this.waitForXWikiInitialization = false;
    }

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        try {
            handleRevision(context);
        } catch (XWikiException e) {
            if (e.getCode() == XWikiException.ERROR_XWIKI_STORE_HIBERNATE_UNEXISTANT_VERSION) {
                context.put("message", "revisiondoesnotexist");
                return "exception";

            } else {
                throw e;
            }
        }

        return "view";
    }

    @Override
    protected boolean supportRedirections()
    {
        return true;
    }
}
