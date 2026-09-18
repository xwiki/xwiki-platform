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
package org.xwiki.documenttabs;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Visibility of the tabs displayed below the content of a page (comments, attachments, history, information, and any
 * tab contributed to the {@code org.xwiki.plaftorm.template.docextra} UI extension point).
 * <p>
 * Both settings below are read from the {@code XWiki.XWikiPreferences} object of the closest preferences document: the
 * {@code WebPreferences} of the current space, then of each of its ancestors, then the wiki preferences. This is the
 * same cascade as {@code $xwiki.getSpacePreference()}, but applied per tab rather than per property, so that a space
 * can hide a single tab without having to restate the visibility of all the others.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Role
@Unstable
public interface DocumentTabsConfiguration
{
    /**
     * Name of the {@code XWiki.XWikiPreferences} property holding the master switch.
     */
    String SHOW_TABS_PROPERTY = "showdocumenttabs";

    /**
     * Name of the {@code XWiki.XWikiPreferences} property holding the per tab visibility. Each entry is a tab
     * identifier prefixed by {@code +} when the tab is shown and by {@code -} when it is hidden; a tab that has no
     * entry at a given level inherits the value of the next level up.
     */
    String TABS_VISIBILITY_PROPERTY = "documentTabsVisibility";

    /**
     * @return {@code false} when the whole tab area must be hidden, which no individual tab can override
     */
    boolean areTabsDisplayed();

    /**
     * @param tabId the identifier of the tab, i.e. the identifier of its UI extension
     * @param defaultVisibility the visibility to use when no administration level defines one for that tab, which is
     *            what the tab itself asks for through the {@code show} parameter of its UI extension
     * @return {@code true} when the tab must be displayed
     */
    boolean isTabVisible(String tabId, boolean defaultVisibility);
}
