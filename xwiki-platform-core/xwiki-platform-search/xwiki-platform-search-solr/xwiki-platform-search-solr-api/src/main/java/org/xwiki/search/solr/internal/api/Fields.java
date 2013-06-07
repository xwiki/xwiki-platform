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
package org.xwiki.search.solr.internal.api;

/**
 * Contains constants naming the Solr/Lucene index fields used by this module for indexing entities. Also contains
 * additional constants used for composing field names on multilingual fields.
 * <p/>
 * A multilingual and virtual/alias field, is not stored in the index with the specified name. It is only used at query
 * time and it is expanded automatically to the actual fields from the index.
 * <p/>
 * Example: "title" becomes "title_ | title_en | title_ro | title_fr | etc..."
 * <p/>
 * <b>Note</b>: When indexing a field, the actual field name must be used instead of the virtual field name.
 * 
 * @version $Id$
 * @since 4.3M2
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
    String LANGUAGE = "language";
    
    /**
     * Locale of the document.
     */
    String LOCALE = "locale";

    /**
     * Keyword field, holds the name of the virtual wiki a document belongs to.
     */
    String WIKI = "wiki";

    /**
     * Name of the space the document belongs to.
     */
    String SPACE = "space";

    /**
     * Unanalyzed and not stored version of the document's space.
     */
    String SPACE_EXACT = "space_exact";

    /**
     * Name of the document.
     */
    String NAME = "name";

    /**
     * FullName of the document (example: {@code Main.WebHome}).
     */
    String FULLNAME = "fullname";

    /**
     * Title of the document.
     * <p/>
     * Note: Multilingual and virtual field.
     */
    String TITLE = "title";

    /**
     * Lowercased, unanalyzed and not stored version of the document's title, used for sorting.
     */
    String TITLE_SORT = "title_sort";

    /**
     * Version of the document (example: {@code 4.2}).
     */
    String VERSION = "version";
    
    /**
     * For storing the comment associated to the version.
     * <p/>
     * Note: Multilingual and virtual field.
     */
    String COMMENT = "comment";

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
     * XWiki object number, only used for objects and properties.
     */
    String NUMBER = "number";

    /**
     * XWiki object content. Used by objects to index their properties and by documents to index all the properties of
     * the contained objects.
     * <p/>
     * Note: Multilingual and virtual field.
     */
    String OBJECT_CONTENT = "objcontent";

    /**
     * Last modifier.
     */
    String AUTHOR = "author";

    /**
     * Last modifier, in its display version (i.e., "first_name last_name").
     */
    String AUTHOR_DISPLAY = "author_display";

    /**
     * Lowercased, unanalyzed and not stored version of the document's last author display version, used for sorting.
     */
    String AUTHOR_DISPLAY_SORT = "author_display_sort";

    /**
     * Creator of the document.
     */
    String CREATOR = "creator";

    /**
     * Creator of the document, in its display version (i.e., "first_name last_name").
     */
    String CREATOR_DISPLAY = "creator_display";

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
     * Document score, not an actual field. It's only computed at query time.
     */
    String SCORE = "score";

    /**
     * Fulltext content, not stored (and can therefore not be restored from the index).
     * <p/>
     * Note: Multilingual and virtual field.
     */
    String DOCUMENT_CONTENT = "doccontent";

    /**
     * Attachment content.
     * <p/>
     * Note: Multilingual and virtual field.
     */
    String ATTACHMENT_CONTENT = "attcontent";

    /**
     * For storing mimetype of the attachments.
     */
    String MIME_TYPE = "mimetype";

    /**
     * Filename, only used for attachments.
     */
    String FILENAME = "filename";

    /**
     * For storing property name.
     */
    String PROPERTY_NAME = "propertyname";

    /**
     * For storing property value.
     * <p/>
     * Note: Multilingual and virtual field.
     */
    String PROPERTY_VALUE = "propertyvalue";

    /**
     * Underscore character, used to separate the field name from the suffix.
     */
    String USCORE = "_";

    /**
     * Multilingual field suffix for the field of "text_general" type.
     * <p>
     * Underscore to make sure to not collide with an existing locale or root locale.
     */
    String MULTILINGUAL = USCORE;

    /**
     * Format string for multilingual fields.
     */
    String MULTILIGNUAL_FORMAT = "%s_%s";
}
