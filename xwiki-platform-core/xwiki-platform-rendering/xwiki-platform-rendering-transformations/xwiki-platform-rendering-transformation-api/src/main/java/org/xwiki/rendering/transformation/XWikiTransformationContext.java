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
package org.xwiki.rendering.transformation;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Extends the generic {@link TransformationContext} to add XWiki specific information.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@Unstable
public class XWikiTransformationContext extends TransformationContext
{
    private DocumentReference contentDocumentReference;

    /**
     * @return the reference of the document whose content is being transformed; some transformations require specific
     *         access rights, which are evaluated for the current user against this document
     */
    public DocumentReference getContentDocumentReference()
    {
        return this.contentDocumentReference;
    }

    /**
     * Set the reference of the document whose content is being transformed. Some transformations require specific
     * access rights, which are evaluated for the current user against this document.
     *
     * @param contentDocumentReference the content document reference
     */
    public void setContentDocumentReference(DocumentReference contentDocumentReference)
    {
        this.contentDocumentReference = contentDocumentReference;
    }
}
