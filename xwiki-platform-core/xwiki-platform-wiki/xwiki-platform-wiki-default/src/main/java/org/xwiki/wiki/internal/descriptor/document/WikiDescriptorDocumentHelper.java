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
package org.xwiki.wiki.internal.descriptor.document;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Component to load and resolve wiki descriptor documents.
 * @version $Id$
 * @since 5.3M2
 */
@Role
public interface WikiDescriptorDocumentHelper
{
    /**
     * @param wikiId The id of the wiki that we want the descriptor document reference
     * @return the descriptor document reference corresponding to the wikiId
     */
    DocumentReference getDocumentReferenceFromId(String wikiId);

    /**
     * @param descriptorDocumentReference the reference to the document that hold the wiki descriptor
     * @return the id of the wiki corresponding to the descriptor
     */
    String getWikiIdFromDocumentReference(DocumentReference descriptorDocumentReference);

    /**
     * @param descriptorDocumentFullname the fullname of the document that hold the wiki descriptor
     * @return the id of the wiki corresponding to the descriptor
     */
    String getWikiIdFromDocumentFullname(String descriptorDocumentFullname);

    /**
     * @param wikiId The id of the wiki that we want the descriptor document
     * @return the descriptor document corresponding to the wikiId
     * @throws WikiManagerException if problems occur
     */
    XWikiDocument getDocumentFromWikiId(String wikiId) throws WikiManagerException;

    /**
     * @param wikiAlias the alias of the wiki that we cant the descriptor document reference
     * @return the wiki descriptor document reference of the specified wiki
     * @throws WikiManagerException if problems occur
     */
    DocumentReference findXWikiServerClassDocumentReference(String wikiAlias) throws WikiManagerException;

    /**
     * @param wikiAlias the alias of the wiki that we cant the descriptor document reference
     * @return the wiki descriptor document of the specified wiki
     * @throws WikiManagerException if problems occur
     */
    XWikiDocument findXWikiServerClassDocument(String wikiAlias) throws WikiManagerException;

    /**
     * @return the list of all descriptor documents
     * @throws WikiManagerException if problems occur
     */
    List<XWikiDocument> getAllXWikiServerClassDocument() throws WikiManagerException;

    /**
     * @return the list of all descriptor document names
     * @throws WikiManagerException if problems occur
     */
    List<String> getAllXWikiServerClassDocumentNames() throws WikiManagerException;
}
