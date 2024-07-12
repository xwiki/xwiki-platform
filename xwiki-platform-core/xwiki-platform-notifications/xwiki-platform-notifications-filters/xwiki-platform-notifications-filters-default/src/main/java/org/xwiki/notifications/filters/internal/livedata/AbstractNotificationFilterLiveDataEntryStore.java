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

package org.xwiki.notifications.filters.internal.livedata;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;

/**
 * Abstract class providing helpers for both
 * {@link org.xwiki.notifications.filters.internal.livedata.custom.NotificationCustomFiltersLiveDataEntryStore} and
 * {@link org.xwiki.notifications.filters.internal.livedata.system.NotificationSystemFiltersLiveDataEntryStore}.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
public abstract class AbstractNotificationFilterLiveDataEntryStore implements LiveDataEntryStore
{
    private static final String TARGET_SOURCE_PARAMETER = "target";
    private static final String WIKI_SOURCE_PARAMETER = "wiki";
    private static final String UNAUTHORIZED_EXCEPTION_MSG = "You don't have rights to access those information.";

    @Inject
    protected NotificationFilterLiveDataTranslationHelper translationHelper;

    @Inject
    protected EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    protected EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    protected static final class TargetInformation
    {
        public boolean isWikiTarget;
        public EntityReference ownerReference;

    }

    protected Map<String, Object> getStaticListInfo(List<String> items)
    {
        return Map.of(
            "extraClass", "list-unstyled",
            "items", items
        );
    }

    protected Map<String, Object> displayNotificationFormats(Collection<NotificationFormat> notificationFormats)
    {
        List<String> items = notificationFormats
            .stream()
            .sorted(Comparator.comparing(NotificationFormat::name))
            .map(notificationFormat -> this.translationHelper.getFormatTranslation(notificationFormat))
            .toList();

        return getStaticListInfo(items);
    }

    protected TargetInformation getTargetInformation(LiveDataQuery query) throws LiveDataException
    {
        Map<String, Object> sourceParameters = query.getSource().getParameters();
        if (!sourceParameters.containsKey(TARGET_SOURCE_PARAMETER)) {
            throw new LiveDataException("The target source parameter is mandatory.");
        }
        String target = String.valueOf(sourceParameters.get(TARGET_SOURCE_PARAMETER));
        TargetInformation result = new TargetInformation();
        if (WIKI_SOURCE_PARAMETER.equals(target)) {
            result.isWikiTarget = true;
            result.ownerReference =
                this.entityReferenceResolver.resolve(String.valueOf(sourceParameters.get(WIKI_SOURCE_PARAMETER)),
                    EntityType.WIKI);
        } else {
            result.isWikiTarget = false;
            result.ownerReference = this.entityReferenceResolver.resolve(String.valueOf(sourceParameters.get(target)),
                EntityType.DOCUMENT);
        }
        if (!this.contextualAuthorizationManager.hasAccess(Right.ADMIN)
            && !result.ownerReference.equals(getCurrentUserReference())) {
            throw new LiveDataException(UNAUTHORIZED_EXCEPTION_MSG);
        }
        return result;
    }

    protected WikiReference getCurrentWikiReference()
    {
        XWikiContext context = this.contextProvider.get();
        return context.getWikiReference();
    }

    private DocumentReference getCurrentUserReference()
    {
        XWikiContext context = this.contextProvider.get();
        return context.getUserReference();
    }

    protected void checkAccessFilterPreference(DefaultNotificationFilterPreference filterPreference)
        throws LiveDataException
    {
        if (!this.contextualAuthorizationManager.hasAccess(Right.ADMIN)
            && !Objects.equals(filterPreference.getOwner(),
            this.entityReferenceSerializer.serialize(getCurrentUserReference()))) {
            throw new LiveDataException(UNAUTHORIZED_EXCEPTION_MSG);
        }
    }
}
