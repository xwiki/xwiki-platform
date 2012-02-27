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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.util.XWikiStubContextProvider;

/**
 * An automatic XWikiContext stub injecter for ExecutionContext for daemons unable to create a proper XWikiContext (no
 * real request information or not even know about XWikiContext like components).
 * 
 * @see XWikiStubContextProvider
 * @version $Id$
 * @since 2.0M3
 */
@Component
@Singleton
@Named("XWikiStubContextInitializer")
public class XWikiStubContextInitializer implements ExecutionContextInitializer
{
    /**
     * Generate stub XWikiContext.
     */
    @Inject
    private XWikiStubContextProvider stubContextProvider;

    @Override
    public void initialize(ExecutionContext context) throws ExecutionContextException
    {
        XWikiContext xcontext = (XWikiContext) context.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);

        if (xcontext == null) {
            // if the XWikiContext is not provided in the Execution context it mean the Execution context is being
            // initialized by a daemon thread
            XWikiContext stubContext = this.stubContextProvider.createStubContext();

            if (stubContext != null) {
                // the stub context has been properly initialized, we inject it in the Execution context
                context.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, stubContext);
            }
        }
    }
}
