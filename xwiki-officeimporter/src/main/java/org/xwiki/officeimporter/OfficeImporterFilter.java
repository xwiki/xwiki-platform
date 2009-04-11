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
package org.xwiki.officeimporter;

import org.w3c.dom.Document;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.rendering.block.XDOM;

/**
 * A filter interface allowing customizations to the default office importer process.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public interface OfficeImporterFilter
{
    /**
     * Injects the {@link DocumentAccessBridge} component to be used for accessing documents.
     * 
     * @param docBridge the {@link DocumentAccessBridge} component.
     */
    void setDocBridge(DocumentAccessBridge docBridge);
    
    /**
     * Allows extended filtering on the main XHTML {@link Document} generated during the import process.
     * 
     * @param documentName name of the target wiki page.
     * @param document the {@link Document} containing the html content of the office document.
     */
    void filter(String documentName, Document document);
    
    /**
     * Allows extended filtering on xwiki documents generated during the import process.
     * 
     * @param documentName name of the target wiki page.
     * @param xdom the {@link XDOM} representing the master wiki document.
     * @param isSplit if this document is a result of a split operation.
     */
    void filter(String documentName, XDOM xdom, boolean isSplit);
    
    /**
     * Allows extended filtering on imported documents just before the content is saved.
     * 
     * @param documentName name of the target wiki page.
     * @param content xwiki/2.0 content of the document.
     * @param isSplit if this document is a result of a split operation.
     * @return the filtered document content.
     */
    String filter(String documentName, String content, boolean isSplit);
}
