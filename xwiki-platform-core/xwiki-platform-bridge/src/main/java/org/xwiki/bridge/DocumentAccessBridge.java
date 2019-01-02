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

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.stability.Unstable;

/**
 * Exposes methods for accessing Document data. This is temporary until we remodel the Model classes and the Document
 * services.
 * 
 * @version $Id$
 * @since 1.6M1
 */
@Role
public interface DocumentAccessBridge
{
    /**
     * Find the document reference corresponding to the entity reference based on what exist in the database (page
     * reference can means two different documents for example).
     * 
     * @param entityReference the reference to resolve
     * @return the document reference
     * @since 10.6RC1
     */
    @Unstable
    default DocumentReference getDocumentReference(EntityReference entityReference)
    {
        return new DocumentReference(entityReference.extractReference(EntityType.DOCUMENT));
    }

    /**
     * Get the document object associated with the passed document name and context language.
     * <p>
     * Note that the returned document does not contain objects and attachment so it should be used very carefully.
     * 
     * @param documentReference the String reference of the document to find
     * @return the document object matching the passed document name
     * @throws Exception when the storage cannot be accessed
     * @deprecated use {@link #getTranslatedDocumentInstance(DocumentReference)} instead
     */
    @Deprecated
    DocumentModelBridge getDocument(String documentReference) throws Exception;

    /**
     * Get the document object associated with the passed document name and context language.
     * <p>
     * Note that the returned document does not contain objects and attachment so it should be used very carefully.
     * 
     * @param documentReference the name of the document to find
     * @return the document object matching the passed document name
     * @throws Exception when the storage cannot be accessed
     * @since 2.2M1
     * @deprecated deprecated since 10.2RC1, use {@link #getTranslatedDocumentInstance(DocumentReference)} instead
     */
    @Deprecated
    DocumentModelBridge getDocument(DocumentReference documentReference) throws Exception;

    /**
     * Get the document object associated with the passed document.
     * 
     * @param documentReference the reference of the document instance to find
     * @return the document instance matching the passed document reference
     * @throws Exception when loading the document failed
     * @since 10.2
     * @since 9.11.4
     */
    default DocumentModelBridge getDocumentInstance(DocumentReference documentReference) throws Exception
    {
        return null;
    }

    /**
     * Get the document object associated with the passed reference.
     * 
     * @param reference the direct or indicate reference of the document instance to find
     * @return the document instance matching the passed reference
     * @throws Exception when loading the document failed
     * @since 10.6RC1
     */
    default DocumentModelBridge getDocumentInstance(EntityReference reference) throws Exception
    {
        return null;
    }

    /**
     * Get the document object associated with the passed document name and context locale.
     * <p>
     * Note that the returned document does not contain objects and attachment so it should be used very carefully.
     * 
     * @param documentReference the reference of the document instance to find
     * @return the document instance matching the passed document reference and context locale
     * @throws Exception when loading the document failed
     * @since 10.2
     * @since 9.11.4
     */
    default DocumentModelBridge getTranslatedDocumentInstance(DocumentReference documentReference) throws Exception
    {
        return getDocument(documentReference);
    }

    /**
     * Get the document object associated with the passed entity reference and context locale.
     * <p>
     * Note that the returned document does not contain objects and attachment so it should be used very carefully.
     * 
     * @param entityReference the reference of the entity instance to find
     * @return the document instance matching the passed document reference and context locale
     * @throws Exception when loading the document failed
     * @since 10.6RC1
     */
    default DocumentModelBridge getTranslatedDocumentInstance(EntityReference entityReference) throws Exception
    {
        return getTranslatedDocumentInstance(new DocumentReference(entityReference));
    }

    /**
     * Get the reference to the current document (found in the Context).
     * 
     * @return the reference to the current document
     * @since 2.2M1
     */
    DocumentReference getCurrentDocumentReference();

    /**
     * Check if a document exists or not in the wiki.
     * 
     * @param documentReference The reference of the document to check.
     * @return <code>true</code> if the document already exists, <code>false</code> otherwise.
     * @since 2.2.1
     */
    boolean exists(DocumentReference documentReference);

    /**
     * Check if a document exists or not in the wiki.
     * 
     * @param documentReference The reference of the document to check.
     * @return <code>true</code> if the document already exists, <code>false</code> otherwise.
     * @deprecated replaced by {@link #exists(DocumentReference)} since 2.2.1
     */
    @Deprecated
    boolean exists(String documentReference);

    /**
     * Updates the target document with the new content provided. If the target document does not exists, a new one will
     * be created.
     * 
     * @param documentReference the reference to the target document
     * @param content Content to be set.
     * @param editComment Comment describing this particular change.
     * @param isMinorEdit Flag indicating if this change is a minor one.
     * @throws Exception if the storage cannot be accessed.
     * @since 2.2.1
     */
    void setDocumentContent(DocumentReference documentReference, String content, String editComment,
        boolean isMinorEdit) throws Exception;

    /**
     * Updates the target document with the new content provided. If the target document does not exists, a new one will
     * be created.
     * 
     * @param documentReference the reference to the target document
     * @param content Content to be set.
     * @param editComment Comment describing this particular change.
     * @param isMinorEdit Flag indicating if this change is a minor one.
     * @throws Exception if the storage cannot be accessed.
     * @deprecated replaced by {@link #setDocumentContent(DocumentReference, String, String, boolean)} since 2.2.1
     */
    @Deprecated
    void setDocumentContent(String documentReference, String content, String editComment, boolean isMinorEdit)
        throws Exception;

    /**
     * Retrieves the textual content of the document, in the current language.
     * 
     * @param documentReference the reference of the document to access
     * @return The document's content.
     * @throws Exception If the document cannot be accessed.
     * @deprecated replaced by {@link #getDocument(DocumentReference)} and {@link DocumentModelBridge#getContent()}
     *             since 2.2.1
     */
    @Deprecated
    String getDocumentContent(String documentReference) throws Exception;

    /**
     * Get the syntax Id of the target document. If the target document does not exists, the default syntax of a new
     * document is returned.
     * 
     * @param documentReference the reference of the target document
     * @return the syntax id.
     * @throws Exception If the storage cannot be accessed.
     * @deprecated replaced by {@link #getDocument(DocumentReference)} and {@link DocumentModelBridge#getSyntaxId()}
     *             since 2.2.1
     */
    @Deprecated
    String getDocumentSyntaxId(String documentReference) throws Exception;

    /**
     * Changes the syntax Id of the target document to the given syntaxId. If the target document does not exists, a new
     * one will be created.
     * 
     * @param documentReference the reference of the target document
     * @param syntaxId New syntax Id.
     * @throws Exception If the storage cannot be accessed.
     * @since 2.2.1
     */
    void setDocumentSyntaxId(DocumentReference documentReference, String syntaxId) throws Exception;

    /**
     * Changes the syntax Id of the target document to the given syntaxId. If the target document does not exists, a new
     * one will be created.
     * 
     * @param documentReference the reference of the target document
     * @param syntaxId New syntax Id.
     * @throws Exception If the storage cannot be accessed.
     * @deprecated replaced by {@link #setDocumentSyntaxId(DocumentReference, String)} since 2.2.1
     */
    @Deprecated
    void setDocumentSyntaxId(String documentReference, String syntaxId) throws Exception;

    /**
     * Sets the parent document name attribute for this document.
     * 
     * @param documentReference the reference of the target document
     * @param parentReference name of the parent document.
     * @throws Exception If the storage cannot be accessed.
     * @since 2.2
     */
    void setDocumentParentReference(DocumentReference documentReference, DocumentReference parentReference)
        throws Exception;

    /**
     * Sets the title of this document.
     * 
     * @param documentReference the reference of the target document
     * @param title the title to be set.
     * @throws Exception If the storage cannot be accessed.
     * @since 2.2
     */
    void setDocumentTitle(DocumentReference documentReference, String title) throws Exception;

    /**
     * Retrieves the textual content of the document, in the document's default language.
     * <p>
     * Note: you should always use {@link #getDocumentContent(String)} unless you really need specifically the
     * document's content for default language of the document.
     * 
     * @param documentReference the reference of the document to access
     * @return The document's content.
     * @throws Exception If the document cannot be accessed.
     * @since 2.2.1
     */
    String getDocumentContentForDefaultLanguage(DocumentReference documentReference) throws Exception;

    /**
     * Retrieves the textual content of the document, in the document's default language.
     * <p>
     * Note: you should always use {@link #getDocumentContent(String)} unless you really need specifically the
     * document's content for default language of the document.
     * 
     * @param documentReference the reference of the document to access
     * @return The document's content.
     * @throws Exception If the document cannot be accessed.
     * @deprecated replaced by {@link #getDocumentContentForDefaultLanguage(DocumentReference)} since 2.2.1
     */
    @Deprecated
    String getDocumentContentForDefaultLanguage(String documentReference) throws Exception;

    /**
     * Retrieves the textual content of the document, in the given language.
     * 
     * @param documentReference the referenc of the document to access
     * @param language The desired translation of the document.
     * @return The document's content.
     * @throws Exception If the document cannot be accessed.
     * @since 2.2.1
     */
    String getDocumentContent(DocumentReference documentReference, String language) throws Exception;

    /**
     * Retrieves the textual content of the document, in the given language.
     * 
     * @param documentReference the referenc of the document to access
     * @param language The desired translation of the document.
     * @return The document's content.
     * @throws Exception If the document cannot be accessed.
     * @deprecated replaced by {@link #getDocumentContent(DocumentReference, String)} since 2.2.1
     */
    @Deprecated
    String getDocumentContent(String documentReference, String language) throws Exception;

    /**
     * Get the number of the first object that has a property that match the expectation.
     * 
     * @param documentReference the reference of the document to look for objects into
     * @param classReference the reference of the class to look objects of
     * @param parameterName the name of the parameter to check the value for
     * @param valueToMatch the value to match for this parameter
     * @return the number of the first matching object, or -1 if none found
     */
    int getObjectNumber(DocumentReference documentReference, DocumentReference classReference, String parameterName,
        String valueToMatch);

    /**
     * Retrieves the value for an object property.
     * 
     * @param documentReference the reference of the document to access
     * @param className The name of the class.
     * @param objectNumber The number of the object from the given class.
     * @param propertyName The name of the property to retrieve.
     * @return the property value or null if it doesn't exist or an error occurred while looking for the property (the
     *         document doesn't exist for example)
     */
    Object getProperty(String documentReference, String className, int objectNumber, String propertyName);

    /**
     * Retrieves the value for an object property, from the first object of the given class.
     * 
     * @param documentReference the reference of the document to access
     * @param className The name of the class.
     * @param propertyName The name of the property to retrieve.
     * @return the property value or null if it doesn't exist or an error occurred while looking for the property (the
     *         document doesn't exist for example)
     * @deprecated since 2.2M1 use {@link #getProperty(DocumentReference, DocumentReference, String)} instead
     */
    @Deprecated
    Object getProperty(String documentReference, String className, String propertyName);

    /**
     * Retrieves the value for an object property.
     * 
     * @param objectReference the reference of the object to access
     * @param propertyName The name of the property to retrieve.
     * @return the property value or null if it doesn't exist or an error occurred while looking for the property (the
     *         document doesn't exist for example)
     * @since 3.2M3
     */
    Object getProperty(ObjectReference objectReference, String propertyName);

    /**
     * Retrieves the value for an object property.
     * 
     * @param objectPropertyReference the reference of the property to access
     * @return the property value or null if it doesn't exist or an error occurred while looking for the property (the
     *         document doesn't exist for example)
     * @since 3.2M3
     */
    Object getProperty(ObjectPropertyReference objectPropertyReference);

    /**
     * Retrieves the value for an object property, from the first object of the given class.
     * 
     * @param documentReference the reference of the document to access
     * @param classReference the reference to the XWiki Class
     * @param propertyName The name of the property to retrieve.
     * @return the property value or null if it doesn't exist or an error occurred while looking for the property (the
     *         document doesn't exist for example)
     * @since 2.2M1
     */
    Object getProperty(DocumentReference documentReference, DocumentReference classReference, String propertyName);

    /**
     * Retrieves the value for an object property, from the Nth object of the given class.
     * 
     * @param documentReference the reference of the document to access
     * @param classReference the reference to the XWiki Class
     * @param objectNumber the number of the object to get the property for
     * @param propertyName The name of the property to retrieve.
     * @return the property value or null if it doesn't exist or an error occurred while looking for the property (the
     *         document doesn't exist for example)
     * @since 3.2M3
     */
    Object getProperty(DocumentReference documentReference, DocumentReference classReference, int objectNumber,
        String propertyName);

    /**
     * Retrieves the value for an object property, from the first object of any class that has a property with that
     * name.
     * 
     * @param documentReference the reference of the document to access
     * @param propertyName The name of the property to retrieve.
     * @return the property value or null if it doesn't exist or an error occurred while looking for the property (the
     *         document doesn't exist for example)
     */
    Object getProperty(String documentReference, String propertyName);

    /**
     * @param documentReference the reference of the document to access
     * @param className the name of the class in the passed document from which to get the properties
     * @return the list of properties available in the passed document and class names
     */
    List<Object> getProperties(String documentReference, String className);

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
     * @param documentReference the reference of the document to access
     * @param className name of the class.
     * @param propertyName name of the property to set.
     * @param propertyValue value of the property to set.
     * @throws Exception if the document cannot be accessed.
     * @deprecated use {@link DocumentAccessBridge#setProperty(DocumentReference, DocumentReference, String, Object)}
     */
    @Deprecated
    void setProperty(String documentReference, String className, String propertyName, Object propertyValue)
        throws Exception;

    /**
     * Sets the given property of the first object (of the given class) attached to the document. If no such object
     * exists, this method will create a new object of the given class, attach it to the document and set the property.
     * 
     * @param documentReference the reference of the document to access
     * @param classReference the reference of the class.
     * @param propertyName name of the property to set.
     * @param propertyValue value of the property to set.
     * @throws Exception if the document cannot be accessed.
     */
    void setProperty(DocumentReference documentReference, DocumentReference classReference, String propertyName,
        Object propertyValue) throws Exception;

    /**
     * Returns the content of a document attachment.
     * 
     * @param documentReference the reference of the document to access
     * @param attachmentName The filename of the attachment to access.
     * @return The content of the attachment, as an array of <code>byte</code>s, which is empty if the attachment does
     *         not exist.
     * @throws Exception If the document cannot be accessed.
     * @deprecated use {@link #getAttachmentContent(org.xwiki.model.reference.AttachmentReference)} instead
     */
    @Deprecated
    byte[] getAttachmentContent(String documentReference, String attachmentName) throws Exception;

    /**
     * Returns the content of a document attachment.
     * 
     * @param attachmentReference the name of the attachment to access
     * @return The content of the attachment as an input stream
     * @throws Exception If the document cannot be accessed.
     * @since 2.2M1
     */
    InputStream getAttachmentContent(AttachmentReference attachmentReference) throws Exception;

    /**
     * Sets the content of a document attachment. If the document or the attachment does not exist, both will be created
     * newly.
     * 
     * @param attachmentReference the name of the attachment to access
     * @param attachmentData Attachment content.
     * @throws Exception If the storage cannot be accessed.
     * @since 2.2.1
     */
    void setAttachmentContent(AttachmentReference attachmentReference, byte[] attachmentData) throws Exception;

    /**
     * Sets the content of a document attachment. If the document or the attachment does not exist, both will be created
     * newly.
     * 
     * @param documentReference the reference to the target document name
     * @param attachmentFilename the name of the attachment
     * @param attachmentData Attachment content.
     * @throws Exception If the storage cannot be accessed.
     * @deprecated replaced by {@link #setAttachmentContent(AttachmentReference, byte[])} since 2.2.1
     */
    @Deprecated
    void setAttachmentContent(String documentReference, String attachmentFilename, byte[] attachmentData)
        throws Exception;

    /**
     * Returns the current version of a document attachment.
     * 
     * @param attachmentReference identifies the attachment to access
     * @return the current version of the specified attachment, {@code null} if the attachment does not exist
     * @throws Exception if the document cannot be accessed
     * @since 2.5M2
     */
    String getAttachmentVersion(AttachmentReference attachmentReference) throws Exception;

    /**
     * Retrieves the internal (without the hostname) URL that can be used to access a document, using a specific action.
     * 
     * @param documentReference the reference of the document to access
     * @param action The "mode" in which the document is accessed, for example <code>view</code> to view the document,
     *            <code>edit</code> to open the document for modifications, etc.
     * @param queryString An optional query string to append to the URL, use <code>null</code> or an empty string to
     *            skip.
     * @param anchor An optional URL fragment to append to the URL, use <code>null</code> or an empty string to skip.
     * @return A <code>String</code> representation of the URL, starting with the path segment of the URL (without
     *         protocol, host and port), for example <code>/xwiki/bin/save/Main/WebHome?content=abc</code>.
     * @since 2.2.1
     */
    String getDocumentURL(DocumentReference documentReference, String action, String queryString, String anchor);

    /**
     * Retrieves the internal (without the hostname) URL that can be used to access a document, using a specific action.
     * 
     * @param entityReference the reference of the entity to access
     * @param action The "mode" in which the document is accessed, for example <code>view</code> to view the document,
     *            <code>edit</code> to open the document for modifications, etc.
     * @param queryString An optional query string to append to the URL, use <code>null</code> or an empty string to
     *            skip.
     * @param anchor An optional URL fragment to append to the URL, use <code>null</code> or an empty string to skip.
     * @return A <code>String</code> representation of the URL, starting with the path segment of the URL (without
     *         protocol, host and port), for example <code>/xwiki/bin/save/Main/WebHome?content=abc</code>.
     * @since 10.6RC1
     */
    default String getDocumentURL(EntityReference entityReference, String action, String queryString, String anchor)
    {
        return getDocumentURL(entityReference, action, queryString, anchor, false);
    }

    /**
     * Retrieves the relative (without the hostname) or absolute (with the hostname) URL that can be used to access a
     * document, using a specific action.
     * 
     * @param documentReference the reference of the document to access
     * @param action The "mode" in which the document is accessed, for example <code>view</code> to view the document,
     *            <code>edit</code> to open the document for modifications, etc.
     * @param queryString An optional query string to append to the URL, use <code>null</code> or an empty string to
     *            skip.
     * @param anchor An optional URL fragment to append to the URL, use <code>null</code> or an empty string to skip.
     * @param isFullURL if true then the URL will be an absolute URL which contains the host name, and protocol.
     * @return A <code>String</code> representation of the URL, starting with the path segment of the URL (without
     *         protocol, host and port), for example <code>/xwiki/bin/save/Main/WebHome?content=abc</code>.
     * @since 2.5M1
     */
    String getDocumentURL(DocumentReference documentReference, String action, String queryString, String anchor,
        boolean isFullURL);

    /**
     * Retrieves the relative (without the hostname) or absolute (with the hostname) URL that can be used to access a
     * document, using a specific action.
     * 
     * @param entityReference the reference of the entity to access
     * @param action The "mode" in which the document is accessed, for example <code>view</code> to view the document,
     *            <code>edit</code> to open the document for modifications, etc.
     * @param queryString An optional query string to append to the URL, use <code>null</code> or an empty string to
     *            skip.
     * @param anchor An optional URL fragment to append to the URL, use <code>null</code> or an empty string to skip.
     * @param isFullURL if true then the URL will be an absolute URL which contains the host name, and protocol.
     * @return A <code>String</code> representation of the URL, starting with the path segment of the URL (without
     *         protocol, host and port), for example <code>/xwiki/bin/save/Main/WebHome?content=abc</code>.
     * @since 10.6RC1
     */
    default String getDocumentURL(EntityReference entityReference, String action, String queryString, String anchor,
        boolean isFullURL)
    {
        return getDocumentURL(entityReference.extractReference(EntityType.DOCUMENT), action, queryString, anchor,
            isFullURL);
    }

    /**
     * Retrieves the internal (without the hostname) URL that can be used to access a document, using a specific action.
     * 
     * @param documentReference the reference of the document to access
     * @param action The "mode" in which the document is accessed, for example <code>view</code> to view the document,
     *            <code>edit</code> to open the document for modifications, etc.
     * @param queryString An optional query string to append to the URL, use <code>null</code> or an empty string to
     *            skip.
     * @param anchor An optional URL fragment to append to the URL, use <code>null</code> or an empty string to skip.
     * @return A <code>String</code> representation of the URL, starting with the path segment of the URL (without
     *         protocol, host and port), for example <code>/xwiki/bin/save/Main/WebHome?content=abc</code>.
     * @deprecated replaced by {@link #getDocumentURL(DocumentReference, String, String, String)} since 2.2.1
     */
    @Deprecated
    String getURL(String documentReference, String action, String queryString, String anchor);

    /**
     * Retrieves all attachments in the passed document.
     * 
     * @param documentReference the reference to the document for which to retrieve all attachment references
     * @return the list of attachment names in the passed document
     * @throws Exception in case of a storage issue finding all attachments for the document matching the passed name
     * @since 2.2M1
     */
    List<AttachmentReference> getAttachmentReferences(DocumentReference documentReference) throws Exception;

    /**
     * Retrieves the relative URL (ie the path without the hostname and port) that can be used to access an attachment.
     * 
     * @param documentReference the reference to the document containing the attachment (eg "wiki:Space.Page")
     * @param attachmentFilename the attachment name (eg "my.png")
     * @return the attachment URL
     * @deprecated use {@link #getAttachmentURL(org.xwiki.model.reference.AttachmentReference , boolean)} instead
     */
    @Deprecated
    String getAttachmentURL(String documentReference, String attachmentFilename);

    /**
     * Retrieves the URL (either relative ie the path without the hostname and port, or the full URL) that can be used
     * to access an attachment.
     * 
     * @param attachmentReference the attachment name for which to find the URL
     * @param isFullURL whether the returned URL will a relative URL or the full URL
     * @return the attachment URL
     * @since 2.2M1
     */
    String getAttachmentURL(AttachmentReference attachmentReference, boolean isFullURL);

    /**
     * Retrieves the URL (either relative ie the path without the hostname and port, or the full URL) that can be used
     * to access an attachment.
     *
     * @param attachmentReference the attachment name for which to find the URL
     * @param queryString An optional query string to append to the URL, use <code>null</code> or an empty string to
     *            skip.
     * @param isFullURL whether the returned URL will a relative URL or the full URL
     * @return the attachment URL
     * @since 2.5RC1
     */
    String getAttachmentURL(AttachmentReference attachmentReference, String queryString, boolean isFullURL);

    /**
     * @param documentReference the document for which to retrieve all attachment URLs
     * @param isFullURL whether the returned URL will a relative URL or the full URL
     * @return the list of attachment URLs (either relative ie the path without the hostname and port, or the full URL)
     *         for all attachments in the passed document
     * @throws Exception in case of a storage issue finding all attachments for the document matching the passed name
     * @deprecated use {@link #getAttachmentReferences(org.xwiki.model.reference.DocumentReference)} instead
     * @since 2.2M1
     */
    @Deprecated
    List<String> getAttachmentURLs(DocumentReference documentReference, boolean isFullURL) throws Exception;

    /**
     * @param documentReference the reference of the document to access
     * @return true if current user can view provided document.
     * @since 2.2.1
     * @deprecated since 6.1, use
     *             {@link org.xwiki.security.authorization.ContextualAuthorizationManager#checkAccess(org.xwiki.security.authorization.Right, org.xwiki.model.reference.EntityReference)}
     *             with {@link org.xwiki.security.authorization.Right#VIEW} instead
     */
    @Deprecated
    boolean isDocumentViewable(DocumentReference documentReference);

    /**
     * @param documentReference the reference of the document to access
     * @return true if current user can view provided document.
     * @deprecated use {@link #isDocumentViewable(org.xwiki.model.reference.DocumentReference)} instead
     */
    @Deprecated
    boolean isDocumentViewable(String documentReference);

    /**
     * @param documentReference the reference of the document to be edited
     * @return True if current user has 'edit' access on the target document.
     * @deprecated use {@link #isDocumentEditable(org.xwiki.model.reference.DocumentReference)} instead
     */
    @Deprecated
    boolean isDocumentEditable(String documentReference);

    /**
     * @param documentReference the name of the document to be edited.
     * @return True if current user has 'edit' access on the target document.
     * @since 2.2M1
     * @deprecated since 6.1, use
     *             {@link org.xwiki.security.authorization.ContextualAuthorizationManager#checkAccess(org.xwiki.security.authorization.Right, org.xwiki.model.reference.EntityReference)}
     *             with {@link org.xwiki.security.authorization.Right#EDIT} instead
     */
    @Deprecated
    boolean isDocumentEditable(DocumentReference documentReference);

    /**
     * @return true if the current document's author has programming rights.
     * @deprecated since 6.1RC1, use
     *             {@link org.xwiki.security.authorization.ContextualAuthorizationManager#hasAccess(org.xwiki.security.authorization.Right)}
     *             instead
     */
    @Deprecated
    boolean hasProgrammingRights();

    /**
     * Utility method to retrieve the current user.
     * 
     * @return the current user full reference.
     * @deprecated replaced by {@link org.xwiki.bridge.DocumentAccessBridge#getCurrentUserReference()} since 4.0RC1
     */
    @Deprecated
    String getCurrentUser();

    /**
     * Utility method to retrieve the current user document reference.
     *
     * @return the current user document reference.
     * @since 4.0RC1
     */
    DocumentReference getCurrentUserReference();

    /**
     * @return true if current user is an advanced user
     * @since 9.2RC1
     */
    default boolean isAdvancedUser()
    {
        return true;
    }

    /**
     * @param userReference the reference of the user
     * @return true if passed user is an advanced user
     * @since 9.2RC1
     */
    default boolean isAdvancedUser(EntityReference userReference)
    {
        return true;
    }

    /**
     * Utility method to set the current user.
     * 
     * @param userName the current user
     * @since 2.4M2
     */
    void setCurrentUser(String userName);

    /**
     * @return The default encoding for the current wiki.
     */
    String getDefaultEncoding();

    /**
     * Sets the passed document as the current document in the XWiki Context and saves current values related to the
     * current document into a backup object.
     * 
     * @param backupObjects the object in which to some context properties will be saved
     * @param documentReference the reference to the document to set as the current document
     * @throws Exception in case of an error like a problem loading the document from the database
     * @deprecated use {@link #pushDocumentInContext(Map, DocumentReference)} instead
     */
    @Deprecated
    void pushDocumentInContext(Map<String, Object> backupObjects, String documentReference) throws Exception;

    /**
     * Sets the passed document as the current document in the XWiki Context and saves current values related to the
     * current document into a backup object.
     * 
     * @param backupObjects the object in which to some context properties will be saved
     * @param documentReference the reference to the document to set as the current document
     * @throws Exception in case of an error like a problem loading the document from the database
     * @since 2.2.1
     */
    void pushDocumentInContext(Map<String, Object> backupObjects, DocumentReference documentReference) throws Exception;

    /**
     * Sets the passed document as the current document in the XWiki Context and saves current values related to the
     * current document into a backup object.
     * 
     * @param backupObjects the object in which to some context properties will be saved
     * @param document the document to set as the current document
     * @throws Exception in case of an error like a problem loading the document from the database
     * @since 8.4M1
     */
    default void pushDocumentInContext(Map<String, Object> backupObjects, DocumentModelBridge document) throws Exception
    {
        pushDocumentInContext(backupObjects, document.getDocumentReference());
    }

    /**
     * Restore values saved in a backup object in the XWiki Context and restore the current document with the same value
     * before {@link #pushDocumentInContext(Map, String)} was called.
     * 
     * @param backupObjects the object containing the backed-up context properties to restore
     */
    void popDocumentFromContext(Map<String, Object> backupObjects);

    /**
     * @return the current wiki
     * @deprecated replaced by {@link org.xwiki.model.ModelContext#getCurrentEntityReference()} since 2.2M1
     */
    @Deprecated
    String getCurrentWiki();

    /**
     * @return the author of the current document.
     * @since 10.10RC1
     * @since 10.8.2
     * @since 9.11.9
     */
    @Unstable
    default DocumentReference getCurrentAuthorReference()
    {
        return null;
    }
}
