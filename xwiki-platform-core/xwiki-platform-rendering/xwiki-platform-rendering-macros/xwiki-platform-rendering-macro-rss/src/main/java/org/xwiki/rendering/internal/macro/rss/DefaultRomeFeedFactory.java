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

import javax.net.ssl.HttpsURLConnection;
import java.net.SocketTimeoutException;
import java.net.URLConnection;
import java.text.MessageFormat;

import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.rss.RssMacroParameters;
import org.apache.commons.lang3.StringUtils;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * Factory implementation using Rome to return the feed's data.
 *
 * @version $Id$
 * @since 1.9
 */
public class DefaultRomeFeedFactory implements RomeFeedFactory
{
    /**
     * The maximum number of milliseconds to wait when inquiring the RSS feed provider.
     */
    private static final int TIMEOUT_MILLISECONDS = 5000;
    private static final String USER_AGENT_HEADER = "User-Agent";
    private String version = this.getClass().getPackage().getImplementationVersion();
    private String userAgent = "XWiki/" + this.version;

    @Override
    public SyndFeed createFeed(RssMacroParameters parameters) throws MacroExecutionException
    {
        if (StringUtils.isEmpty(parameters.getFeed())) {
            throw new MacroExecutionException("The required 'feed' parameter is missing");
        }

        SyndFeedInput syndFeedInput = new SyndFeedInput();

        SyndFeed feed;
        try {
            if (StringUtils.startsWith(parameters.getFeed().toLowerCase(), "https")) {
                HttpsURLConnection httpsURLConnection = (HttpsURLConnection) parameters.getFeedURL().openConnection();
                httpsURLConnection.setConnectTimeout(TIMEOUT_MILLISECONDS);
                httpsURLConnection.setRequestProperty(USER_AGENT_HEADER, userAgent);
                feed = syndFeedInput.build(new XmlReader(httpsURLConnection));

            } else {
                URLConnection httpURLConnection = parameters.getFeedURL().openConnection();
                httpURLConnection.setConnectTimeout(TIMEOUT_MILLISECONDS);
                httpURLConnection.setRequestProperty(USER_AGENT_HEADER, userAgent);
                feed = syndFeedInput.build(new XmlReader(httpURLConnection));
            }
        } catch (SocketTimeoutException ex) {
            throw new MacroExecutionException(MessageFormat.format("Connection timeout when trying to reach [{0}]",
                    parameters.getFeedURL()));
        } catch (Exception ex) {
            throw new MacroExecutionException(MessageFormat.format("Error processing [{0}] : {1}", parameters
                    .getFeedURL(), ex.getMessage()), ex);
        }
        if (feed == null) {
            throw new MacroExecutionException(MessageFormat.format("No feed found at [{0}]",
                    parameters.getFeedURL()));
        }

        return feed;
    }
}
