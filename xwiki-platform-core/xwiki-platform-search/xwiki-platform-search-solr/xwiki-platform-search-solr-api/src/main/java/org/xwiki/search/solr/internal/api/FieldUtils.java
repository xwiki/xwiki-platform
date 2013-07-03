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

import java.util.Locale;

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
 * @since 5.1M2
 */
public final class FieldUtils
{
    /**
     * Keyword field, holds a string uniquely identifying a document across the index. this is used for finding old
     * versions of a document to be indexed.
     */
    public static final String ID = "id";

    /**
     * Language of the document.
     */
    public static final String LANGUAGE = "language";

    /**
     * The real/calculated locale of the document (the default locale in default document entry case).
     */
    public static final String LOCALE = "locale";

    /**
     * Technical locale of the document (emty for the default document entry). Not indexed, mostly used to find the
     * document in database.
     */
    public static final String DOCUMENT_LOCALE = "doclocale";

    /**
     * The list of Locales covered by this entity. Dynamically determined from the list of enabled Locales and the
     * various Locales of the associated wiki document.
     */
    public static final String LOCALES = "locales";

    /**
     * Keyword field, holds the name of the virtual wiki a document belongs to.
     */
    public static final String WIKI = "wiki";

    /**
     * Name of the space the document belongs to.
     */
    public static final String SPACE = "space";

    /**
     * Unanalyzed and not stored version of the document's space.
     */
    public static final String SPACE_EXACT = "space_exact";

    /**
     * Name of the document.
     */
    public static final String NAME = "name";

    /**
     * Unanalyzed and not stored version of the document's space.
     */
    public static final String NAME_EXACT = "name_exact";

    /**
     * FullName of the document (example: {@code Main.WebHome}).
     */
    public static final String FULLNAME = "fullname";

    /**
     * Title of the document.
     * <p/>
     * Note: Multilingual and virtual field.
     */
    public static final String TITLE = "title";

    /**
     * Lowercased, unanalyzed and not stored version of the document's title, used for sorting.
     */
    public static final String TITLE_SORT = "title_sort";

    /**
     * Version of the document (example: {@code 4.2}).
     */
    public static final String VERSION = "version";

    /**
     * For storing the comment associated to the version.
     * <p/>
     * Note: Multilingual and virtual field.
     */
    public static final String COMMENT = "comment";

    /**
     * Type of a document. "DOCUMENT", "ATTACHMENT", "OBJECT" and "OBJECT_PROPERTY" are the used values corresponding to
     * the values from {@link org.xwiki.model.EntityType}.
     */
    public static final String TYPE = "type";

    /**
     * XWiki object class, only used for objects and properties.
     */
    public static final String CLASS = "class";

    /**
     * XWiki object number, only used for objects and properties.
     */
    public static final String NUMBER = "number";

    /**
     * XWiki object content. Used by objects to index their properties and by documents to index all the properties of
     * the contained objects.
     * <p/>
     * Note: Multilingual and virtual field.
     */
    public static final String OBJECT_CONTENT = "objcontent";

    /**
     * Last modifier.
     */
    public static final String AUTHOR = "author";

    /**
     * Last modifier, in its display version (i.e., "first_name last_name").
     */
    public static final String AUTHOR_DISPLAY = "author_display";

    /**
     * Lowercased, unanalyzed and not stored version of the document's last author display version, used for sorting.
     */
    public static final String AUTHOR_DISPLAY_SORT = "author_display_sort";

    /**
     * Creator of the document.
     */
    public static final String CREATOR = "creator";

    /**
     * Creator of the document, in its display version (i.e., "first_name last_name").
     */
    public static final String CREATOR_DISPLAY = "creator_display";

    /**
     * Date of last modification.
     */
    public static final String DATE = "date";

    /**
     * Date of creation.
     */
    public static final String CREATIONDATE = "creationdate";

    /**
     * Document hidden flag.
     */
    public static final String HIDDEN = "hidden";

    /**
     * Document score, not an actual field. It's only computed at query time.
     */
    public static final String SCORE = "score";

    /**
     * Fulltext plain rendered content.
     * <p/>
     * Note: Multilingual and virtual field.
     */
    public static final String DOCUMENT_RENDERED_CONTENT = "doccontent";

    /**
     * Raw content.
     */
    public static final String DOCUMENT_RAW_CONTENT = "doccontentraw";

    /**
     * Attachment content.
     * <p/>
     * Note: Multilingual and virtual field.
     */
    public static final String ATTACHMENT_CONTENT = "attcontent";

    /**
     * Attachment version.
     */
    public static final String ATTACHMENT_VERSION = "attversion";

    /**
     * For storing mimetype of the attachments.
     */
    public static final String MIME_TYPE = "mimetype";

    /**
     * Filename, only used for attachments.
     */
    public static final String FILENAME = "filename";

    /**
     * For storing property name.
     */
    public static final String PROPERTY_NAME = "propertyname";

    /**
     * For storing property value.
     * <p/>
     * Note: Multilingual and virtual field.
     */
    public static final String PROPERTY_VALUE = "propertyvalue";

    /**
     * Underscore character, used to separate the field name from the suffix.
     */
    public static final String USCORE = "_";

    /**
     * Utility class.
     */
    private FieldUtils()
    {
    }

    /**
     * Format string for multilingual fields.
     * 
     * @param field the field name
     * @param locale the locale of the content of the field
     * @return the localized version of the field name
     */
    public static String getFieldName(String field, Locale locale)
    {
        StringBuilder builder = new StringBuilder(field);

        builder.append(USCORE);

        if (locale != null) {
            if (locale.equals(Locale.ROOT)) {
                builder.append(USCORE);
            } else {
                builder.append(locale);
            }
        }

        return builder.toString();
    }
}
