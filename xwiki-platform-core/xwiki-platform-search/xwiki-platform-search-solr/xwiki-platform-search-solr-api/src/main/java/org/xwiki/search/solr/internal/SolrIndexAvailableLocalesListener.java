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
package org.xwiki.search.solr.internal;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.xwiki.bridge.event.AbstractDocumentEvent;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.RegexEventFilter;
import org.xwiki.search.solr.internal.api.FieldUtils;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.search.solr.internal.api.SolrInstance;

import com.xpn.xwiki.XWikiContext;

/**
 * Update already indexed entries when new available locales are added.
 * 
 * @version $Id$
 * @since 5.1RC1
 */
@Component
@Named("solr.availablelocales")
@Singleton
public class SolrIndexAvailableLocalesListener implements EventListener
{
    /**
     * The regex used to match preferences documents.
     */
    private static final String PREFERENCEDOCUMENT_REGEX = ".*:XWiki.XWikiPreferences";

    /**
     * The events to listen to that trigger the index update.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new DocumentUpdatedEvent(new RegexEventFilter(
        PREFERENCEDOCUMENT_REGEX)), new DocumentUpdatedEvent(new RegexEventFilter(PREFERENCEDOCUMENT_REGEX)),
        new ApplicationReadyEvent(), new WikiReadyEvent());

    /**
     * The currently available locales for each running wiki.
     */
    private Map<String, Set<Locale>> localesCache = new ConcurrentHashMap<String, Set<Locale>>();

    /**
     * Logging framework.
     */
    @Inject
    private Logger logger;

    /**
     * Provider for the {@link SolrInstance} that allows communication with the Solr server.
     */
    @Inject
    private Provider<SolrInstance> solrInstanceProvider;

    /**
     * The solr index.
     * <p>
     * Lazily initialize the {@link SolrIndexer} to not initialize it too early.
     */
    @Inject
    private Provider<SolrIndexer> solrIndexer;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return this.getClass().getName();
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiContext xcontext = (XWikiContext) data;

        String wiki = xcontext.getDatabase();

        Set<Locale> oldLocales = this.localesCache.get(wiki);
        List<Locale> availableLocales = xcontext.getWiki().getAvailableLocales(xcontext);

        // Update the cache
        this.localesCache.put(wiki, new HashSet<Locale>(availableLocales));

        try {
            if (event instanceof AbstractDocumentEvent) {
                Collection<Locale> newLocales = CollectionUtils.subtract(availableLocales, oldLocales);

                if (!newLocales.isEmpty()) {
                    StringBuilder builder = new StringBuilder();

                    for (Locale newLocale : newLocales) {
                        for (Locale locale : getParentLocales(newLocale)) {
                            if (builder.length() > 0) {
                                builder.append(" OR ");
                            }
                            builder.append(FieldUtils.DOCUMENT_LOCALE);
                            builder.append(':');
                            builder.append('"');
                            builder.append(locale.toString());
                            builder.append('"');
                        }
                    }

                    SolrQuery solrQuery = new SolrQuery(builder.toString());
                    solrQuery.setFields(FieldUtils.NAME, FieldUtils.SPACE, FieldUtils.WIKI, FieldUtils.DOCUMENT_LOCALE);

                    // TODO: be nicer with the memory when there is a lot of indexed documents and do smaller batches or
                    // stream the results
                    QueryResponse response = this.solrInstanceProvider.get().query(solrQuery);

                    SolrDocumentList results = response.getResults();
                    for (SolrDocument solrDocument : results) {
                        DocumentReference reference =
                            createDocumentReference((String) solrDocument.get(FieldUtils.WIKI),
                                (String) solrDocument.get(FieldUtils.SPACE),
                                (String) solrDocument.get(FieldUtils.NAME),
                                (String) solrDocument.get(FieldUtils.DOCUMENT_LOCALE));

                        this.solrIndexer.get().index(reference, true);
                    }
                }
            }
        } catch (Exception e) {
            this.logger.error("Failed to handle event [{}] with source [{}]", event, source, e);
        }
    }

    /**
     * @param locale the locale
     * @return the parents of the locale
     */
    private Set<Locale> getParentLocales(Locale locale)
    {
        Set<Locale> parentLocales = new HashSet<Locale>(LocaleUtils.localeLookupList(locale, Locale.ROOT));

        parentLocales.remove(locale);

        return parentLocales;
    }

    /**
     * @param wiki the wiki part of the reference
     * @param space the space part of the reference
     * @param name the name part of the reference
     * @param localeString the locale part of the reference as String
     * @return the complete document reference
     */
    private DocumentReference createDocumentReference(String wiki, String space, String name, String localeString)
    {
        if (localeString == null || localeString.isEmpty()) {
            return new DocumentReference(wiki, space, name);
        } else {
            return new DocumentReference(wiki, space, name, LocaleUtils.toLocale(localeString));
        }
    }
}
