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
package org.xwiki.url.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.url.URLContextManager;

/**
 * Gets the current URL scheme to use from the Execution Context.
 *
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Singleton
public class DefaultURLContextManager implements URLContextManager
{
    static final String CONTEXT_KEY = "urlscheme";

    @Inject
    private Execution execution;

    @Override
    public String getURLFormatId()
    {
        // Note: There should always be a context ready
        ExecutionContext ec = this.execution.getContext();
        return (String) ec.getProperty(CONTEXT_KEY);
    }

    @Override
    public void setURLFormatId(String urlFormatId)
    {
        ExecutionContext ec = this.execution.getContext();
        ec.setProperty(CONTEXT_KEY, urlFormatId);
    }
}
