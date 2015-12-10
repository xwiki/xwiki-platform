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
package org.xwiki.webjars.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Sets the current wiki in the XWiki Context.
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component
@Singleton
public class DefaultContextInitializer implements ContextInitializer
{
    @Inject
    private Execution execution;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public void initialize(String wikiId) throws ResourceReferenceHandlerException
    {
        // Note: the Execution Context and XWikiContext have already been initialized in the
        // Resource Reference Handler Servlet. We need to set the current wiki though.
        ExecutionContext context = this.execution.getContext();
        try {
            XWikiContext xwikiContext = (XWikiContext) context.getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);

            // Set the current wiki
            String normalizedWikiId = wikiId;
            if (StringUtils.isEmpty(wikiId)) {
                // Find the main wiki's name
                normalizedWikiId = this.wikiDescriptorManager.getMainWikiId();
            }
            xwikiContext.setWikiId(normalizedWikiId);
        } catch (Exception e) {
            throw new ResourceReferenceHandlerException("Failed to initialize WebJars Handler's Execution Context",
                e);
        }
    }
}
