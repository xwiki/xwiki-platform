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
package org.xwiki.wikistream.mediawiki.xml.input;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface MediaWikiConstants
{
    String PAGE_TAG = "page";

    String PAGE_TITLE_TAG = "title";

    String PAGE_REVISION_TAG = "revision";

    String AUTHOR_TAG = "username";

    String TIMESTAMP_TAG = "timestamp";

    String IS_MINOR_TAG = "minor";

    String VERSION_TAG = "version";

    String COMMENT_TAG = "comment";

    String TEXT_CONTENT_TAG = "text";

    List<String> MW_PROPERTIES = Arrays.asList(PAGE_TITLE_TAG, AUTHOR_TAG, COMMENT_TAG, IS_MINOR_TAG, TIMESTAMP_TAG);

    Map<String, String> EVENT_MAPPING = new HashMap<String, String>()
    {
        {
            put("page", "page");
            put("title", "title");
            put("revision", "revision");
            put("comment", "comment");
            put("username", "author");
        }
    };
}
