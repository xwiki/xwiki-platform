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
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextInitializer;
import org.xwiki.url.URLConfiguration;

/**
 * Sets the URL Scheme to use in the Execution Context by getting it from XWiki's configuration.
 *
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named("urlscheme")
@Singleton
public class URLExecutionContextInitializer implements ExecutionContextInitializer
{
    /**
     * Used to get the URL Scheme to use from the configuration.
     */
    @Inject
    private URLConfiguration configuration;

    @Override
    public void initialize(ExecutionContext context) throws ExecutionContextException
    {
        if (!context.hasProperty(DefaultURLContextManager.CONTEXT_KEY)) {
            context.newProperty(DefaultURLContextManager.CONTEXT_KEY)
                .inherited()
                .initial(this.configuration.getURLFormatId())
                .declare();
        }
    }
}
