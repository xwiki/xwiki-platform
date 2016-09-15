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

import java.util.Locale;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Serialize a reference into a unique identifier string. This is faster compare to the human readable default
 * string serializer and it provide unique reversible string without requiring complex escaping. Compare to default
 * string serializers, it is aimed to be used only by computer and it will be kept stable over time. These strings are
 * comparable and could be used when an string identifier is required (ie: hashmap, caches, storage...). These strings
 * are also a better source for hashing algorithms.
 *
 * ie: The string created looks like 4:wiki5:space3:doc for the wiki:space.doc document
 * and 4:wiki5:space3:doc20:wiki:xspace.class[0] for the wiki:space.doc^wiki:xspace.class[0] object.
 *
 * @version $Id$
 * @since 4.0M1
 * @see LocalUidStringEntityReferenceSerializer
 */
@Component
@Named("uid")
@Singleton
public class UidStringEntityReferenceSerializer extends AbstractStringEntityReferenceSerializer
{
    @Override
    protected void serializeEntityReference(EntityReference currentReference, StringBuilder representation,
        boolean isLastReference, Object... parameters)
    {
        // Append name
        String name = currentReference.getName();
        representation.append(name.length()).append(':').append(name);

        // Append Locale
        if (currentReference instanceof DocumentReference) {
            Locale locale = ((DocumentReference) currentReference).getLocale();
            if (locale != null) {
                String localeString = locale.toString();
                if (!localeString.isEmpty()) {
                    representation.append(localeString.length()).append(':').append(localeString);
                }
            }
        }
    }
}
