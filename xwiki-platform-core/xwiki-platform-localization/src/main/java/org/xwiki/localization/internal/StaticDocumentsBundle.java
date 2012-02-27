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
package org.xwiki.localization.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.localization.Bundle;
import org.xwiki.localization.WikiInformation;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * Bundle corresponding to global (at the wiki level) localization documents. A Properties object is constructed for
 * each wiki and for each language, containing all the translations from the different documents configured in that
 * wiki. Although it takes more time to rebuild the properties when the list of documents changes, it saves the extra
 * calls that would be needed if each document would be stored in its own Properties object.
 * 
 * @version $Id$
 */
@Component("staticDocuments")
public class StaticDocumentsBundle extends AbstractWikiBundle implements Bundle, EventListener, Initializable
{
    /** The name of the property containing the list of global document bundles. */
    private static final String DOCUMENT_BUNDLE_PROPERTY = "documentBundles";

    /** String to use when joining the list of document names. */
    private static final String JOIN_SEPARATOR = ",";

    /**
     * <p>
     * Cached bundles corresponding to all the documents globally registered in the wiki preferences. For each wiki, for
     * each language, only one {@link Properties} object is created, since this needs less memory than keeping one
     * object per document, and this list is not supposed to change very often. This trades an increased initial
     * overhead for a better performance afterwards.
     * </p>
     * <p>
     * Map: (wiki name -&gt; map: (language -&gt; bundle)).
     * </p>
     */
    private Map<String, Map<String, Properties>> staticBundles = new HashMap<String, Map<String, Properties>>();

    /**
     * <p>
     * Cache storing the previously seen list of document bundles for each wiki.
     * </p>
     * <p>
     * Map: (wiki name -&gt; array of document names).
     * </p>
     */
    private Map<String, String[]> staticBundleNames = new HashMap<String, String[]>();

    @Override
    public void initialize() throws InitializationException
    {
        // Set the Bundle priority
        setPriority(300);
    }

    @Override
    public String getTranslation(String key, String language)
    {
        String translation = key;
        try {
            Properties props = getStaticBundle(this.wikiInfo.getCurrentWikiName(), language);
            if (props.containsKey(key)) {
                translation = props.getProperty(key);
            }
        } catch (Exception e) {
            getLogger().info("Unhandled exception while translating: {0}", e.getMessage());
        }
        return translation;
    }

    /**
     * {@inheritDoc}
     * <p>
     * If the configured list of global bundles was changed, invalidate the whole wiki bundle.
     * </p>
     * 
     * @see EventListener#onEvent(Event, Object, Object)
     * @see AbstractWikiBundle#onEvent(Event, Object, Object)
     */
    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentModelBridge doc = (DocumentModelBridge) source;
        if (WikiInformation.PREFERENCES_DOCUMENT_NAME.equals(doc.getFullName())) {
            synchronized (this.staticBundleNames) {
                String wiki = doc.getWikiName();
                if (this.staticBundleNames.containsKey(wiki)) {
                    // If we call remove here, the new value will be re-inserted by the following call.
                    String oldBundleNames = StringUtils.join(this.staticBundleNames.remove(wiki), JOIN_SEPARATOR);
                    String newBundleNames = StringUtils.join(this.getStaticDocumentBundles(wiki), JOIN_SEPARATOR);
                    if (!StringUtils.equals(oldBundleNames, newBundleNames)) {
                        synchronized (this.staticBundles) {
                            this.staticBundles.remove(wiki);
                        }
                    }
                }
            }
        }

        // Invalidate individual translations if needed.
        super.onEvent(event, source, data);
    }

    /**
     * Retrieves the translations for a given wiki/language. If these properties are already cached, then use them.
     * Otherwise, first load from the wiki and update the cache. The returned Properties object contain the translations
     * for all documents registered as localization bundles for a wiki.
     * 
     * @param wiki The target wiki.
     * @param language The 2-character code of the requested language.
     * @return A {@link Properties} object with the static translations of the wiki, in the requested language.
     */
    protected Properties getStaticBundle(String wiki, String language)
    {
        synchronized (this.staticBundles) {
            Map<String, Properties> wikiBundles;
            if (!this.staticBundles.containsKey(wiki)) {
                wikiBundles = new HashMap<String, Properties>();
                this.staticBundles.put(wiki, wikiBundles);
            } else {
                wikiBundles = this.staticBundles.get(wiki);
            }

            if (wikiBundles.containsKey(language)) {
                return wikiBundles.get(language);
            }

            Properties result;
            result = this.loadStaticBundle(wiki, language);
            wikiBundles.put(language, result);
            return result;
        }
    }

    /**
     * Constructs the {@link Properties} object corresponding to a wiki in a given language, from all the individual
     * documents registered as wiki-wide localization bundles.
     * 
     * @param wiki The target wiki.
     * @param language The 2-character code of the requested language.
     * @return A {@link Properties} object with the static translations of the wiki, in the requested language.
     */
    protected Properties loadStaticBundle(String wiki, String language)
    {
        Properties properties = new Properties();
        String[] bundles = getStaticDocumentBundles(wiki);
        // Reverse the order of the bundles, so that the first entry in the list has the most priority.
        ArrayUtils.reverse(bundles);
        for (String documentName : bundles) {
            String fullDocumentName = documentName;
            try {
                if (documentName.indexOf(WikiInformation.WIKI_PREFIX_SEPARATOR) < 0) {
                    fullDocumentName = wiki + WikiInformation.WIKI_PREFIX_SEPARATOR + documentName;
                }
                properties.putAll(getDocumentBundle(fullDocumentName, language));
            } catch (Exception ex) {
                getLogger().warn("Exception loading document bundle [{0}]", fullDocumentName);
            }
        }
        // Also watch the preferences for this wiki, in case the list of documents is changed.
        watchDocument(wiki + WikiInformation.WIKI_PREFIX_SEPARATOR + WikiInformation.PREFERENCES_DOCUMENT_NAME);
        return properties;
    }

    /**
     * Return the list of document names configured for a wiki as global localization bundles. If this list is already
     * cached, then use it. Otherwise, first load from the wiki and update the cache.
     * 
     * @param wiki The target wiki.
     * @return An array of <code>String</code>, each value representing the name of a document that should be used as a
     *         global localization resource. The names do not usually contain the wiki prefix, as these are the raw
     *         values taken from the wiki settings.
     */
    protected String[] getStaticDocumentBundles(String wiki)
    {
        synchronized (this.staticBundleNames) {
            if (this.staticBundleNames.containsKey(wiki)) {
                return this.staticBundleNames.get(wiki);
            }
            String[] result = new String[0];
            try {
                String bundles =
                    this.documentAccessBridge.getProperty(wiki + WikiInformation.WIKI_PREFIX_SEPARATOR
                        + WikiInformation.PREFERENCES_DOCUMENT_NAME, WikiInformation.PREFERENCES_CLASS_NAME,
                        DOCUMENT_BUNDLE_PROPERTY);
                if (!StringUtils.isBlank(bundles)) {
                    result = bundles.split("[,|]\\s*");
                }
            } catch (Exception ex) {
                getLogger().warn("Cannot access a wiki setting", ex);
            }
            this.staticBundleNames.put(wiki, result);
            return result;
        }
    }
}
