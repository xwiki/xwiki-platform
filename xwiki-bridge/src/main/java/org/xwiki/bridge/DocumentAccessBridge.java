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
package org.xwiki.bridge;

import java.util.Map;

/**
 * Exposes methods for accessing Document data. This is temporary until we remodel the Model classes and the Document
 * services.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public interface DocumentAccessBridge
{
    /**
     * This component's role, used when code needs to look it up.
     */
    String ROLE = DocumentAccessBridge.class.getName();

    /**
     * Get the document object associated with the passed document name.
     * 
     * @param documentName the name of the document to find
     * @return the document object matching the passed document name
     * @throws Exception when the storage cannot be accessed
     */
    DocumentModelBridge getDocument(String documentName) throws Exception;

    /**
     * Get the different parts of a document name (wiki, space, page).
     * 
     * @param documentName the name of the document for which to return name information
     * @return the document name object containing the information
     */
    DocumentName getDocumentName(String documentName);

    /**
     * Get the different parts of a the current document name (wiki, space, page).
     * <p>
     * The current document is found in the context.
     * 
     * @return the document name object containing the information
     * @since 1.8.3
     */
    DocumentName getCurrentDocumentName();

    /**
     * Check if a document exists or not in the wiki.
     * 
     * @param documentName The name of the document to check.
     * @return <code>true</code> if the document already exists, <code>false</code> otherwise.
     */
    boolean exists(String documentName);

    /**
     * Updates the target document with the new content provided. If the target document does not exists, a new one will
     * be created.
     * 
     * @param documentName Name of the target document.
     * @param content Content to be set.
     * @param editComment Comment describing this particular change.
     * @param isMinorEdit Flag indicating if this change is a minor one.
     * @throws Exception if the storage cannot be accessed.
     */
    void setDocumentContent(String documentName, String content, String editComment, boolean isMinorEdit)
        throws Exception;

    /**
     * Retrieves the textual content of the document, in the current language.
     * 
     * @param documentName The name of the document to access.
     * @return The document's content.
     * @throws Exception If the document cannot be accessed.
     */
    String getDocumentContent(String documentName) throws Exception;

    /**
     * Get the syntax Id of the target document. If the target document does not exists, the default syntax of a new
     * document is returned.
     * 
     * @param documentName Name of the target document.
     * @return the syntax id.
     * @throws Exception If the storage cannot be accessed.
     */
    String getDocumentSyntaxId(String documentName) throws Exception;

    /**
     * Changes the syntax Id of the target document to the given syntaxId. If the target document does not exists, a new
     * one will be created.
     * 
     * @param documentName Name of the target document.
     * @param syntaxId New syntax Id.
     * @throws Exception If the storage cannot be accessed.
     */
    void setDocumentSyntaxId(String documentName, String syntaxId) throws Exception;

    /**
     * Retrieves the textual content of the document, in the document's default language.
     * <p>
     * Note: you should always use {@link #getDocumentContent(String)} unless you really need specifically the
     * document's content for default language of the document.
     * 
     * @param documentName The name of the document to access.
     * @return The document's content.
     * @throws Exception If the document cannot be accessed.
     */
    String getDocumentContentForDefaultLanguage(String documentName) throws Exception;

    /**
     * Retrieves the textual content of the document, in the given language.
     * 
     * @param documentName The name of the document to access.
     * @param language The desired translation of the document.
     * @return The document's content.
     * @throws Exception If the document cannot be accessed.
     */
    String getDocumentContent(String documentName, String language) throws Exception;

    /**
     * Retrieves the value for an object property.
     * 
     * @param documentName The name of the document to access.
     * @param className The name of the class.
     * @param objectNumber The number of the object from the given class.
     * @param propertyName The name of the property to retrieve.
     * @return A <code>string</code> representation of the property value.
     * @throws Exception If the document cannot be accessed.
     */
    String getProperty(String documentName, String className, int objectNumber, String propertyName) throws Exception;

    /**
     * Retrieves the value for an object property, from the first object of the given class.
     * 
     * @param documentName The name of the document to access.
     * @param className The name of the class.
     * @param propertyName The name of the property to retrieve.
     * @return A <code>string</code> representation of the property value.
     * @throws Exception If the document cannot be accessed.
     */
    String getProperty(String documentName, String className, String propertyName) throws Exception;

    /**
     * Retrieves the value for an object property, from the first object of any class that has a property with that
     * name.
     * 
     * @param documentName The name of the document to access.
     * @param propertyName The name of the property to retrieve.
     * @return A <code>string</code> representation of the property value.
     * @throws Exception If the document cannot be accessed.
     */
    String getProperty(String documentName, String propertyName) throws Exception;

    /**
     * @param className The name of the class.
     * @param propertyName The name of the property.
     * @return class name of the property object or null if property is not found. For example StringProperty,
     *         IntegerProperty.
     * @throws Exception if class cannot be accessed
     */
    String getPropertyType(String className, String propertyName) throws Exception;

    /**
     * @param className The name of the class.
     * @param propertyName The name of the property of the class.
     * @return is the property stored in a special custom mapped class.
     * @throws Exception if class cannot be accessed
     */
    boolean isPropertyCustomMapped(String className, String propertyName) throws Exception;

    /**
     * Sets the given property of the first object (of the given class) attached to the document. If no such object
     * exists, this method will create a new object of the given class, attach it to the document and set the property.
     * 
     * @param documentName name of the document to access.
     * @param className name of the class.
     * @param propertyName name of the property to set.
     * @param propertyValue value of the property to set.
     * @throws Exception if the document cannot be accessed.
     */
    void setProperty(String documentName, String className, String propertyName, Object propertyValue) throws Exception;

    /**
     * Returns the content of a document attachment.
     * 
     * @param documentName The name of the document to access.
     * @param attachmentName The filename of the attachment to access.
     * @return The content of the attachment, as an array of <code>byte</code>s, which is empty if the attachment does
     *         not exist.
     * @throws Exception If the document cannot be accessed.
     */
    byte[] getAttachmentContent(String documentName, String attachmentName) throws Exception;

    /**
     * Sets the content of a document attachment. If the document or the attachment does not exist, both will be created
     * newly.
     * 
     * @param documentName Target document name.
     * @param attachmentName Name of the attachment.
     * @param attachmentData Attachment content.
     * @throws Exception If the storage cannot be accessed.
     */
    void setAttachmentContent(String documentName, String attachmentName, byte[] attachmentData) throws Exception;

    /**
     * Retrieves the internal (without the hostname) URL that can be used to access a document, using a specific action.
     * 
     * @param documentName The name of the document to access.
     * @param action The "mode" in which the document is accessed, for example <code>view</code> to view the document,
     *            <code>edit</code> to open the document for modifications, etc.
     * @param queryString An optional query string to append to the URL, use <code>null</code> or an empty string to
     *            skip.
     * @param anchor An optional URL fragment to append to the URL, use <code>null</code> or an empty string to skip.
     * @return A <code>String</code> representation of the URL, starting with the path segment of the URL (without
     *         protocol, host and port), for example <code>/xwiki/bin/save/Main/WebHome?content=abc</code>.
     */
    String getURL(String documentName, String action, String queryString, String anchor);

    /**
     * Retrieves the internal (without the hostname) URL that can be used to access an attachment.
     * 
     * @param documentName the full name of the document containing the attachment (eg "wiki:Space.Page")
     * @param attachmentName the attachment name (eg "my.png")
     * @return the attachment URL
     */
    String getAttachmentURL(String documentName, String attachmentName);

    /**
     * @param documentName the name of the document to access.
     * @return true if current user can view provided document.
     */
    boolean isDocumentViewable(String documentName);

    /**
     * @param documentName The name of the document to be edited.
     * @return True if current user has 'edit' access on the target document.
     */
    boolean isDocumentEditable(String documentName);

    /**
     * @return true if the current document's author has programming rights.
     */
    boolean hasProgrammingRights();

    /**
     * Utility method to retrieve the current user.
     * 
     * @return The current user.
     */
    String getCurrentUser();

    /**
     * @return The default encoding for the current wiki.
     */
    String getDefaultEncoding();

    /**
     * Sets the passed document as the current document in the XWiki Context and saves current values related to the
     * current document into a backup object.
     * 
     * @param backupObjects the object in which to some context properties will be saved
     * @param documentName the document to set as the current document
     * @throws Exception in case of an error like a problem loading the document from the database
     */
    void pushDocumentInContext(Map<String, Object> backupObjects, String documentName) throws Exception;

    /**
     * Restore values saved in a backup object in the XWiki Context and restore the current document with the same value
     * before {@link #pushDocumentInContext(Map, String)} was called.
     * 
     * @param backupObjects the object containing the backed-up context properties to restore
     */
    void popDocumentFromContext(Map<String, Object> backupObjects);
}
