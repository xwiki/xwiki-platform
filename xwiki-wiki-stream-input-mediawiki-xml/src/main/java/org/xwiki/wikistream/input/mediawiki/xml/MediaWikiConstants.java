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
package org.xwiki.wikistream.input.mediawiki.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @version $Id: 5c213c4c836ba7a506c7fae073a3c2eee28e20be $
 */
public class MediaWikiConstants
{
    public final static String PAGE_TAG = "page";

    public final static String PAGE_TITLE_TAG = "title";

    public final static String PAGE_REVISION_TAG = "revision";

    public final static String AUTHOR_TAG = "username";

    public final static String TIMESTAMP_TAG = "timestamp";

    public final static String IS_MINOR_TAG = "minor";

    public final static String VERSION_TAG = "version";

    public final static String COMMENT_TAG = "comment";

    public final static String TEXT_CONTENT_TAG = "text";

    public final static List<String> MW_PROPERTIES = new ArrayList<String>(Arrays.asList(PAGE_TITLE_TAG, AUTHOR_TAG,
        COMMENT_TAG, IS_MINOR_TAG, TIMESTAMP_TAG));

    public static String convertPageName(String mediaWikiPageName)
    {
        String xwikiPageName = mediaWikiPageName.replaceAll("[.:\\\\]", "");

        // In MediaWiki the first character can have any case
        xwikiPageName = xwikiPageName.substring(0, 1).toUpperCase() + xwikiPageName.substring(1);

        return xwikiPageName;
    }

}
