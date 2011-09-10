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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.localization.Bundle;
import org.xwiki.localization.WikiInformation;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.DocumentDeleteEvent;
import org.xwiki.observation.event.DocumentSaveEvent;
import org.xwiki.observation.event.DocumentUpdateEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.observation.event.filter.FixedNameEventFilter;

/**
 * Base class for {@link Bundle}s using wiki documents as resources. Provides methods for loading properties from
 * documents, watching loaded documents and invalidating cached translations.
 * 
 * @version $Id$
 */
public abstract class AbstractWikiBundle extends AbstractBundle implements Bundle, EventListener
{
    /** The encoding used for storing unicode characters as bytes. */
    protected static final String UNICODE_BYTE_ENCODING = "UTF-8";

    /** The encoding assumed by the ResourceBundle loader. */
    protected static final String DEFAULT_RESOURCE_BYTE_ENCODING = "ISO-8859-1";

    /** Allows to register for receiving notifications about document changes. */
    @Requirement
    protected ObservationManager observation;

    /** Provides access to documents. */
    @Requirement
    protected DocumentAccessBridge documentAccessBridge;

    /**
     * Caching the default language for each wiki.
     */
    protected Map<String, String> defaultWikiLanguage = new HashMap<String, String>();

    /**
     * <p>
     * Cached properties corresponding to resources loaded from wiki documents.
     * </p>
     * <p>
     * Map: (document name -&gt; map: (language -&gt; properties)). The document name is always prefixed with the wiki
     * name.
     * </p>
     */
    protected Map<String, Map<String, Properties>> documentBundles = new HashMap<String, Map<String, Properties>>();

    /**
     * Retrieves the properties loaded from the document. If these properties are already cached, then use them.
     * Otherwise, first load from the wiki and update the cache.
     * 
     * @param documentName The full name of the source document, including the wiki prefix.
     * @param language The translation to retrieve.
     * @return A {@link Properties} object corresponding to the document translation, including as a base the default
     *         language values for the keys that are not defined in the requested translation.
     * @throws Exception if the document cannot be accessed.
     * @see #loadDocumentBundle(String, String)
     */
    protected Properties getDocumentBundle(String documentName, String language) throws Exception
    {
        synchronized (this.documentBundles) {
            if (this.documentBundles.containsKey(documentName)) {
                if (this.documentBundles.get(documentName).containsKey(language)) {
                    return this.documentBundles.get(documentName).get(language);
                }
            } else {
                this.documentBundles.put(documentName, new HashMap<String, Properties>());
            }

            Properties result = loadDocumentBundle(documentName, language);
            this.documentBundles.get(documentName).put(language, result);
            watchDocument(documentName);
            return result;
        }
    }

    /**
     * Loads translations from a document, in a specified language. If the requested language is not the same as the
     * default wiki language, then the translations in the default language are first loaded as a base translation.
     * Additionally, the document is added to the list of watched documents, so that changes in the document are
     * detected.
     * 
     * @param documentName The full name of the source document, including the wiki prefix.
     * @param language The translation to load.
     * @return A {@link Properties} object loaded from the translation, including as a base the default language values
     *         for the keys that are not defined in the requested translation.
     * @throws Exception if the document cannot be accessed.
     * @see #loadDocumentBundle(String, String, String)
     */
    protected Properties loadDocumentBundle(String documentName, String language) throws Exception
    {
        String wiki;
        if (documentName.indexOf(WikiInformation.WIKI_PREFIX_SEPARATOR) > 0) {
            wiki = StringUtils.substringBefore(documentName, WikiInformation.WIKI_PREFIX_SEPARATOR);
        } else {
            wiki = this.wikiInfo.getCurrentWikiName();
        }
        return loadDocumentBundle(wiki, documentName, language);
    }

    /**
     * Loads translations from a document, in a specified language. If the requested language is not the same as the
     * default wiki language, then the translations in the default language are first loaded as a base translation.
     * Additionally, the document is added to the list of watched documents, so that changes in the document are
     * detected.
     * 
     * @param wiki The wiki containing the document.
     * @param documentName The full name of the source document, including the wiki prefix.
     * @param language The translation to load.
     * @return A {@link Properties} object loaded from the translation, including as a base the default language values
     *         for the keys that are not defined in the requested translation.
     * @throws Exception if the document cannot be accessed.
     * @see #loadPropertiesFromString(Properties, String, String)
     */
    protected Properties loadDocumentBundle(String wiki, String documentName, String language) throws Exception
    {
        Properties properties = new Properties();
        String defaultContent =
            this.documentAccessBridge.getDocumentContent(documentName, this.wikiInfo.getDefaultWikiLanguage());
        loadPropertiesFromString(properties, defaultContent, documentName);
        if (!StringUtils.equals(language, getDefaultWikiLanguage(wiki))) {
            String content = this.documentAccessBridge.getDocumentContent(documentName, language);
            loadPropertiesFromString(properties, content, documentName);
        }
        watchDocument(documentName);
        return properties;
    }

    /**
     * <p>
     * Loads the translations defined in a document into an existing {@link Properties} object. Old values from the
     * passed Properties are overwritten when translations are found in the document content.
     * </p>
     * <p>
     * Since the Properties {@link Properties#load(InputStream) load} method always assumes the stream contains only
     * ISO-8859-1 characters, we need to embed somehow Unicode characters outside the 8859-1 encoding into the input
     * stream. We achieve this by explicitly splitting the content into UTF-8 bytes, which are then read as a byte
     * stream. When loaded, the properties are wrongly created by interpreting those bytes are ISO-8859-1 characters, so
     * we must re-split and re-combine each property back into the UTF-8 encoding. Although this is time consuming, the
     * alternative is to re-implement the Properties class so that it accepts a proper character input stream. And
     * besides, resources are supposed to be loaded only once in real live wikis.
     * </p>
     * 
     * @param props The {@link Properties} object to enhance. It is modified inside the method body.
     * @param content The content from which to load translations.
     * @param documentName The name of the container wiki document. Needed for logging only.
     * @return The enhanced properties file, the same object as the <tt>props</tt> parameter.
     */
    protected Properties loadPropertiesFromString(Properties props, String content, String documentName)
    {
        byte[] bcontent;
        Properties temp = new Properties();
        try {
            // We force splitting into UTF-8 since it is supposed to be available on all platforms, and it can represent
            // all unicode characters.
            bcontent = content.getBytes(UNICODE_BYTE_ENCODING);
        } catch (UnsupportedEncodingException ex) {
            // This should not happen, ever! If it does, there is something wrong in the system.
            getLogger().error("Error splitting a document resource bundle into bytes using the UTF-8 encoding", ex);
            bcontent = content.getBytes();
        }
        InputStream is = new ByteArrayInputStream(bcontent);
        try {
            temp.load(is);
            // Adds new properties into the existing object, overriding old values, but not clearing completely.
            for (Enumeration< ? > keys = temp.propertyNames(); keys.hasMoreElements();) {
                String key = (String) keys.nextElement();
                props.setProperty(key, new String(temp.getProperty(key).getBytes(DEFAULT_RESOURCE_BYTE_ENCODING),
                    UNICODE_BYTE_ENCODING));
            }
        } catch (IOException ex) {
            // Cannot do anything more
            getLogger().error("Invalid document resource bundle: [{0}]", ex, documentName);
        }
        return props;
    }

    /**
     * Retrieves the default language configured for a wiki. This method uses a cache, If a value is defined in the
     * cache, then return it. Otherwise, ask the setting from {@link AbstractBundle#wikiInfo} and add it to the cache.
     * This method does not detect stale values, the cache will be invalidated when changes to the preferences document
     * are detected.
     * 
     * @param wiki The target wiki whose default language is to be determined.
     * @return The 2-character code of the default language of the wiki.
     */
    protected String getDefaultWikiLanguage(String wiki)
    {
        synchronized (this.defaultWikiLanguage) {
            if (this.defaultWikiLanguage.containsKey(wiki)) {
                return this.defaultWikiLanguage.get(wiki);
            }
            String result = this.wikiInfo.getDefaultWikiLanguage(wiki);
            this.defaultWikiLanguage.put(wiki, result);

            return result;
        }
    }

    /**
     * Sets a new value in the default wiki language cache, informing whether the old value was changed or not.
     * 
     * @param wiki The target wiki whose default language is to be set.
     * @param language The 2-character code of the new default language of the wiki.
     * @return <code>false</code> if the previous value was the same as the new value (the language didn't change),
     *         <code>true</code> otherwise.
     */
    protected boolean setDefaultWikiLanguage(String wiki, String language)
    {
        synchronized (this.defaultWikiLanguage) {
            return !StringUtils.equals(this.defaultWikiLanguage.put(wiki, language), language);
        }
    }

    /**
     * Registers for events on a specific document.
     * 
     * @param documentName the full name of the document to watch
     */
    protected void watchDocument(String documentName)
    {
        // addListener ignores duplicates, so it is safe to add a listener many times.
        EventFilter filter = new FixedNameEventFilter(documentName);
        this.observation.addListener(new DocumentUpdateEvent(filter), this);
        this.observation.addListener(new DocumentSaveEvent(filter), this);
        this.observation.addListener(new DocumentDeleteEvent(filter), this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Invalidates cached properties depending on the changed documents.
     * </p>
     * 
     * @see EventListener#onEvent(Event, Object, Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        // - if doc is a cached bundle
        // -- if is default language
        // --- invalidate all the document's bundles
        // -- else if translation is cached
        // --- invalidate the translated bundle
        DocumentModelBridge doc = (DocumentModelBridge) source;
        DocumentModelBridge oldDoc = doc.getOriginalDocument();
        if (oldDoc == null || doc.getContent().equals(oldDoc.getContent())) {
            // Since translations are kept inside the document content, then if the content didn't change, there's no
            // need to invalidate the cached properties.
            return;
        }
        String docName = doc.getWikiName() + ":" + doc.getFullName();
        synchronized (this.documentBundles) {
            if (this.documentBundles.containsKey(docName)) {
                if (doc.getRealLanguage().equals(getDefaultWikiLanguage(doc.getWikiName()))) {
                    // Default language, invalidate all document bundles
                    this.documentBundles.remove(docName);
                } else if (this.documentBundles.get(docName).containsKey(doc.getRealLanguage())) {
                    // Invalidate just the translation
                    this.documentBundles.get(docName).remove(doc.getRealLanguage());
                }
            }
        }
    }
}
