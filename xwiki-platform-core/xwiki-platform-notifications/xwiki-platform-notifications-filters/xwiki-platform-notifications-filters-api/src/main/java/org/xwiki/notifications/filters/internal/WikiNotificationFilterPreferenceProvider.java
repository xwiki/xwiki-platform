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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceProvider;

/**
 * This is an of the role {@link NotificationFilterPreferenceProvider}.
 * It allows retrieving filter preferences from the administration XObjects.
 *
 * @version $Id$
 * @since 10.3RC1
 * @since 9.11.5
 */
@Component
@Named(WikiNotificationFilterPreferenceProvider.HINT)
@Singleton
public class WikiNotificationFilterPreferenceProvider implements NotificationFilterPreferenceProvider
{
    /**
     * Hint of the provider.
     */
    public static final String HINT = "wiki";

    private static final LocalDocumentReference GLOBAL_PREFERENCES = new LocalDocumentReference(
            Arrays.asList("XWiki", "Notifications", "Code"), "NotificationAdministration");

    @Inject
    @Named("cached")
    private ModelBridge modelBridge;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user)
            throws NotificationException
    {
        return modelBridge.getFilterPreferences(new DocumentReference(GLOBAL_PREFERENCES, user.getWikiReference()),
                HINT);
    }

    @Override
    public void saveFilterPreferences(Set<NotificationFilterPreference> filterPreferences) throws NotificationException
    {
        List<NotificationFilterPreference> list = filterPreferences.stream().filter(
            fp -> HINT.equals(fp.getProviderHint())).collect(Collectors.toList());
        modelBridge.saveFilterPreferences(getCurrentUserWikiPreference(), list);
    }

    private DocumentReference getCurrentUserWikiPreference()
    {
        return new DocumentReference(GLOBAL_PREFERENCES,
                documentAccessBridge.getCurrentDocumentReference().getWikiReference());
    }

    @Override
    public void deleteFilterPreference(String filterPreferenceName) throws NotificationException
    {
        modelBridge.deleteFilterPreference(getCurrentUserWikiPreference(), filterPreferenceName);
    }

    @Override
    public void setFilterPreferenceEnabled(String filterPreferenceName, boolean enabled)
            throws NotificationException
    {
        modelBridge.setFilterPreferenceEnabled(getCurrentUserWikiPreference(), filterPreferenceName, enabled);
    }

    @Override
    public int getProviderPriority()
    {
        return 100;
    }
}
