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

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.phase.Initializable;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;

/**
 * Generic implementation deferring default values for unspecified reference parts to extending classes. This allows for
 * example both the Current Entity Reference Resolver and the Default Entity Reference Resolver to share the code from
 * this class.
 *
 * @see AbstractEntityReferenceResolver
 * @version $Id$
 * @since 2.2M1
 */
public abstract class AbstractStringEntityReferenceResolver extends AbstractEntityReferenceResolver implements
    EntityReferenceResolver<String>, Initializable
{
    /**
     * Array of character to unescape in entity names.
     */
    private String[] escapeMatching;

    /**
     * The replacement array corresponding to the array in {@link #escapeMatching} array.
     */
    private String[] escapeMatchingReplace;

    @Inject
    private SymbolScheme symbolScheme;

    private Map<EntityType, Map<Character, EntityType>> referenceSetup;

    /**
     * Empty constructor, to be used by the Component Manager, which will also inject the Symbol Scheme.
     */
    public AbstractStringEntityReferenceResolver()
    {
        // Empty constructor, to be used by the Component Manager, which will also inject the Symbol Scheme
    }

    /**
     * Constructor to be used when using this class as a POJO and not as a component.
     *
     * @param symbolScheme the scheme to use for serializing the passed references (i.e. defines the separators to use
     *        between the Entity types, and the characters to escape and how to escape them)
     */
    public AbstractStringEntityReferenceResolver(SymbolScheme symbolScheme)
    {
        this.symbolScheme = symbolScheme;
        initialize();
    }

    @Override
    public void initialize()
    {
        this.referenceSetup = new EnumMap<>(EntityType.class);

        Map<EntityType, Map<EntityType, Character>> separators = getSymbolScheme().getSeparatorSymbols();
        for (Map.Entry<EntityType, Map<EntityType, Character>> separatorEntry : separators.entrySet()) {
            Map<Character, EntityType> characterMap = new HashMap<>();
            for (Map.Entry<EntityType, Character> characterEntry : separatorEntry.getValue().entrySet()) {
                characterMap.put(characterEntry.getValue(), characterEntry.getKey());
            }
            this.referenceSetup.put(separatorEntry.getKey(), characterMap);
        }

        String escape = Character.toString(getSymbolScheme().getEscapeSymbol());
        this.escapeMatching = new String[] { escape + escape, escape };
        this.escapeMatchingReplace = new String[] { escape, StringUtils.EMPTY };
    }

    @Override
    public EntityReference resolve(String entityReferenceRepresentation, EntityType type, Object... parameters)
    {
        Map<Character, EntityType> typeSetup = getTypeSetup(type);

        // Check if the type require anything specific
        if (typeSetup == null) {
            return getEscapedReference(entityReferenceRepresentation, type, parameters);
        }

        // Handle the case when the passed representation is null. In this case we consider it similar to passing
        // an empty string.
        StringBuilder representation;
        if (entityReferenceRepresentation == null) {
            representation = new StringBuilder();
        } else {
            representation = new StringBuilder(entityReferenceRepresentation);
        }

        EntityReference reference = null;

        EntityType currentType = type;

        while (typeSetup != null && !typeSetup.isEmpty()) {
            // Search all characters for a non escaped separator. If found, then consider the part after the
            // character as the reference name and continue parsing the part before the separator.
            EntityType parentType = null;

            int i = representation.length();
            while (--i >= 0) {
                char currentChar = representation.charAt(i);
                int nextIndex = i - 1;
                char nextChar = 0;
                if (nextIndex >= 0) {
                    nextChar = representation.charAt(nextIndex);
                }

                if (typeSetup.containsKey(currentChar)) {
                    int numberOfEscapeChars =
                        getNumberOfCharsBefore(getSymbolScheme().getEscapeSymbol(), representation, nextIndex);

                    if (numberOfEscapeChars % 2 == 0) {
                        parentType = typeSetup.get(currentChar);
                        break;
                    } else {
                        // Unescape the character
                        representation.delete(nextIndex, i);
                        --i;
                    }
                } else if (nextChar == getSymbolScheme().getEscapeSymbol()) {
                    // Unescape the character
                    representation.delete(nextIndex, i);
                    --i;
                }
            }

            reference = appendNewReference(reference, getNewReference(i, representation, currentType, parameters));

            if (parentType != null) {
                currentType = parentType;
            } else {
                currentType = typeSetup.values().iterator().next();
            }

            typeSetup = getTypeSetup(currentType);
        }

        // Handle last entity reference's name
        reference = appendNewReference(reference, getEscapedReference(representation, currentType, parameters));

        return reference;
    }

    /**
     * The default is extracted from the default {@link SymbolScheme}, but extending classes can override it.
     *
     * @param type the type for which to get the setup
     * @return the reference setup map for the requested type, consisting of &lt;parent separator, parent type&gt; pairs
     * @since 7.4.1
     * @since 8.0M1
     */
    protected Map<Character, EntityType> getTypeSetup(EntityType type)
    {
        return this.referenceSetup.get(type);
    }

    private EntityReference getEscapedReference(CharSequence representation, EntityType type, Object... parameters)
    {
        EntityReference newReference;
        if (representation.length() > 0) {
            String name =
                StringUtils.replaceEach(representation.toString(), this.escapeMatching, this.escapeMatchingReplace);
            if (name != null) {
                newReference = new EntityReference(name, type);
            } else {
                newReference = null;
            }
        } else {
            newReference = resolveDefaultReference(type, parameters);
        }

        return newReference;
    }

    private EntityReference getNewReference(int i, StringBuilder representation, EntityType type, Object... parameters)
    {
        EntityReference newReference;

        // Found a valid separator (not escaped), separate content on its left from content on its
        // right
        if (i == representation.length() - 1) {
            newReference = resolveDefaultReference(type, parameters);
        } else {
            String name = representation.substring(i + 1, representation.length());
            newReference = new EntityReference(name, type);
        }

        representation.delete(i < 0 ? 0 : i, representation.length());

        return newReference;
    }

    private EntityReference appendNewReference(EntityReference reference, EntityReference newReference)
    {
        if (newReference != null) {
            if (reference != null) {
                return reference.appendParent(newReference);
            } else {
                return newReference;
            }
        }

        return reference;
    }

    /**
     * Search how many time the provided character is found consecutively started to the provided index and before.
     * 
     * @param c the character to be searched
     * @param representation the string being searched
     * @param currentPosition the current position where the search is started in backward direction
     * @return the number of character in the found group
     */
    private int getNumberOfCharsBefore(char c, StringBuilder representation, int currentPosition)
    {
        int position = currentPosition;

        while (position >= 0 && representation.charAt(position) == c) {
            --position;
        }

        return currentPosition - position;
    }

    protected SymbolScheme getSymbolScheme()
    {
        return this.symbolScheme;
    }
}
