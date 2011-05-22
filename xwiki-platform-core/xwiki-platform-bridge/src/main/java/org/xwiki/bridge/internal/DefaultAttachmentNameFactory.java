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
package org.xwiki.bridge.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.AttachmentName;
import org.xwiki.bridge.AttachmentNameFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameFactory;
import org.xwiki.component.annotation.Component;

/**
 * Implementation that supports the format defined in {@link #createAttachmentName(String)}. 
 *  
 * @version $Id$
 * @since 2.0RC1
 * @deprecated use {@link org.xwiki.model.reference.AttachmentReferenceResolver} instead since 2.2M1
 */
@Component
@Singleton
@Deprecated
public class DefaultAttachmentNameFactory implements AttachmentNameFactory
{
    /**
     * Character to separate document name from filename, see {@link #createAttachmentName(String)}.
     */
    public static final String FILENAME_SEPARATOR = "@";

    /**
     * Factory that uses the current document name (if set) to create a document name from a raw reference.
     */
    @Inject
    @Named("current")
    private DocumentNameFactory currentDocumentNameFactory;

    /**
     * Factory that uses a default page name if no page is specified in the pased raw reference.
     */
    @Inject
    private DocumentNameFactory defaultDocumentNameFactory;

    /**
     * Used to get the current document name.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;
    
    /**
     * {@inheritDoc}
     * 
     * The supported formats are:
     * <ul>
     *   <li>wiki:space.page@filename</li>
     *   <li>filename</li>
     * </ul>
     * If no "@" symbol is found then the reference is the attachment's filename and the document is the current
     * document.
     * 
     * @see AttachmentNameFactory#createAttachmentName(String)
     */
    public AttachmentName createAttachmentName(String reference)
    {
        AttachmentName name;
        
        int pos = reference.lastIndexOf(FILENAME_SEPARATOR);
        if (pos > -1) {
            String rawDocumentName = reference.substring(0, pos);
            String filename = reference.substring(pos + 1);
            name = new AttachmentName(this.currentDocumentNameFactory.createDocumentName(rawDocumentName), filename); 
        } else {
            // No filename separator, we consider the full reference as a filename attached to the current document
            DocumentName currentDocumentName = this.documentAccessBridge.getCurrentDocumentName();
            if (currentDocumentName == null) {
                // If no current document is set use a default document factory
                name = new AttachmentName(this.defaultDocumentNameFactory.createDocumentName(null), reference);
            } else {
                name = new AttachmentName(currentDocumentName, reference);
            }
        }

        return name;
    }
}
