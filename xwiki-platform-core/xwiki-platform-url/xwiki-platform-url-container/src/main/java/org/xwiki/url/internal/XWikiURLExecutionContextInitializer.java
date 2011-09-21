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
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;

/**
 * Sets the XWiki URL in the Execution Context (if found).
 *
 * @version $Id$
 * @since 3.0M3
 */
@Component
@Named("xwikiurl")
@Singleton
public class XWikiURLExecutionContextInitializer implements ExecutionContextInitializer
{
    /**
     * Used to get access to the Request.
     */
    @Inject
    private Container container;

    @Override
    public void initialize(ExecutionContext context) throws ExecutionContextException
    {
        // If there's no Request don't set the XWiki URL in the Execution Context
        Request request = this.container.getRequest();
        if (request != null) {
            Object xwikiURL = request.getProperty(Request.XWIKI_URL);
            if (xwikiURL != null) {
                context.setProperty(Request.XWIKI_URL, xwikiURL);
            }
        }
    }
}
