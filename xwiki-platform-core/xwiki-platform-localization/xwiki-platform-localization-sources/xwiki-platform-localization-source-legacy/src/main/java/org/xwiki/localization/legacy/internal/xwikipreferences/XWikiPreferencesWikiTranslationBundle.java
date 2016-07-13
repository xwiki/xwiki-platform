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
package org.xwiki.localization.legacy.internal.xwikipreferences;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.cache.DisposableCacheValue;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.localization.Translation;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.internal.AbstractTranslationBundle;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.internal.event.XObjectPropertyAddedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;

/**
 * @version $Id$
 * @since 4.3M2
 */
public class XWikiPreferencesWikiTranslationBundle extends AbstractTranslationBundle implements EventListener,
    DisposableCacheValue
{
    public static final String ID = XWikiPreferencesTranslationBundle.ID + ".wiki";

    /**
     * The name of the property containing the list of global document bundles.
     */
    private static final String DOCUMENT_BUNDLE_PROPERTY = "documentBundles";

    /**
     * String to use when joining the list of document names.
     */
    private static final String JOIN_SEPARATOR = ",";

    private ObservationManager observation;

    private DocumentAccessBridge documentAccessBridge;

    private DocumentReferenceResolver<String> resolver;

    private final List<Event> events;

    private final String wiki;

    private XWikiPreferencesTranslationBundle parent;

    private Map<DocumentReference, XWikiPreferencesDocumentTranslationBundle> bundles;

    public XWikiPreferencesWikiTranslationBundle(String wiki, XWikiPreferencesTranslationBundle parent,
        ComponentManager componentManager) throws ComponentLookupException
    {
        super(XWikiPreferencesWikiTranslationBundle.ID + '.' + wiki);

        this.wiki = wiki;
        this.parent = parent;

        this.observation = componentManager.getInstance(ObservationManager.class);
        this.documentAccessBridge = componentManager.getInstance(DocumentAccessBridge.class);
        this.resolver = componentManager.getInstance(DocumentReferenceResolver.TYPE_STRING);

        intializeBundles();

        // Observation

        DocumentReference preferences = new DocumentReference(this.wiki, "XWiki", "XWikiPreferences");

        EntityReference documentBundlesProperty =
            new EntityReference(DOCUMENT_BUNDLE_PROPERTY, EntityType.OBJECT_PROPERTY, new RegexEntityReference(
                Pattern.compile(this.wiki + ":XWiki.XWikiPreferences\\[\\d*\\]"), EntityType.OBJECT, preferences));

        this.events =
            Arrays.<Event>asList(new XObjectPropertyAddedEvent(documentBundlesProperty),
                new XObjectPropertyUpdatedEvent(documentBundlesProperty));

        this.observation.addListener(this);
    }

    private Set<DocumentReference> getDocuments()
    {
        DocumentReference preferencesReference = new DocumentReference(this.wiki, "XWiki", "XWikiPreferences");

        String documentNameListString =
            (String) this.documentAccessBridge.getProperty(preferencesReference, preferencesReference,
                DOCUMENT_BUNDLE_PROPERTY);

        Set<DocumentReference> documents;
        if (documentNameListString != null) {
            String[] documentNameList = documentNameListString.split(JOIN_SEPARATOR);

            documents = new LinkedHashSet<DocumentReference>(documentNameList.length);
            for (String documentName : documentNameList) {
                documents.add(this.resolver.resolve(documentName.trim(), preferencesReference));
            }
        } else {
            documents = Collections.emptySet();
        }

        return documents;
    }

    private void intializeBundles()
    {
        Set<DocumentReference> documents = getDocuments();

        Map<DocumentReference, XWikiPreferencesDocumentTranslationBundle> newBundles =
            new LinkedHashMap<DocumentReference, XWikiPreferencesDocumentTranslationBundle>(documents.size());
        for (DocumentReference document : documents) {
            newBundles.put(document, this.parent.getDocumentTranslationBundle(document));
        }

        this.bundles = newBundles;
    }

    // EventListeners

    @Override
    public String getName()
    {
        return "localization.bundle." + getId();
    }

    @Override
    public List<Event> getEvents()
    {
        return this.events;
    }

    @Override
    public void onEvent(Event arg0, Object arg1, Object arg2)
    {
        intializeBundles();
    }

    // Bundle

    @Override
    public Translation getTranslation(String key, Locale locale)
    {
        for (TranslationBundle bundle : this.bundles.values()) {
            Translation translation = bundle.getTranslation(key, locale);
            if (translation != null && translation.getLocale().equals(locale)) {
                return translation;
            }
        }

        // Try parent locale
        Locale parentLocale = LocaleUtils.getParentLocale(locale);
        if (parentLocale != null) {
            return getTranslation(key, parentLocale);
        }

        return null;
    }

    @Override
    public void dispose() throws Exception
    {
        this.observation.removeListener(getName());
    }
}
