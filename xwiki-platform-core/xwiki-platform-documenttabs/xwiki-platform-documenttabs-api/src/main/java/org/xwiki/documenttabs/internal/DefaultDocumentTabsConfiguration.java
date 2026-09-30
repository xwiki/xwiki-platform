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
package org.xwiki.documenttabs.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.documenttabs.DocumentTabsConfiguration;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

/**
 * Reads the tab visibility from the {@code XWiki.XWikiPreferences} objects of the space and wiki preferences documents.
 *
 * @version $Id$
 * @since 18.9.0RC1
 */
@Component
@Singleton
public class DefaultDocumentTabsConfiguration implements DocumentTabsConfiguration
{
    /**
     * Name of the preferences document of a space.
     */
    private static final String WEB_PREFERENCES = "WebPreferences";

    /**
     * Space of the wiki preferences document, which is also the space of the preferences class.
     */
    private static final String XWIKI_SPACE = "XWiki";

    /**
     * Name of the wiki preferences document.
     */
    private static final String XWIKI_PREFERENCES = "XWikiPreferences";

    /**
     * Values that {@code $xwiki.getSpacePreference()} callers historically treat as {@code false}; any other non empty
     * value means {@code true}.
     */
    private static final Set<String> FALSE_VALUES = Set.of("false", "no", "0");

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Override
    public boolean areTabsDisplayed()
    {
        for (DocumentReference preferencesDocument : getPreferencesDocuments()) {
            String value = getStringProperty(preferencesDocument, SHOW_TABS_PROPERTY);
            if (!value.isEmpty()) {
                return !FALSE_VALUES.contains(value.toLowerCase());
            }
        }

        return true;
    }

    @Override
    public boolean isTabVisible(String tabId, boolean defaultVisibility)
    {
        String shown = '+' + tabId;
        String hidden = '-' + tabId;
        for (DocumentReference preferencesDocument : getPreferencesDocuments()) {
            for (String entry : getListProperty(preferencesDocument, TABS_VISIBILITY_PROPERTY)) {
                if (shown.equals(entry)) {
                    return true;
                } else if (hidden.equals(entry)) {
                    return false;
                }
            }
        }

        return defaultVisibility;
    }

    /**
     * @return the preferences documents to look at, closest first: the {@code WebPreferences} of the current space,
     *         then of each of its ancestors, and finally the wiki preferences
     */
    private List<DocumentReference> getPreferencesDocuments()
    {
        DocumentReference currentDocument = this.documentAccessBridge.getCurrentDocumentReference();
        if (currentDocument == null) {
            return List.of();
        }

        List<DocumentReference> preferencesDocuments = new ArrayList<>();
        EntityReference space = currentDocument.extractReference(EntityType.SPACE);
        while (space != null) {
            preferencesDocuments.add(new DocumentReference(WEB_PREFERENCES, new SpaceReference(space)));
            space = space.getParent();
            if (space != null && space.getType() != EntityType.SPACE) {
                space = null;
            }
        }
        SpaceReference xwikiSpace = new SpaceReference(XWIKI_SPACE, currentDocument.getWikiReference());
        preferencesDocuments.add(new DocumentReference(XWIKI_PREFERENCES, xwikiSpace));

        return preferencesDocuments;
    }

    private Object getProperty(DocumentReference preferencesDocument, String propertyName)
    {
        WikiReference wiki = preferencesDocument.getWikiReference();
        DocumentReference classReference =
            new DocumentReference(XWIKI_PREFERENCES, new SpaceReference(XWIKI_SPACE, wiki));

        return this.documentAccessBridge.getProperty(preferencesDocument, classReference, propertyName);
    }

    private String getStringProperty(DocumentReference preferencesDocument, String propertyName)
    {
        Object value = getProperty(preferencesDocument, propertyName);

        return value == null ? "" : value.toString().trim();
    }

    private Collection<String> getListProperty(DocumentReference preferencesDocument, String propertyName)
    {
        Object value = getProperty(preferencesDocument, propertyName);
        if (value instanceof Collection) {
            return ((Collection<?>) value).stream().map(String::valueOf).toList();
        }

        return List.of();
    }
}
