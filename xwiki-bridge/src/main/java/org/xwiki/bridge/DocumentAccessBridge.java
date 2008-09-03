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
     * Check if a document exists or not in the wiki.
     * 
     * @param documentName The name of the document to check.
     * @return <code>true</code> if the document already exists, <code>false</code> otherwise.
     * @throws Exception If the storage cannot be accessed or the document name is invalid.
     */
    boolean exists(String documentName) throws Exception;

    /**
     * Retrieves the textual content of the document.
     * 
     * @param documentName The name of the document to access.
     * @return The document's content.
     * @throws Exception If the document cannot be accessed.
     */
    String getDocumentContent(String documentName) throws Exception;

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
     * @return class name of the property object or null if property is not found. For example StringProperty, IntegerProperty.
     * @throws Exception if class cannot be accessed
     */
    String getPropertyType(String className, String propertyName) throws Exception;

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
     * Retrieves the internal (without the hostname) URL that can be used to access a document, using a specific action,
     * 
     * @param documentName The name of the document to access.
     * @param action The "mode" in which the document is accessed, for example <code>view</code> to view the document,
     *            <code>edit</code> to open the document for modifications, etc.
     * @param queryString An optional query string to append to the URL, use <code>null</code> or an empty string to
     *            skip.
     * @param anchor An optional URL fragment to append to the URL, use <code>null</code> or an empty string to skip.
     * @return A <code>String</code> representation of the URL, starting with the path segment of the URL (without
     *         protocol, host and port), for example <code>/xwiki/bin/save/Main/WebHome?content=abc</code>.
     * @throws Exception If the document cannot be accessed.
     */
    String getURL(String documentName, String action, String queryString, String anchor) throws Exception;
}
