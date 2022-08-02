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

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * <p>
 * Serialize a reference into a unique identifier string within a wiki. Its similar to the
 * {@link UidStringEntityReferenceSerializer}, but is made appropriate for a wiki independent storage.
 * </p>
 * <p>
 * The string created looks like {@code 5:space3:doc} for the {@code wiki:space.doc} document reference. and
 * {@code 5:space3:doc15:xspace.class[0]} for the wiki:space.doc^wiki:xspace.class[0] object. (with {@code 5} being the
 * length of the space name, i.e the length of {@code space} and {@code 3} being the length of the page name, i.e. the
 * length of {@code doc}).
 * </p>
 *
 * @version $Id$
 * @since 4.0M1
 * @see UidStringEntityReferenceSerializer
 */
@Component
@Named("local/uid")
@Singleton
public class LocalUidStringEntityReferenceSerializer implements EntityReferenceSerializer<String>
{
    /**
     * Unique instance of the uid serializer.
     */
    public static final LocalUidStringEntityReferenceSerializer INSTANCE =
        new LocalUidStringEntityReferenceSerializer();

    @Inject
    private SymbolScheme symbolScheme;

    @Override
    public String serialize(EntityReference reference, Object... parameters)
    {
        if (reference == null) {
            return null;
        }

        StringBuilder representation = new StringBuilder();
        List<EntityReference> references = reference.getReversedReferenceChain();
        EntityReference wikiReference = references.get(0);

        int index;
        if (wikiReference.getType() == EntityType.WIKI) {
            index = 1;
        } else {
            index = 0;
        }

        for (; index < reference.size(); ++index) {
            serializeEntityReference(references.get(index), representation, wikiReference, parameters);
        }

        return representation.toString();
    }

    /**
     * Serialize a single reference element into the representation string builder.
     *
     * @param currentReference the reference to serialize
     * @param representation the builder where to happen the serialized member
     * @param wikiReference the wiki reference of this entity reference
     * @param parameters optional parameters
     */
    protected void serializeEntityReference(EntityReference currentReference, StringBuilder representation,
        EntityReference wikiReference, Object... parameters)
    {
        String name = currentReference.getName();

        // FIXME: Not really nice to parse here the serialized XClass reference to remove its wiki name when local.
        // This also why this is not a simple derived class of UidStringEntityReferenceSerializer.
        if (wikiReference != null && currentReference.getType() == EntityType.OBJECT) {
            if (name.startsWith(wikiReference.getName()
                + this.symbolScheme.getSeparatorSymbols().get(EntityType.SPACE).get(EntityType.WIKI)))
            {
                name = name.substring(wikiReference.getName().length() + 1);
            }
        }
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
