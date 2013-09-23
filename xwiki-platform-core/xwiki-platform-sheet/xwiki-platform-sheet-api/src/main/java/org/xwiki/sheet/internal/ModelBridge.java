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
package org.xwiki.sheet.internal;

import java.util.Map;
import java.util.Set;

import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * Bridge between the sheet module and the XWiki model. This interface is used mainly to decouple the sheet module from
 * the old XWiki model so that it can be tested more easily.
 * <p>
 * NOTE: Keep this interface internal because it's not part of the public API exposed by the sheet module.
 * 
 * @version $Id$
 * @since 4.1M1
 */
@Role
public interface ModelBridge
{
    /**
     * This method is needed only to keep backward compatibility with older XWiki applications that are still using the
     * deprecated "inline" action. Newer applications should use the "edit" action instead.
     * 
     * @param document a document
     * @return the default edit mode for the given document, e.g. "edit", "inline"
     */
    String getDefaultEditMode(DocumentModelBridge document);

    /**
     * @param document a document
     * @return the default translation of the given document; if the given document is the default translation then it
     *         is returned as is
     */
    DocumentModelBridge getDefaultTranslation(DocumentModelBridge document);

    /**
     * @return the action performed on the current document, e.g. "view", "edit", "preview"
     */
    String getCurrentAction();

    /**
     * @param document a document
     * @return {@code true} if the given document has programming rights, {@code false} otherwise
     */
    boolean hasProgrammingRights(DocumentModelBridge document);

    /**
     * Sets the content author of the given document.
     * 
     * @param document a document
     * @param contentAuthorReference specifies the content author
     */
    void setContentAuthorReference(DocumentModelBridge document, DocumentReference contentAuthorReference);

    /**
     * @param document a document
     * @return the content author of the given document
     */
    DocumentReference getContentAuthorReference(DocumentModelBridge document);

    /**
     * @return the current document from the XWiki context
     */
    DocumentModelBridge getCurrentDocument();

    /**
     * Unlike {@link org.xwiki.bridge.DocumentAccessBridge#pushDocumentInContext(Map, DocumentReference)} which puts on
     * the context the version of the document taken from the database (the saved version), this method puts on the
     * context the given document instance. This is useful when the given document is modified, which is the case when
     * we preserve the programming rights of the sheet.
     * 
     * @param document the document instance to put on the context
     * @return the map used to restore the context
     * @see org.xwiki.bridge.DocumentAccessBridge#pushDocumentInContext(Map, DocumentReference)
     */
    Map<String, Object> pushDocumentInContext(DocumentModelBridge document);

    /**
     * @param document a document
     * @return the type of XObjects attached to the given document
     */
    Set<DocumentReference> getXObjectClassReferences(DocumentModelBridge document);
}
