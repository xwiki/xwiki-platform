/*
 * 
 * ===================================================================
 *
 * Copyright (c) 2005 Jens Krämer, All rights reserved.
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
 *
 *
 * Created on 28.01.2005
 *
 */
package net.jkraemer.xwiki.plugins.lucene;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.time.FastDateFormat;
import org.apache.log4j.Logger;

/**
 * Contains constants naming the Lucene index fields used by this Plugin and
 * some helper methods for proper handling of special field values like dates.
 * @author <a href="mailto:jk@jkraemer.net">Jens Krämer </a>
 */
public abstract class IndexFields
{

    /**
     * Keyword field, holds a string uniquely identifying a document across the
     * index. this is used for finding old versions of a document to be indexed.
     */
    public static final String          DOCUMENT_ID           = "_docid";
    /** Keyword field, holds the name of the virtual wiki a document belongs to */
    public static final String          DOCUMENT_WIKI         = "wiki";
    /** Name of the document */
    public static final String          DOCUMENT_NAME         = "name";
    /** Name of the web the document belongs to */
    public static final String          DOCUMENT_WEB          = "web";
    /** Language of the document */
    public static final String          DOCUMENT_LANGUAGE     = "lang";
    /**
     * Type of a document, "attachment" or "wikipage", used to control
     * presentation of searchresults. See {@link SearchResult}and
     * xdocs/searchResult.vm.
     */
    public static final String          DOCUMENT_TYPE         = "type";

    /** Filename, only used for attachments */
    public static final String          FILENAME              = "filename";
    /** Last modifier */
    public static final String          DOCUMENT_AUTHOR       = "author";
    /** Creator of the document */
    public static final String          DOCUMENT_CREATOR      = "creator";
    /** Date of last modification */
    public static final String          DOCUMENT_DATE         = "date";
    /** Date of creation */
    public static final String          DOCUMENT_CREATIONDATE = "creationdate";
    /**
     * Fulltext content, not stored (and can therefore not be restored from the
     * index).
     */
    public static final String          FULLTEXT              = "ft";
    /** not in use */
    public static final String          KEYWORDS              = "kw";
    /**
     * Format for date storage in the index, and therefore the format which has
     * to be used for date-queries.
     */
    public static final String          DATE_FORMAT           = "yyyyMMddHHmm";

    private static final FastDateFormat df                    = FastDateFormat
                                                                      .getInstance (IndexFields.DATE_FORMAT);
    private static final Logger         LOG                   = Logger.getLogger (IndexFields.class);

    public static final String dateToString (Date date)
    {
        return df.format (date);
    }

    public static final Date stringToDate (String dateValue)
    {
        SimpleDateFormat sdf = new SimpleDateFormat (DATE_FORMAT);
        try
        {
            return sdf.parse (dateValue);
        } catch (Exception e)
        {
            // silently ignore
        }
        return null;
    }

    /**
     * 
     */
    private IndexFields ()
    {
        super ();
        // TODO Auto-generated constructor stub
    }

}
