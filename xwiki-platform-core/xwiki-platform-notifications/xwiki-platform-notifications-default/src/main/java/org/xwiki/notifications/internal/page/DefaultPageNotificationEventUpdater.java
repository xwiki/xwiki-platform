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
package org.xwiki.notifications.internal.page;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.internal.ModelBridge;
import org.xwiki.notifications.page.PageNotificationEventDescriptor;
import org.xwiki.notifications.page.PageNotificationEventUpdater;
import org.xwiki.query.Query;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * This is the default implementation of {@link PageNotificationEventUpdater}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Singleton
public class DefaultPageNotificationEventUpdater implements PageNotificationEventUpdater
{
    @Inject
    private PageNotificationEventDescriptorManager pageNotificationEventListenerContainer;

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private QueryManager queryManager;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Logger logger;

    /**
     * Fetch every registered PageNotificationEventDescriptorClass XObjects in the wiki, then update the
     * PageNotificationEventListener.
     */
    public void updateDescriptors()
    {
        List<PageNotificationEventDescriptor> descriptorList = new ArrayList<>();

        // Fetch every PageNotificationEventDescriptors in the farm
        try {
            final List<String> descriptors = (List<String>) (List) this.queryManager.createQuery(
                    "from doc.object(XWiki.Notifications.Code.PageNotificationEventDescriptorClass)"
                    + " as document",
                    Query.XWQL)
                    .addFilter(componentManager.<QueryFilter>getInstance(QueryFilter.class, "unique"))
                    .execute();

            for (String descriptor: descriptors) {
                DocumentReference document = documentReferenceResolver.resolve(descriptor);
                descriptorList.add(modelBridge.getPageNotificationEventDescriptor(document));
            }
            this.pageNotificationEventListenerContainer.updateDescriptorList(descriptorList);
        } catch (Exception e) {
            logger.warn(String.format(
                    "Unable to update the list of in-page notifications. Exception : %s\nStacktrace : %s",
                    e.getMessage(),
                    e.getStackTrace()));
        }
    }
}
