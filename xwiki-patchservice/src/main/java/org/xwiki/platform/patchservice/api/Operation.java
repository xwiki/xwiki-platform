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
 *
 */
package org.xwiki.platform.patchservice.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * <p>
 * An <tt>Operation</tt> is the basic unit of change affecting a document. Currently there are 5 types of possible
 * changes:
 * <ul>
 * <li>changes affecting the document content;</li>
 * <li>changes affecting the document properties (author, date, language...);</li>
 * <li>changes affecting the Class properties (or property definitions);</li>
 * <li>changes affecting the Object properties;</li>
 * <li>changes affecting the attachments.</li>
 * </ul>
 * </p>
 * <p>
 * Editing and saving a new version of a document is equivalent to applying several operations, grouped in a
 * {@link Patch}. For example, between two consecutive document versions there can be several content operations
 * (delete a continuous portion of text, insert new text at a certain position, insert new text at another position) and
 * several document properties operations (change the author, change the modification date).
 * </p>
 * <p>
 * This interface describes a read-only operation, one that can only be applied on a document. {@link RWOperation}
 * describes a read-write operation, one that can be programatically created/defined.
 * </p>
 * 
 * @see RWOperation
 * @version $Id$
 */
public interface Operation extends XmlSerializable
{
    /* Operation type flags */
    /**
     * Operation affecting the document content: Insert some text at a certain position.
     */
    String TYPE_CONTENT_INSERT = "content-insert";

    /**
     * Operation affecting the document content: Delete some text from a certain position.
     */
    String TYPE_CONTENT_DELETE = "content-delete";

    /**
     * Operation affecting the document properties: Set a new value for a document property.
     */
    String TYPE_PROPERTY_SET = "property-set";

    /**
     * Operation affecting the contained Class: Add a new property definition.
     */
    String TYPE_CLASS_PROPERTY_ADD = "class-property-add";

    /**
     * Operation affecting the contained Class: Change a property definition.
     */
    String TYPE_CLASS_PROPERTY_CHANGE = "class-property-change";

    /**
     * Operation affecting the contained Class: Delete a property definition.
     */
    String TYPE_CLASS_PROPERTY_DELETE = "class-property-delete";

    /**
     * Operation affecting an attached object: Add a new object.
     */
    String TYPE_OBJECT_ADD = "object-add";

    /**
     * Operation affecting an attached object: Delete an existing object.
     */
    String TYPE_OBJECT_DELETE = "object-delete";

    /**
     * Operation affecting an attached object: Change the value for an object property.
     */
    String TYPE_OBJECT_PROPERTY_SET = "object-property-set";

    /**
     * Operation affecting an attached object: Insert some text at a certain position inside a multiline object
     * property.
     */
    String TYPE_OBJECT_PROPERTY_INSERT_AT = "object-property-textinsert";

    /**
     * Operation affecting an attached object: Delete some text from a certain position from inside a multiline object
     * property.
     */
    String TYPE_OBJECT_PROPERTY_DELETE_AT = "object-property-textdelete";

    /**
     * Operation affecting the document attachments: Add a new attachment.
     */
    String TYPE_ATTACHMENT_ADD = "attachment-add";

    /**
     * Operation affecting the document attachments: Set a new content (version) for an existing attachment.
     */
    String TYPE_ATTACHMENT_SET = "attachment-set";

    /**
     * Operation affecting the document attachments: Delete an existing attachment.
     */
    String TYPE_ATTACHMENT_DELETE = "attachment-delete";

    /* Operation metadata */
    /**
     * Get the type of this operation.
     * 
     * @return A string identifying the operation type, one of the constant values defined in this class, or
     *         <tt>null</tt> if this operation was not properly initialized.
     */
    String getType();

    /* Actions */
    /**
     * Apply this operation on a document.
     * 
     * @param doc The document being patched.
     * @param context The XWiki context, needed for some document manipulations.
     * @throws XWikiException If the patch cannot be applied, either because the operation is not well defined, or is
     *             not compatible with the document version (a conflict was detected).
     */
    void apply(XWikiDocument doc, XWikiContext context) throws XWikiException;
}
