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
package org.xwiki.notifications.filters.internal.scope;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.generics.AbstractNode;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;
import org.xwiki.notifications.filters.internal.AbstractScopeOrUserNotificationFilter;
import org.xwiki.notifications.filters.internal.LocationOperatorNodeGenerator;
import org.xwiki.notifications.preferences.NotificationPreference;

/**
 * Define a notification filter based on a scope in the wiki.
 *
 * @version $Id$
 * @since 9.9RC1
 */
@Component
@Named(ScopeNotificationFilter.FILTER_NAME)
@Singleton
public class ScopeNotificationFilter extends AbstractScopeOrUserNotificationFilter<ScopeNotificationFilterPreference>
{
    /**
     * Name of the filter.
     */
    public static final String FILTER_NAME = "scopeNotificationFilter";

    @Inject
    private LocationOperatorNodeGenerator locationOperatorNodeGenerator;

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private ScopeNotificationFilterExpressionGenerator scopeNotificationFilterExpressionGenerator;

    /**
     * Constructs a ScopeNotificationFilter.
     */
    public ScopeNotificationFilter()
    {
        super(FILTER_NAME);
    }

    @Override
    public AbstractNode filterExpression(DocumentReference user, NotificationPreference preference)
    {
        return scopeNotificationFilterExpressionGenerator.filterExpression(user, preference);
    }

    @Override
    public AbstractOperatorNode generateFilterExpression(DocumentReference user, NotificationPreference preference,
            NotificationFilterType filterType)
    {
        // TODO: maybe stop using AbstractScopeOrUserNotificationFilter
        // Not used
        return null;
    }

    @Override
    protected boolean matchRestriction(Event event, ScopeNotificationFilterPreference scopePreference)
            throws NotificationException
    {
        return event.getDocument().equals(scopePreference.getScopeReference())
                || event.getDocument().hasParent(scopePreference.getScopeReference());
    }

    @Override
    protected ScopeNotificationFilterPreference convertPreferences(NotificationFilterPreference pref)
    {
        return new ScopeNotificationFilterPreference(pref, entityReferenceResolver);
    }

    @Override
    protected AbstractOperatorNode generateNode(ScopeNotificationFilterPreference filterPreferenceScope)
    {
        return locationOperatorNodeGenerator.generateNode(filterPreferenceScope.getScopeReference());
    }

    @Override
    protected int getDeepLevel(NotificationFilterPreference pref)
    {
        ScopeNotificationFilterPreference scopeNotificationFilterPreference = (ScopeNotificationFilterPreference) pref;
        return scopeNotificationFilterPreference.getScopeReference().size();
    }
}
