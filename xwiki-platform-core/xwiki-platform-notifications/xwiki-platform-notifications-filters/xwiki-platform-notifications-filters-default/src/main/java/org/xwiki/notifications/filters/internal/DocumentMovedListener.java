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
package org.xwiki.notifications.filters.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.namespace.NamespaceContextExecutor;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;

/**
 * Update the notification filter preferences when a document is renamed/moved.
 *
 * @since 11.3RC1
 * @version $Id$
 */
@Component
@Named(DocumentMovedListener.NAME)
@Singleton
public class DocumentMovedListener extends AbstractEventListener
{
    /**
     * Name of the component.
     */
    public static final String NAME = "NotificationsFiltersPreferences-DocumentMovedListener";

    private static final String NEW_PAGE = "newPage";

    private static final String OLD_PAGE = "oldPage";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private NotificationFilterPreferenceConfiguration filterPreferenceConfiguration;

    @Inject
    private Logger logger;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private NamespaceContextExecutor namespaceContextExecutor;

    @Inject
    @Named("cached")
    private ModelBridge cachedModelBridge;

    /**
     * Guess what it does.
     */
    public DocumentMovedListener()
    {
        super(NAME, new DocumentRenamedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentRenamedEvent renamedEvent = (DocumentRenamedEvent) event;
        DocumentReference sourceLocation = renamedEvent.getSourceReference();
        DocumentReference targetLocation = renamedEvent.getTargetReference();

        try {
            boolean isExecutedOnMainStore = false;
            if (filterPreferenceConfiguration.useLocalStore()) {
                updatePreferences(sourceLocation, targetLocation);
                isExecutedOnMainStore = wikiDescriptorManager.isMainWiki(wikiDescriptorManager.getCurrentWikiId());
            }
            if (filterPreferenceConfiguration.useMainStore() && !isExecutedOnMainStore) {
                namespaceContextExecutor.execute(new WikiNamespace(wikiDescriptorManager.getMainWikiId()), () -> {
                    updatePreferences(sourceLocation, targetLocation);
                    return null;
                });
            }

        } catch (Exception e) {
            logger.error("Failed to update the notification filter preference when [{}] has been moved to [{}].",
                renamedEvent.getSourceReference(), renamedEvent.getTargetReference(), e);
        } finally {
            ((CachedModelBridge) cachedModelBridge).clearCache();
        }
    }

    private void updatePreferences(DocumentReference sourceLocation, DocumentReference targetLocation)
        throws XWikiException
    {
        XWikiContext context = contextProvider.get();
        XWikiHibernateStore hibernateStore = context.getWiki().getHibernateStore();
        hibernateStore.beginTransaction(context);
        Session session = hibernateStore.getSession(context);

        if ("WebHome".equals(sourceLocation.getName())) {
            session
                .createQuery(
                    "update DefaultNotificationFilterPreference p set p.page = :newPage " + "where p.page = :oldPage")
                .setString(NEW_PAGE, serializer.serialize(targetLocation.getLastSpaceReference()))
                .setString(OLD_PAGE, serializer.serialize(sourceLocation.getLastSpaceReference())).executeUpdate();
        }
        session
            .createQuery("update DefaultNotificationFilterPreference p set p.pageOnly = :newPage "
                + "where p.pageOnly = :oldPage")
            .setString(NEW_PAGE, serializer.serialize(targetLocation))
            .setString(OLD_PAGE, serializer.serialize(sourceLocation)).executeUpdate();

        hibernateStore.endTransaction(context, true);
    }
}
