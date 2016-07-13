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
package org.xwiki.lesscss.internal.colortheme;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Specialized implementation of {@link org.xwiki.lesscss.internal.colortheme.ColorThemeReference} for color theme stored as a
 * document in the wiki.
 *
 * @since 6.4M2
 * @version $Id$
 */
public class DocumentColorThemeReference implements ColorThemeReference
{
    private DocumentReference colorThemeDocument;

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Construct a new reference.
     * @param colorThemeDocument reference to the color theme document
     * @param entityReferenceSerializer document reference serializer
     */
    public DocumentColorThemeReference(DocumentReference colorThemeDocument, 
            EntityReferenceSerializer<String> entityReferenceSerializer)
    {
        this.colorThemeDocument = colorThemeDocument;
        this.entityReferenceSerializer = entityReferenceSerializer;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DocumentColorThemeReference) {
            DocumentColorThemeReference documentSkinReference = (DocumentColorThemeReference) o;
            return colorThemeDocument.equals(documentSkinReference.colorThemeDocument);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return colorThemeDocument.hashCode();
    }

    @Override
    public String serialize()
    {
        return String.format("ColorThemeDocument[%s]", entityReferenceSerializer.serialize(colorThemeDocument));
    }

    @Override
    public String toString()
    {
        return serialize();
    }
}
