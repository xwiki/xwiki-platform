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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;

/**
 * A hierarchy of scope notification filter preferences.
 *
 * @version $Id$
 * @since 9.9RC1
 */
public class ScopeNotificationFilterPreferencesHierarchy
{
    private List<ScopeNotificationFilterPreference> preferences;

    /**
     * Construct a hierarchy of scope notification filter preferences.
     * @param preferences a list of scope notification filter preferences.
     */
    public ScopeNotificationFilterPreferencesHierarchy(
            List<ScopeNotificationFilterPreference> preferences)
    {
        this.preferences = preferences;

        // Compare preferences 2 by 2 to see if some are children of the other
        for (ScopeNotificationFilterPreference pref : preferences) {
            for (ScopeNotificationFilterPreference otherPref : preferences) {
                if (pref == otherPref) {
                    continue;
                }
                if (otherPref.isParentOf(pref)) {
                    otherPref.addChild(pref);
                }
            }
        }
    }

    /**
     * @return the list of documents to include (page is the deepest possible level so it cannot be contradicted)
     */
    public ArrayList<String> getPagesToInclude()
    {
        Iterator<ScopeNotificationFilterPreference> it = getFiltersAboutPages().filter(
            pref -> pref.getFilterType() == NotificationFilterType.INCLUSIVE).iterator();
        ArrayList<String> pagesToInclude = new ArrayList<>();
        while (it.hasNext()) {
            ScopeNotificationFilterPreference pref = it.next();
            for (String page : pref.getProperties(NotificationFilterProperty.PAGE)) {
                pagesToInclude.add(page);
            }
        }
        return pagesToInclude;
    }

    /**
     * @return the list of documents to exclude (page is the deepest possible level so it cannot be contradicted)
     */
    public ArrayList<String> getPagesToExclude()
    {
        Iterator<ScopeNotificationFilterPreference> it = getFiltersAboutPages().filter(
            pref -> pref.getFilterType() == NotificationFilterType.EXCLUSIVE).iterator();
        ArrayList<String> pagesToExclude = new ArrayList<>();
        while (it.hasNext()) {
            ScopeNotificationFilterPreference pref = it.next();
            for (String page : pref.getProperties(NotificationFilterProperty.PAGE)) {
                pagesToExclude.add(page);
            }
        }
        return pagesToExclude;
    }

    private Stream<ScopeNotificationFilterPreference> getFiltersAboutPages()
    {
        return preferences.stream().filter(pref -> !pref.getProperties(NotificationFilterProperty.PAGE).isEmpty());
    }

    /**
     * @return an iterator to get top level exclusive filters (ie the black list) except those that concern a document
     */
    public Iterator<ScopeNotificationFilterPreference> getExclusiveFiltersThatHasNoParents()
    {
        return preferences.stream().filter(
            pref -> !pref.hasParent() && pref.getFilterType() == NotificationFilterType.EXCLUSIVE
                && pref.getProperties(NotificationFilterProperty.PAGE).isEmpty()
        ).iterator();
    }

    /**
     * @return an iterator to get top level inclusive filters (ie the white list) except those that concern a document
     */
    public Iterator<ScopeNotificationFilterPreference> getInclusiveFiltersThatHasNoParents()
    {
        return preferences.stream().filter(
            pref -> !pref.hasParent() && pref.getFilterType() == NotificationFilterType.INCLUSIVE
                && pref.getProperties(NotificationFilterProperty.PAGE).isEmpty()
        ).iterator();
    }

    /**
     * @return if the hierarchy is empty
     */
    public boolean isEmpty()
    {
        return preferences.isEmpty();
    }
}
