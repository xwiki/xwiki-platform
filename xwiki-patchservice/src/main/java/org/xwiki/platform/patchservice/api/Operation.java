package org.xwiki.platform.patchservice.api;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * An <tt>Operation</tt> is the basic unit of change affecting a document. Currently there are 5
 * types of possible changes:
 * <ul>
 * <li>changes affecting the document content;</li>
 * <li>changes affecting the document properties (author, date, language...);</li>
 * <li>changes affecting the Class properties (or property definitions);</li>
 * <li>changes affecting the object properties;</li>
 * <li>changes affecting the attachments.</li>
 * </ul>
 * <p>
 * Editing and saving a new version of a document is equivalent to applying several operations. For
 * example, between two consecutive document versions there can be several content operations
 * (delete a continuous portion of text, insert new text at a certain position, insert new text at
 * another position) and several document properties operations (change the author, change the
 * modification date).
 * </p>
 * <p>
 * This interface describes a read-only operation, one that can only be applied on a document.
 * {@link RWOperation} describes a read-write operation, one that can be programatically
 * created/defined.
 * </p>
 * 
 * @see RWOperation
 * @version $Id: $
 */
public interface Operation
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
    String TYPE_CLASS_PROPERTY_ADD = "class-prop-add";

    /**
     * Operation affecting the contained Class: Change a property definition.
     */
    String TYPE_CLASS_PROPERTY_CHANGE = "class-prop-change";

    /**
     * Operation affecting the contained Class: Delete a property definition.
     */
    String TYPE_CLASS_PROPERTY_DELETE = "class-prop-delete";

    /**
     * Operation affecting an attached object: Add a new object.
     */
    String TYPE_OBJECT_ADD = "object-add";

    /**
     * Operation affecting an attached object: Delete an existing object.
     */
    String TYPE_DBJECT_DELETE = "object-delete";

    /**
     * Operation affecting an attached object: Change the value for an object property.
     */
    String TYPE_OBJECT_PROPERTY_SET = "object-prop-set";

    /**
     * Operation affecting an attached object: Insert some text at a certain position inside a
     * multiline object property.
     */
    String TYPE_OBJECT_PROPERTY_INSERT_AT = "object-prop-textinsert";

    /**
     * Operation affecting an attached object: Delete some text from a certain position from inside
     * a multiline object property.
     */
    String TYPE_OBJECT_PROPERTY_DELETE_AT = "object-prop-textdelete";

    /**
     * Operation affecting the document attachments: Add a new attachment.
     */
    String TYPE_ATTACHMENT_ADD = "attachment-add";

    /**
     * Operation affecting the document attachments: Set a new content (version) for an existing
     * attachment.
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
     * @return A string identifying the operation type, one of the constant values defined in this
     *         class, or <tt>null</tt> if this operation was not properly initialized.
     */
    String getType();

    /* Actions */
    /**
     * Serialize this operation as a DOM Element that can be inserted in the given DOM Document.
     * 
     * @param doc A DOM Document used for generating a compatible Element. The document is not
     *            changed, as the constructed Element is just returned, not inserted in the
     *            document.
     * @return The operation exported as a DOM Element.
     * @throws XWikiException If the Operation is not well defined.
     */
    Element toXml(Document doc) throws XWikiException;

    /**
     * Load the operation from an XML.
     * 
     * @param e A DOM Element defining the Operation.
     * @throws XWikiException If the provided element is not a valid (or compatible) exported
     *             Operation.
     */
    void fromXml(Element e) throws XWikiException;

    /**
     * Apply this operation on a document.
     * 
     * @param doc The document being patched.
     * @throws XWikiException If the patch cannot be applied, either because the operation is not
     *             well defined, or is not compatible with the document version (a conflict was
     *             detected).
     */
    void apply(XWikiDocument doc) throws XWikiException;
}
