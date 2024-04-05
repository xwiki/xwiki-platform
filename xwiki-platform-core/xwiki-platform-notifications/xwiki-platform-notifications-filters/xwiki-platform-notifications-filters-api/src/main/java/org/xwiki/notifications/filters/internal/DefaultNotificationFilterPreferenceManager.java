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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterPreferenceProvider;
import org.xwiki.notifications.filters.NotificationFilterType;

/**
 * Default implementation of {@link NotificationFilterPreferenceManager}.
 *
 * @version $Id$
 * @since 10.9
 */
@Component
@Singleton
public class DefaultNotificationFilterPreferenceManager implements NotificationFilterPreferenceManager
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @FunctionalInterface
    interface ProviderCallable
    {
        void doInProvider(NotificationFilterPreferenceProvider provider) throws NotificationException;
    }

    @FunctionalInterface
    interface RetrieveWithProviderCallable<E>
    {
        Collection<E> retrieveWithProvider(NotificationFilterPreferenceProvider provider)
            throws NotificationException;
    }

    private List<NotificationFilterPreferenceProvider> getProviderList() throws NotificationException
    {
        try {
            return componentManager.getInstanceList(NotificationFilterPreferenceProvider.class);
        } catch (ComponentLookupException e) {
            throw new NotificationException("Error when trying to load the list of providers", e);
        }
    }

    private String getProviderDebugMessage(String loggerMessage, NotificationFilterPreferenceProvider provider)
    {
        return String.format("%s with provider %s", loggerMessage, provider);
    }

    private String getExceptionMessage(String loggerMessage, List<NotificationException> exceptions)
    {
        return String.format("%s - All providers called failed, see exceptions: [%s].",
            loggerMessage,
            exceptions.stream().map(ExceptionUtils::getRootCauseMessage).collect(Collectors.joining(",")));
    }

    private void providerExceptionWrapper(ProviderCallable callable, String loggerMessage) throws NotificationException
    {
        boolean allFailing = true;
        List<NotificationException> exceptions = new ArrayList<>();
        List<NotificationFilterPreferenceProvider> providerList = getProviderList();
        if (providerList.size() > 1) {
            for (NotificationFilterPreferenceProvider provider : providerList) {
                try {
                    callable.doInProvider(provider);
                    allFailing = false;
                } catch (NotificationException e) {
                    this.logger.debug(getProviderDebugMessage(loggerMessage, provider), e);
                    exceptions.add(e);
                }
            }
            if (allFailing) {
                throw new NotificationException(getExceptionMessage(loggerMessage, exceptions));
            }
        } else {
            callable.doInProvider(providerList.get(0));
        }
    }

    private <E> Collection<E> retrieveWithProviderExceptionWrapper(RetrieveWithProviderCallable<E> callable,
        String loggerMessage) throws NotificationException
    {
        boolean allFailing = true;
        List<NotificationException> exceptions = new ArrayList<>();
        Set<E> result = new HashSet<>();
        List<NotificationFilterPreferenceProvider> providerList = getProviderList();
        if (providerList.size() > 1) {
            for (NotificationFilterPreferenceProvider provider : getProviderList()) {
                try {
                    result.addAll(callable.retrieveWithProvider(provider));
                    allFailing = false;
                } catch (NotificationException e) {
                    this.logger.debug(getProviderDebugMessage(loggerMessage, provider), e);
                    exceptions.add(e);
                }
            }
            if (allFailing) {
                throw new NotificationException(getExceptionMessage(loggerMessage, exceptions));
            }
        } else {
            result.addAll(callable.retrieveWithProvider(providerList.get(0)));
        }
        return result;
    }

    @Override
    public Collection<NotificationFilterPreference> getFilterPreferences(DocumentReference user)
            throws NotificationException
    {
        return this.retrieveWithProviderExceptionWrapper(provider -> provider.getFilterPreferences(user),
            String.format("Error when trying to get filter preferences for user [%s]", user));
    }

    @Override
    public Collection<NotificationFilterPreference> getFilterPreferences(WikiReference wikiReference)
        throws NotificationException
    {
        return this.retrieveWithProviderExceptionWrapper(provider -> provider.getFilterPreferences(wikiReference),
            String.format("Error when trying to get filter preferences for wiki [%s]", wikiReference));
    }

    @Override
    public Stream<NotificationFilterPreference> getFilterPreferences(
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilter filter)
    {
        return filterPreferences.stream().filter(preference -> filter.getName().equals(preference.getFilterName()));
    }

    @Override
    public Stream<NotificationFilterPreference> getFilterPreferences(
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilter filter,
            NotificationFilterType filterType)
    {
        return getFilterPreferences(filterPreferences, filter).filter(
            preference -> preference.getFilterType() == filterType);
    }

    @Override
    public Stream<NotificationFilterPreference> getFilterPreferences(
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilter filter,
            NotificationFilterType filterType, NotificationFormat format)
    {
        return getFilterPreferences(filterPreferences, filter, filterType).filter(
            preference -> preference.getNotificationFormats().contains(format));
    }

    @Override
    public void saveFilterPreferences(DocumentReference user, Set<NotificationFilterPreference> filterPreferences)
    {
        Map<String, Set<NotificationFilterPreference>> preferencesMapping = new HashMap<>();

        for (NotificationFilterPreference filterPreference : filterPreferences) {
            // Try to get the corresponding provider, if no provider can be found, discard the save of the preference
            String providerHint = filterPreference.getProviderHint();
            if (componentManager.hasComponent(NotificationFilterPreferenceProvider.class, providerHint)) {
                if (!preferencesMapping.containsKey(providerHint)) {
                    preferencesMapping.put(providerHint, new HashSet<>());
                }

                preferencesMapping.get(providerHint).add(filterPreference);
            }
        }

        // Once we have created the mapping, save all the preferences using their correct providers
        for (String providerHint : preferencesMapping.keySet()) {
            try {
                NotificationFilterPreferenceProvider provider =
                        componentManager.getInstance(NotificationFilterPreferenceProvider.class, providerHint);

                provider.saveFilterPreferences(user, preferencesMapping.get(providerHint));

            } catch (ComponentLookupException e) {
                logger.error("Unable to retrieve the notification filter preference provider for hint [{}]:",
                        providerHint, e);
            } catch (NotificationException e) {
                logger.warn("Unable save the filter preferences [{}] against the provider [{}]: [{}]",
                        preferencesMapping.get(providerHint), providerHint, ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    @Override
    public void deleteFilterPreference(DocumentReference user, String filterPreferenceId) throws NotificationException
    {
        deleteFilterPreferences(user, Set.of(filterPreferenceId));
    }

    @Override
    public void deleteFilterPreferences(DocumentReference user, Set<String> filterPreferenceIds)
        throws NotificationException
    {
        this.providerExceptionWrapper(provider -> provider.deleteFilterPreferences(user, filterPreferenceIds),
            String.format("Error when trying to remove filter preferences %s for user [%s]", filterPreferenceIds,
                user));
    }

    @Override
    public void deleteFilterPreference(WikiReference wikiReference, String filterPreferenceId)
        throws NotificationException
    {
        this.providerExceptionWrapper(provider -> provider.deleteFilterPreference(wikiReference, filterPreferenceId),
            String.format("Error when trying to remove filter preference [%s] for wiki [%s]", filterPreferenceId,
                wikiReference));
    }


    @Override
    public void setFilterPreferenceEnabled(DocumentReference user, String filterPreferenceId, boolean enabled)
            throws NotificationException
    {
        this.providerExceptionWrapper(provider ->
                provider.setFilterPreferenceEnabled(user, filterPreferenceId, enabled),
            String.format("Error when trying to set filter preference [%s] enabled to [%s] for user [%s]",
                enabled,
                filterPreferenceId,
                user));
    }

    @Override
    public void setFilterPreferenceEnabled(WikiReference wikiReference, String filterPreferenceId, boolean enabled)
        throws NotificationException
    {
        this.providerExceptionWrapper(provider ->
                provider.setFilterPreferenceEnabled(wikiReference, filterPreferenceId, enabled),
            String.format("Error when trying to set filter preference [%s] enabled to [%s] for wiki [%s]",
                enabled,
                filterPreferenceId,
                wikiReference));
    }

    @Override
    public void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException
    {
        this.providerExceptionWrapper(provider ->
                provider.setStartDateForUser(user, startDate),
            String.format("Error when trying to set start date to [%s] for user [%s]",
                startDate,
                user));
    }
}
