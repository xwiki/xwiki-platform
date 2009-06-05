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

package org.xwiki.rendering.internal.macro.rss;

import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.rss.RssMacroParameters;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * Auxiliary class which takes care of extracting the data from a RSS feed and 
 * providing it to the Rss macro.
 * 
 * @version $Id: $
 * @since 1.9
 */
public class FeedReader 
{
    /**
     * The maximum number of seconds to wait when inquiring the RSS feed provider.
     */
    protected static final int TIMEOUT_SECONDS = 5; 

    /**
     * Unique ID for Class Serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The actual feed from which the data needs to be read from.
     */
    private SyndFeed feed;

    /**
     * @param parameters the Rss macro's parameters needed for getting the data
     * @throws MacroExecutionException in case the feed cannot be read
     */
    public FeedReader(RssMacroParameters parameters) throws MacroExecutionException 
    {
        if (StringUtils.isEmpty(parameters.getFeed())) {
            throw new MacroExecutionException(RssMacro.PARAMETER_MISSING_ERROR);
        }
        
        SyndFeedInput input = new SyndFeedInput();
        
        try {
            URLConnection connection = parameters.getFeedURL().openConnection();
            connection.setConnectTimeout(TIMEOUT_SECONDS * 1000);
            feed = input.build(new XmlReader(connection));
        } catch (SocketTimeoutException ex) {
            throw new MacroExecutionException(RssMacro.CONNECTION_TIMEOUT_ERROR + parameters.getFeedURL());
        } catch (Exception ex) {
            throw new MacroExecutionException("Error processing " 
                + parameters.getFeedURL() + ": " + ex.getMessage(), ex);
        }
        if (feed == null) { 
            throw new MacroExecutionException(RssMacro.INVALID_DOCUMENT_ERROR + parameters.getFeedURL());
        }
    }
    
    /**
     * @return the feed's image URL
     */
    public String getImageURL() {
        return feed.getImage().getUrl();
    }

    /**
     * @return whether the feed has an image or not
     */
    public boolean hasImage() {
        return feed.getImage() != null;
    }
    
    /**
     * @return the feed's link
     */
    public String getLink() {
        return feed.getLink();
    }

    /**
     * @return the feed's title
     */
    public String getTitle() {
        return feed.getTitle();
    }

    /**
     * @return a list containing the feed's entries
     */
    @SuppressWarnings("unchecked")
    public List getEntries()
    {
        return feed.getEntries();
    }
}
