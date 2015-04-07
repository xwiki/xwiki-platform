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
package org.xwiki.watchlist.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.watchlist.internal.documents.WatchListClassDocumentInitializer;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation for {@link WatchListNotificationCache}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultWatchListNotificationCache implements WatchListNotificationCache, Initializable
{
    /**
     * The realtime interval ID.
     */
    public static final String REALTIME_INTERVAL_ID = "realtime";

    /**
     * Context provider.
     */
    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Used to list the existing wikis.
     */
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * Used to search for subscribers.
     */
    @Inject
    private QueryManager queryManager;

    /**
     * Logging helper object.
     */
    @Inject
    private Logger logger;

    /**
     * Map of subscribers in the wiki farm.
     */
    private Map<String, Set<String>> intervalToSubscribersMap = new HashMap<>();

    /**
     * Watchlist notification intervals.
     */
    private List<String> intervals;

    /**
     * Lock for the subscribers map.
     */
    private ReentrantReadWriteLock subscribersLock = new ReentrantReadWriteLock();

    /**
     * Lock for the intervals list.
     */
    private ReentrantReadWriteLock intervalsLock = new ReentrantReadWriteLock();

    /**
     * Used to access xwiki.properties.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource xwikiProperties;

    /**
     * Init watchlist store. Get all the intervals/jobs present in the wiki. Create the list of subscribers.
     * 
     * @throws InitializationException if problems occur
     */
    public void initialize() throws InitializationException
    {
        XWikiContext context = contextProvider.get();

        // Initialize the intervals cache.
        try {
            intervals = new ArrayList<String>();

            if ("true".equals(xwikiProperties.getProperty("watchlist.realtime.enabled"))) {
                // If the realtime notification feature is explicitly enabled (temporarily disabled by default), then
                // propose/use it as possible notification interval option.
                intervals.add(REALTIME_INTERVAL_ID);
            }

            // Get all the watchlist job documents from the main wiki.
            Query jobDocumentsQuery = queryManager.getNamedQuery("getWatchlistJobDocuments");
            // Make double sure we run the query on the main wiki, since that is where the jobs are defined.
            jobDocumentsQuery.setWiki(context.getWikiId());
            List<String> jobDocumentNames = (List<String>) (List) jobDocumentsQuery.execute();

            // TODO: Sort them by cron expression.

            // Add them to the list of intervals.
            intervals.addAll(jobDocumentNames);
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize the cache of watchlist intervals.", e);
        }

        // Initialize the subscribers cache.
        for (String jobDocumentName : intervals) {
            initSubscribersCache(jobDocumentName);
        }
    }

    /**
     * Retrieves all the users from all the wikis with a WatchList object in their profile.
     * 
     * @param intervalId name of the interval to init the cache for
     * @param context the XWiki context
     */
    private void initSubscribersCache(String intervalId)
    {
        // init subscribers cache
        List<Object> queryParams = new ArrayList<Object>();
        queryParams.add(WatchListClassDocumentInitializer.DOCUMENT_FULL_NAME);
        queryParams.add(intervalId);
        queryParams.add(DefaultWatchListStore.USERS_CLASS);

        Set<String> subscribersForJob =
            globalSearchDocuments(", BaseObject as obj, StringProperty as prop, BaseObject as userobj where"
                + " doc.fullName=obj.name and obj.className=? and obj.id=prop.id.id and prop.value=?"
                + " and doc.fullName=userobj.name and userobj.className=?", 0, 0, queryParams);

        subscribersLock.writeLock().lock();
        try {
            intervalToSubscribersMap.put(intervalId, subscribersForJob);
        } finally {
            subscribersLock.writeLock().unlock();
        }
    }

    /**
     * Search documents on all the wikis by passing HQL where clause values as parameters.
     * 
     * @param request The HQL where clause.
     * @param nb Number of results to retrieve
     * @param start Offset to use in the search query
     * @param values The where clause values that replaces the question marks (?)
     * @return a set of document names prefixed with the wiki they come from ex : xwiki:Main.WebHome
     */
    private Set<String> globalSearchDocuments(String request, int nb, int start, List<Object> values)
    {
        Collection<String> wikiServers = new ArrayList<String>();
        Set<String> results = new HashSet<>();

        try {
            wikiServers = wikiDescriptorManager.getAllIds();
        } catch (Exception e) {
            logger.error("Failed to get the list of wikis", e);
        }

        try {
            // Create the query and set the common parameters.
            Query query = queryManager.createQuery(request, Query.HQL);
            query.setOffset(start);
            query.setLimit(nb);
            query.bindValues(values);

            // Run on each wiki.
            for (String wiki : wikiServers) {
                String wikiPrefix = wiki + DefaultWatchListStore.WIKI_SPACE_SEP;
                try {
                    query.setWiki(wiki);
                    List<String> upDocsInWiki = query.execute();

                    // Prefix the results with the wiki ID.
                    Iterator<String> it = upDocsInWiki.iterator();
                    while (it.hasNext()) {
                        results.add(wikiPrefix + it.next());
                    }
                } catch (Exception e) {
                    logger.error("Failed to search in wiki [{}]", wiki, e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to create query", e);
        }

        return results;
    }

    /**
     * Destroy subscribers cache for the given job.
     * 
     * @param intervalId ID of the interval for which the cache must be destroyed
     * @param context the XWiki context
     */
    private void destroySubscribersCache(String intervalId)
    {
        subscribersLock.writeLock().lock();
        try {
            intervalToSubscribersMap.remove(intervalId);
        } finally {
            subscribersLock.writeLock().unlock();
        }
    }

    @Override
    public Collection<String> getSubscribers(String intervalId)
    {
        Set<String> result = null;

        subscribersLock.readLock().lock();
        try {
            result = intervalToSubscribersMap.get(intervalId);
        } finally {
            subscribersLock.readLock().unlock();
        }

        if (result == null) {
            return Collections.emptySet();
        } else {
            return new HashSet<>(result);
        }
    }

    @Override
    public boolean addSubscriber(String jobId, String user)
    {
        subscribersLock.writeLock().lock();
        try {
            Set<String> subscribersForJob = intervalToSubscribersMap.get(jobId);

            if (subscribersForJob != null) {
                return subscribersForJob.add(user);
            }

            return false;
        } finally {
            subscribersLock.writeLock().unlock();
        }
    }

    @Override
    public boolean moveSubscriber(String oldIntervalId, String newIntervalId, String user)
    {
        // Atomic operation.
        subscribersLock.writeLock().lock();
        try {
            // We do not really care if the remove is successful (i.e. valid interval or existed in the first place).
            removeSubscriber(oldIntervalId, user);

            // We mark the move operation success based on the add operation.
            return addSubscriber(newIntervalId, user);
        } finally {
            subscribersLock.writeLock().unlock();
        }
    }

    @Override
    public boolean removeSubscriber(String intervalId, String user)
    {
        subscribersLock.writeLock().lock();
        try {
            Set<String> subscribersForJob = intervalToSubscribersMap.get(intervalId);

            if (subscribersForJob != null) {
                return subscribersForJob.remove(user);
            }
        } finally {
            subscribersLock.writeLock().unlock();
        }

        return false;
    }

    @Override
    public List<String> getIntervals()
    {
        intervalsLock.readLock().lock();
        try {
            // Do not return the internal reference, but copy it.
            return new ArrayList<String>(intervals);
        } finally {
            intervalsLock.readLock().unlock();
        }
    }

    @Override
    public boolean removeInterval(String intervalId)
    {
        // Atomic operation
        intervalsLock.writeLock().lock();
        subscribersLock.writeLock().lock();
        try {
            if (intervals.remove(intervalId)) {
                destroySubscribersCache(intervalId);

                return true;
            }

            return false;
        } finally {
            subscribersLock.writeLock().unlock();
            intervalsLock.writeLock().unlock();
        }
    }

    @Override
    public boolean addInterval(String jobDocument)
    {
        // Atomic operation
        intervalsLock.writeLock().lock();
        subscribersLock.writeLock().lock();
        try {
            if (intervals.add(jobDocument)) {
                initSubscribersCache(jobDocument);

                // TODO: Re-sort the intervals by cron expression

                return true;
            }

            return false;
        } finally {
            subscribersLock.writeLock().unlock();
            intervalsLock.writeLock().unlock();
        }
    }
}
