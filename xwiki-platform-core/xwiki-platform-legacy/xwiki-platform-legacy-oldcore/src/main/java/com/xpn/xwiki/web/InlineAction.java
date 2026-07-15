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
 * @deprecated use {@link EditAction} with {@code editor=inline} in the query string instead since 3.2
 *
 * @version $Id$
 */
@Component
@Named("inline")
@Singleton
@Deprecated
public class InlineAction extends EditAction
{
    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        // Delegate to the edit action so we profit from every improvement it gets.
        String result = super.render(context);

        // Redirect to the inline template if the edit action was successful.
        return "edit".equals(result) ? "inline" : result;
    }
}
