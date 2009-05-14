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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.localization.Bundle;
import org.xwiki.localization.WikiInformation;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.DocumentUpdateEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.RegexEventFilter;

/**
 * Bundle corresponding to the pulled wiki documents. To pull a document, call
 * <code>$l10n.use("document", "Space.Name")</code>
 * 
 * @version $Id$
 */
@Component("document")
public class PulledDocumentsBundle extends AbstractWikiBundle implements Bundle, EventListener, Initializable
{
    /** The key used for placing the list of pulled document bundles in the current execution context. */
    public static final String PULLED_CONTEXT_KEY = PulledDocumentsBundle.class.getName() + "_bundles";

    /** Provides access to the request context. */
    @Requirement
    protected Execution execution;

    /**
     * {@inheritDoc}
     * <p>
     * Register an event listener for all wiki preferences documents, so that when the default language is changed, all
     * cached bundles from that wiki are invalidated.
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // Set the Bundle priority
        setPriority(100);
        
        this.observation.addListener(new DocumentUpdateEvent(new RegexEventFilter(".+:"
            + WikiInformation.PREFERENCES_DOCUMENT_NAME)), this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Bundle#getTranslation(String, String)
     */
    @SuppressWarnings("unchecked")
    public String getTranslation(String key, String language)
    {
        String translation = key;
        // The list of pulled documents is taken from the execution context.
        List<String> documentNames = (List<String>) this.execution.getContext().getProperty(PULLED_CONTEXT_KEY);
        if (documentNames != null) {
            Properties props;
            synchronized (documentNames) {
                for (String documentName : documentNames) {
                    try {
                        // The document names should contain the wiki prefix already.
                        props = getDocumentBundle(documentName, language);
                        if (props.containsKey(key)) {
                            translation = props.getProperty(key);
                            // The first translation found is returned.
                            break;
                        }
                    } catch (Exception ex) {
                        getLogger().warn("Cannot load document bundle: [{0}]", documentName);
                    }
                }
            }
        }
        return translation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Bundle#use(String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void use(String bundleLocation)
    {
        String documentName = bundleLocation;
        // Always put prefixed document names.
        if (documentName.indexOf(WikiInformation.WIKI_PREFIX_SEPARATOR) < 0) {
            // If the document didn't contain a wiki prefix, then use the current wiki of the execution.
            documentName = this.wikiInfo.getCurrentWikiName() + WikiInformation.WIKI_PREFIX_SEPARATOR + documentName;
        }
        // In theory, the execution context should not be shared by more than one thread. But just in case...
        synchronized (this.execution.getContext()) {
            List<String> documentNames = (List<String>) this.execution.getContext().getProperty(PULLED_CONTEXT_KEY);
            if (documentNames == null) {
                documentNames = new ArrayList<String>();
                this.execution.getContext().setProperty(PULLED_CONTEXT_KEY, documentNames);
            }
            synchronized (documentNames) {
                documentNames.add(documentName);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Invalidates cached bundles when the default language of a wiki changes.
     * </p>
     * 
     * @see EventListener#onEvent(Event, Object, Object)
     */
    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentModelBridge doc = (DocumentModelBridge) source;
        if (WikiInformation.PREFERENCES_DOCUMENT_NAME.equals(doc.getFullName())) {
            String wiki = doc.getWikiName();
            synchronized (this.defaultWikiLanguage) {
                if (this.defaultWikiLanguage.containsKey(wiki)) {
                    // setDefaultWikiLanguage returns true if the old value was different.
                    if (setDefaultWikiLanguage(wiki, this.wikiInfo.getDefaultWikiLanguage(wiki))) {
                        invalidateWikiBundles(wiki + WikiInformation.WIKI_PREFIX_SEPARATOR);
                    }
                }
            }
        }

        // Invalidate individual translations if needed.
        super.onEvent(event, source, data);
    }

    /**
     * Invalidate all cached document bundles from the given wiki. This is needed when the default language of a wiki
     * changes, since translated properties now have a different default to fall back to.
     * 
     * @param wiki The target wiki to invalidate.
     */
    protected void invalidateWikiBundles(String wiki)
    {
        synchronized (this.documentBundles) {
            Iterator<String> it = this.documentBundles.keySet().iterator();
            while (it.hasNext()) {
                if (it.next().startsWith(wiki)) {
                    it.remove();
                }
            }
        }
    }
}
