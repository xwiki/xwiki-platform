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
package com.xpn.xwiki.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

import com.xpn.xwiki.XWikiContext;

/**
 * Provide current {@link XWikiContext} or null if none is set. Should be used in cases creating a new
 * {@link XWikiContext} would not help anyway (usually when checking something that might be null even in a valid
 * {@link XWikiContext} like the current user).
 *
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Singleton
@Named("readonly")
public class ReadOnlyXWikiContextProvider implements Provider<XWikiContext>
{
    /**
     * Used to access current {@link XWikiContext}.
     */
    @Inject
    private Execution execution;

    @Override
    public XWikiContext get()
    {
        return getXWikiContext();
    }

    /**
     * @return current XWikiContext or new one
     */
    private XWikiContext getXWikiContext()
    {
        ExecutionContext econtext = this.execution.getContext();

        if (econtext != null) {
            return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
        }

        return null;
    }
}
