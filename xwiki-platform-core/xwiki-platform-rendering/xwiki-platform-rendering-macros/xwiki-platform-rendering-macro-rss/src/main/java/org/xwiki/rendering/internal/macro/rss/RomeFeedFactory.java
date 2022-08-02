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

import org.xwiki.rendering.macro.rss.RssMacroParameters;
import org.xwiki.rendering.macro.MacroExecutionException;
import com.sun.syndication.feed.synd.SyndFeed;

/**
 * Auxiliary class which takes care of extracting the data from a RSS feed and providing it to the RSS macro.
 *
 * @version $Id$
 * @since 2.0M2
 */
public interface RomeFeedFactory
{
    /**
     * @param parameters the Rss macro's parameters needed for getting the data
     * @return the feed's data 
     * @throws MacroExecutionException in case the feed cannot be read
     */
    SyndFeed createFeed(RssMacroParameters parameters) throws MacroExecutionException;
}
