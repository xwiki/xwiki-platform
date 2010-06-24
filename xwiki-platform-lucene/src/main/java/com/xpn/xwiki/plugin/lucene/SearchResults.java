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
package com.xpn.xwiki.plugin.lucene;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.search.Hits;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.XWiki;

/**
 * Container for the results of a search.
 * <p>
 * This class handles paging through search results and enforces the xwiki rights management by only returning search
 * results the user executing the search is allowed to view.
 * </p>
 * 
 * @version $Id$
 */
public class SearchResults extends Api
{
    private final XWiki xwiki;

    private final Hits hits;

    private static final Log LOG = LogFactory.getLog(SearchResults.class);

    private List<SearchResult> relevantResults;

    /**
     * @param hits Lucene search results
     * @param xwiki xwiki instance for access rights checking
     */
    public SearchResults(Hits hits, XWiki xwiki, XWikiContext context)
    {
        super(context);

        this.hits = hits;
        this.xwiki = xwiki;
    }

    private List<SearchResult> getRelevantResults()
    {
        if (this.relevantResults == null) {
            this.relevantResults = new ArrayList<SearchResult>();
            final int hitcount = this.hits.length();

            for (int i = 0; i < hitcount; i++) {
                try {
                    SearchResult result = new SearchResult(this.hits.doc(i), this.hits.score(i), this.xwiki);

                    String pageName = null;
                    if (result.isWikiContent()) {
                        pageName = result.getWiki() + ":" + result.getSpace() + "." + result.getName();

                        if (this.xwiki.exists(pageName)
                            && this.xwiki.hasAccessLevel("view", this.context.getUser(), pageName)) {
                            this.relevantResults.add(result);
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Error getting search result", e);
                }
            }
        }

        return this.relevantResults;
    }

    /**
     * @return true when there are more results than currently displayed.
     */
    public boolean hasNext(String beginIndex, String items)
    {
        final int itemCount = Integer.parseInt(items);
        final int begin = Integer.parseInt(beginIndex);

        return begin + itemCount - 1 < getRelevantResults().size();
    }

    /**
     * @return true when there is a page before the one currently displayed, that is, when <code>beginIndex > 1</code>
     */
    public boolean hasPrevious(String beginIndex)
    {
        return Integer.parseInt(beginIndex) > 1;
    }

    /**
     * @return the value to be used for the firstIndex URL parameter to build a link pointing to the next page of
     *         results
     */
    public int getNextIndex(String beginIndex, String items)
    {
        final int itemCount = Integer.parseInt(items);
        final int resultcount = getRelevantResults().size();
        int retval = Integer.parseInt(beginIndex) + itemCount;

        return retval > resultcount ? (resultcount - itemCount + 1) : retval;
    }

    /**
     * @return the value to be used for the firstIndex URL parameter to build a link pointing to the previous page of
     *         results
     */
    public int getPreviousIndex(String beginIndex, String items)
    {
        int retval = Integer.parseInt(beginIndex) - Integer.parseInt(items);

        return 0 < retval ? retval : 1;
    }

    /**
     * @return the index of the last displayed search result
     */
    public int getEndIndex(String beginIndex, String items)
    {
        int retval = Integer.parseInt(beginIndex) + Integer.parseInt(items) - 1;
        final int resultcount = getRelevantResults().size();
        if (retval > resultcount) {
            return resultcount;
        }

        return retval;
    }

    /**
     * Helper method for use in velocity templates, takes string values instead of ints. See
     * {@link #getResults(int,int)}.
     */
    public List<SearchResult> getResults(String beginIndex, String items)
    {
        return getResults(Integer.parseInt(beginIndex), Integer.parseInt(items));
    }

    /**
     * Returns a list of search results. According to beginIndex and endIndex, only a subset of the results is returned.
     * To get the first ten results, one would use beginIndex=1 and items=10.
     * 
     * @param beginIndex 1-based index of first result to return.
     * @param items number of items to return
     * @return List of SearchResult instances starting at <code>beginIndex</code> and containing up to
     *         <code>items</code> elements.
     */
    public List<SearchResult> getResults(int beginIndex, int items)
    {
        final int listStartIndex = beginIndex - 1;
        final int listEndIndex = listStartIndex + items;
        int resultcount = 0;
        List<SearchResult> relResults = this.relevantResults;
        if (relResults == null) {
            relResults = new ArrayList<SearchResult>();
            final int hitcount = this.hits.length();

            String database = this.context.getDatabase();
            try {
                for (int i = 0; i < hitcount; i++) {
                    SearchResult result = null;
                    try {
                        result = new SearchResult(this.hits.doc(i), this.hits.score(i), this.xwiki);

                        this.context.setDatabase(result.getWiki());

                        String pageName = null;
                        if (result.isWikiContent()) {
                            pageName = result.getWiki() + ":" + result.getSpace() + "." + result.getName();
                        }
                        if (result.isWikiContent() && this.xwiki.exists(pageName)
                            && this.xwiki.checkAccess(pageName, "view")) {
                            if (resultcount >= listStartIndex) {
                                relResults.add(result);
                            }
                            resultcount++;
                            if (resultcount == listEndIndex)
                                return relResults;
                        }
                    } catch (Exception e) {
                        LOG.error("error getting search result", e);
                    }
                }
            } finally {
                this.context.setDatabase(database);
            }

            return relResults;
        } else {
            resultcount = getRelevantResults().size();

            return getRelevantResults()
                .subList(listStartIndex, listEndIndex < resultcount ? listEndIndex : resultcount);
        }
    }

    /**
     * @return all search results in one list.
     */
    public List<SearchResult> getResults()
    {
        return getRelevantResults();
    }

    /**
     * @return total number of searchresults the user is allowed to view
     */
    public int getHitcount()
    {
        return getRelevantResults().size();
    }

    /**
     * @return total number of searchresults including unallowed items
     */
    public int getTotalHitcount()
    {
        return this.hits.length();
    }
}
