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
package org.xwiki.activeinstalls2.script;

import java.util.List;
import java.util.SequencedMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.activeinstalls2.DataManager;
import org.xwiki.activeinstalls2.TooManyExtensionsException;
import org.xwiki.activeinstalls2.internal.data.Ping;
import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Provides Scripting APIs for the Active Installs module.
 *
 * @version $Id$
 * @since 5.2M2
 */
@Component
@Named("activeinstalls2")
@Singleton
public class ActiveInstallsScriptService implements ScriptService
{
    /**
     * Used to retrieve the data.
     */
    @Inject
    private DataManager dataManager;

    /**
     * Executes a Count query for Active Installs. Note that this counts matching pings and not matching instances,
     * despite its name: an instance sends a ping every day but also every time it's restarted, and thus matches
     * several pings. Use {@link #countDistinctInstalls(String)} to count instances.
     *
     * @param jsonQuery the Elastic Search JSON query used to search for installs.
     * @return the count number
     * @throws Exception when an error happens while retrieving the data
     * @since 14.4RC1
     */
    public long countInstalls(String jsonQuery) throws Exception
    {
        return this.dataManager.countInstalls(jsonQuery);
    }

    /**
     * Executes a Search query for Active Installs.
     *
     * @param jsonQuery the Elastic Search JSON query used to search for installs. For example:
     *        <pre>{@code
     *            {
     *                "term": { "distribution.extension.version" : "5.2" }
     *            }
     *        }</pre>
     * @return the parsed JSON result coming from Elastic Search, as a list of {@link Ping} object.
     * @throws Exception when an error happens while retrieving the data
     * @since 14.4RC1
     */
    public List<Ping> searchInstalls(String jsonQuery) throws Exception
    {
        return this.dataManager.searchInstalls(jsonQuery);
    }

    /**
     * Counts the distinct XWiki instances having sent a matching ping. Contrary to {@link #countInstalls(String)},
     * which counts pings, this counts installs: an instance sends a ping every day but also every time it's
     * restarted, and thus matches several pings. For example:
     * <pre>{@code
     *     {
     *         "range": { "date.current": { "gte": "now-1d" } }
     *     }
     * }</pre>
     *
     * @param jsonQuery the Elastic Search JSON query used to search for installs
     * @return the number of distinct instances. With the default implementation, this is expected to be accurate up
     *      to 40000 distinct instances, and an approximation above that
     * @throws Exception when an error happens while retrieving the data
     * @see DataManager#countDistinctInstalls(String)
     * @since 18.8.0RC1
     */
    @Unstable
    public long countDistinctInstalls(String jsonQuery) throws Exception
    {
        return this.dataManager.countDistinctInstalls(jsonQuery);
    }

    /**
     * Counts, for each extension, the distinct XWiki instances having that extension installed and having sent a
     * matching ping. This is computed with a single query, and is thus much cheaper than calling
     * {@link #countDistinctInstalls(String)} once per extension. See
     * {@link DataManager#countDistinctInstallsByExtension(String)} for how the counts are keyed and for why a query
     * on the extensions doesn't restrict the extensions being counted.
     *
     * @param jsonQuery the Elastic Search JSON query used to search for installs
     * @return the number of distinct instances, keyed by extension id, ordered by descending number of matching
     *      pings (which is close to, but not exactly, ordering by the returned counts). With the default
     *      implementation, each count is expected to be accurate up to 40000 distinct instances, and an
     *      approximation above that
     * @throws TooManyExtensionsException when there are more extensions than can be returned in a single query. Note
     *      that this is a subclass of the {@link Exception} below, and is thus not listed separately in the throws
     *      clause, where it would be redundant
     * @throws Exception when an error happens while retrieving the data
     * @see DataManager#countDistinctInstallsByExtension(String)
     * @since 18.8.0RC1
     */
    @Unstable
    public SequencedMap<String, Long> countDistinctInstallsByExtension(String jsonQuery) throws Exception
    {
        return this.dataManager.countDistinctInstallsByExtension(jsonQuery);
    }
}
