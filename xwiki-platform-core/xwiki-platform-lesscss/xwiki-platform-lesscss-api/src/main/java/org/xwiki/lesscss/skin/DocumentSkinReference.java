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
package org.xwiki.lesscss.skin;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * A specialized implementation of {@link SkinReference} for any skin stored as a document in the wiki.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Unstable
public class DocumentSkinReference implements SkinReference
{
    private DocumentReference skinDocument;

    /**
     * Construct a new document skin reference.
     * @param skinDocument reference to the skin document
     */
    public DocumentSkinReference(DocumentReference skinDocument)
    {
        this.skinDocument = skinDocument;
    }

    /**
     * @return the skin document
     */
    public DocumentReference getSkinDocument()
    {
        return skinDocument;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DocumentSkinReference) {
            DocumentSkinReference documentSkinReference = (DocumentSkinReference) o;
            return skinDocument.equals(documentSkinReference.skinDocument);
        }
        return false;
    };

    @Override
    public int hashCode() {
        return skinDocument.hashCode();
    }

    @Override
    public String toString()
    {
        return String.format("SkinDocument[%s]", skinDocument);
    }
}
