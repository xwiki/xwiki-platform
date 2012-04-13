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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;

/**
 * Contains constants naming the Lucene index fields used by this Plugin and some helper methods for proper handling of
 * special field values like dates.
 * 
 * @version $Id$
 */
public abstract class IndexFields
{
    /**
     * Keyword field, holds a string uniquely identifying a document across the index. this is used for finding old
     * versions of a document to be indexed.
     */
    public static final String DOCUMENT_ID = "_docid";

    /**
     * Keyword field, holds the name of the virtual wiki a document belongs to
     */
    public static final String DOCUMENT_WIKI = "wiki";

    /**
     * Title of the document
     */
    public static final String DOCUMENT_TITLE = "title";

    /**
     * Name of the document
     */
    public static final String DOCUMENT_NAME = "name";

    /**
     * Name of the web the document belongs to
     */
    @Deprecated
    public static final String DOCUMENT_WEB = "web";

    /**
     * Name of the space the document belongs to
     */
    public static final String DOCUMENT_SPACE = "space";

    /**
     * FullName of the document (example : Main.WebHome)
     */
    public static final String DOCUMENT_FULLNAME = "fullname";

    /**
     * Version of the document
     */
    public static final String DOCUMENT_VERSION = "version";
    
    /**
     * Language of the document
     */
    public static final String DOCUMENT_LANGUAGE = "lang";

    /**
     * Type of a document, "attachment", "wikipage" or "objects", used to control presentation of searchresults. See
     * {@link SearchResult}and xdocs/searchResult.vm.
     */
    public static final String DOCUMENT_TYPE = "type";

    /**
     * Filename, only used for attachments
     */
    public static final String FILENAME = "filename";

    /**
     * XWiki object type, only used for objects
     */
    public static final String OBJECT = "object";

    /**
     * Last modifier
     */
    public static final String DOCUMENT_AUTHOR = "author";

    /**
     * Creator of the document
     */
    public static final String DOCUMENT_CREATOR = "creator";

    /**
     * Date of last modification
     */
    public static final String DOCUMENT_DATE = "date";

    /**
     * Date of creation
     */
    public static final String DOCUMENT_CREATIONDATE = "creationdate";

    /**
     * Document hidden flag.
     */
    public static final String DOCUMENT_HIDDEN = "hidden";

    /**
     * Fulltext content, not stored (and can therefore not be restored from the index).
     */
    public static final String FULLTEXT = "ft";

    /**
     * not in use
     */
    public static final String KEYWORDS = "kw";

    /**
     * Format for date storage in the index, and therefore the format which has to be used for date-queries.
     */
    public static final String DATE_FORMAT = "yyyyMMddHHmm";

    private static final FastDateFormat DF = FastDateFormat.getInstance(IndexFields.DATE_FORMAT);

    public static final String dateToString(Date date)
    {
        return DF.format(date);
    }

    public static final Date stringToDate(String dateValue)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        try {
            return sdf.parse(dateValue);
        } catch (Exception e) {
            // silently ignore
        }

        return null;
    }

    public static final boolean stringToBoolean(String booleanValue)
    {
        return Boolean.parseBoolean(booleanValue);
    }

    private IndexFields()
    {
    }
}
