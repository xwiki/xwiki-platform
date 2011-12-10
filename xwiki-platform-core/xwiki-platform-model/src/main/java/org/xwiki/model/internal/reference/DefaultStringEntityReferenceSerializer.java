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

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;

import static org.xwiki.model.internal.reference.StringReferenceSeparators.DBLESCAPE;
import static org.xwiki.model.internal.reference.StringReferenceSeparators.ESCAPE;
import static org.xwiki.model.internal.reference.StringReferenceSeparators.ESCAPES;
import static org.xwiki.model.internal.reference.StringReferenceSeparators.REPLACEMENTS;
import static org.xwiki.model.internal.reference.StringReferenceSeparators.WIKISEP;

/**
 * Generate a string representation of an entity reference (eg "wiki:space.page" for a document reference in the "wiki"
 * Wiki, the "space" Space and the "page" Page).
 * 
 * @version $Id$
 * @since 2.2M1
 */
@Component
public class DefaultStringEntityReferenceSerializer extends AbstractStringEntityReferenceSerializer
{
    @Override
    protected void serializeEntityReference(EntityReference currentReference, StringBuilder representation,
        boolean isLastReference, Object... parameters)
    {
        EntityType currentType = currentReference.getType();
        EntityReference currentParent = currentReference.getParent();
        String[] currentEscapeChars = ESCAPES.get(currentType);

        // Add my separator if I am not the first one in the representation
        if (currentParent != null && representation.length() > 0) {
            if (currentParent.getType() == EntityType.WIKI) {
                representation.append(WIKISEP);
            } else {
                representation.append(currentEscapeChars[0]);
            }
        }

        // If we're on the Root reference then we don't need to escape anything
        if (currentEscapeChars != null) {
            representation.append(StringUtils.replaceEach(currentReference.getName(), currentEscapeChars,
                REPLACEMENTS.get(currentType)));
        } else {
            representation.append(currentReference.getName().replace(ESCAPE, DBLESCAPE));
        }
    }
}
