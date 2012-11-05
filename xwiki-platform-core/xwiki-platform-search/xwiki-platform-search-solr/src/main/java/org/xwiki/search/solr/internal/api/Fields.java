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
package org.xwiki.search.solr;


/**
 * Contains constants naming the Lucene index fields used by this Plugin and some helper methods for proper handling of
 * special field values like dates.
 * 
 * @version $Id$
 */
public interface Fields
{
    /**
     * Keyword field, holds a string uniquely identifying a document across the index. this is used for finding old
     * versions of a document to be indexed.
     */
    String ID = "id";

    /**
     * Language of the document.
     */
    String LANGUAGE = "lang";

    /**
     * Keyword field, holds the name of the virtual wiki a document belongs to.
     */
    String WIKI = "wiki";

    /**
     * Name of the space the document belongs to.
     */
    String SPACE = "space";

    /**
     * Name of the document.
     */
    String NAME = "name";

    /**
     * FullName of the document (example : Main.WebHome).
     */
    String FULLNAME = "fullname";

    /**
     * Title of the document.
     */
    String TITLE = "title";

    /**
     * Version of the document.
     */
    String VERSION = "version";

    /**
     * Type of a document. "DOCUMENT", "ATTACHMENT", "OBJECT" and "OBJECT_PROPERTY" are the used values corresponding to
     * the values from {@link org.xwiki.model.EntityType}.
     */
    String TYPE = "type";

    /**
     * XWiki object class, only used for objects and properties.
     */
    String CLASS = "class";

    /**
     * XWiki object content. Used by objects to index their properties and by documents to index all the properties of
     * the contained objects.
     */
    String OBJECT_CONTENT = "objcontent";

    /**
     * Last modifier.
     */
    String AUTHOR = "author";

    /**
     * Creator of the document.
     */
    String CREATOR = "creator";

    /**
     * Date of last modification.
     */
    String DATE = "date";

    /**
     * Date of creation.
     */
    String CREATIONDATE = "creationdate";

    /**
     * Document hidden flag.
     */
    String HIDDEN = "hidden";

    /**
     * Document score.
     */
    String SCORE = "score";

    /**
     * Fulltext content, not stored (and can therefore not be restored from the index).
     */
    String DOCUMENT_CONTENT = "doccontent";

    /**
     * Attachment content.
     */
    String ATTACHMENT_CONTENT = "attcontent";

    /**
     * not in use.
     */
    String KEYWORDS = "kw";

    /**
     * For storing mimetype of the attachments.
     */
    String MIME_TYPE = "mimetype";

    /**
     * Filename, only used for attachments.
     */
    String FILENAME = "filename";

    /**
     * For storing the doc reference . Used by attachments.
     */
    String DOC_REFERENCE = "docref";

    /**
     * For storing the comments.
     */
    String COMMENT = "comment";

    /**
     * For storing property name.
     */
    String PROPERTY_NAME = "propertyname";

    /**
     * For storing property value.
     */
    String PROPERTY_VALUE = "propertyvalue";
}
