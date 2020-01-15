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
package org.xwiki.model.internal.reference;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

/**
 * Extends {@link DefaultStringEntityReferenceSerializer} to add the serialization of the Locale for Document
 * References.
 *
 * @version $Id$
 * @since 4.2M3
 */
public class LocalizedStringEntityReferenceSerializer extends DefaultStringEntityReferenceSerializer
{
    /**
     * @param symbolScheme the scheme to use for serializing the passed references (i.e. defines the separators to use
     *        between the Entity types, and the characters to escape and how to escape them)
     */
    public LocalizedStringEntityReferenceSerializer(SymbolScheme symbolScheme)
    {
        super(symbolScheme);
    }

    @Override
    protected void serializeEntityReference(EntityReference currentReference, StringBuilder representation,
        boolean isLastReference, Object... parameters)
    {
        super.serializeEntityReference(currentReference, representation, isLastReference, parameters);

        // Append parameters for DocumentReference (if any)
        if (currentReference instanceof DocumentReference) {
            DocumentReference documentReference = (DocumentReference) currentReference;
            if (documentReference.getLocale() != null) {
                representation.append('(').append(documentReference.getLocale()).append(')');
            }
        } else if (currentReference instanceof LocalDocumentReference) {
            LocalDocumentReference documentReference = (LocalDocumentReference) currentReference;
            if (documentReference.getLocale() != null) {
                representation.append('(').append(documentReference.getLocale()).append(')');
            }
        }
    }
}
