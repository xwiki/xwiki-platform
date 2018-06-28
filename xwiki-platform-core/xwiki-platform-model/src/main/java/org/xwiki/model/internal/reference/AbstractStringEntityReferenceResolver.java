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

import java.io.Serializable;
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
public abstract class AbstractStringEntityReferenceResolver extends AbstractEntityReferenceResolver
    implements EntityReferenceResolver<String>, Initializable
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
     *            between the Entity types, and the characters to escape and how to escape them)
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

    private StringBuilder createStringBuilder(String entityReferenceRepresentation)
    {
        // Handle the case when the passed representation is null. In this case we consider it similar to passing
        // an empty string.
        StringBuilder representation;
        if (entityReferenceRepresentation == null) {
            representation = new StringBuilder();
        } else {
            representation = new StringBuilder(entityReferenceRepresentation);
        }

        return representation;
    }

    @Override
    public EntityReference resolve(String entityReferenceRepresentation, EntityType type, Object... parameters)
    {
        Map<Character, EntityType> typeSetup = getTypeSetup(type);

        // Check if the type require anything specific
        if (typeSetup == null || typeSetup.isEmpty()) {
            return getNewReference(entityReferenceRepresentation, true, type, parameters);
        }

        StringBuilder representation = createStringBuilder(entityReferenceRepresentation);

        EntityReference reference = null;

        Character escapeSymbol = getSymbolScheme().getEscapeSymbol();

        EntityType currentType = type;

        while (typeSetup != null && !typeSetup.isEmpty()) {
            // Search all characters for a non escaped separator. If found, then consider the part after the
            // character as the reference name and continue parsing the part before the separator.
            EntityType parentType = null;

            Character parameterSeparator = getSymbolScheme().getParameterSeparator(currentType);
            String defaultParameter = getSymbolScheme().getDefaultParameter(currentType);
            Map<String, Serializable> referenceParameters = null;

            boolean unescape = false;

            int i = representation.length();
            while (--i >= 0) {
                char currentChar = representation.charAt(i);
                int nextIndex = i - 1;
                char nextChar = 0;
                if (nextIndex >= 0) {
                    nextChar = representation.charAt(nextIndex);
                }

                if (typeSetup.containsKey(currentChar)) {
                    int numberOfEscapeChars = getNumberOfCharsBefore(escapeSymbol, representation, nextIndex);

                    if (numberOfEscapeChars % 2 == 0) {
                        parentType = typeSetup.get(currentChar);
                        break;
                    } else {
                        // The character will be unescaped
                        unescape = true;
                        --i;
                    }
                } else if (nextChar == escapeSymbol) {
                    // The character will be unescaped
                    unescape = true;
                    --i;
                } else if (parameterSeparator != null && parameterSeparator.charValue() == currentChar) {
                    int numberOfEscapeChars = getNumberOfCharsBefore(escapeSymbol, representation, nextIndex);

                    if (numberOfEscapeChars % 2 == 0) {
                        if (referenceParameters == null) {
                            referenceParameters = new HashMap<>();
                        }

                        parseParameter(representation, i + 1, referenceParameters, defaultParameter, escapeSymbol);

                        representation.delete(i, representation.length());
                    } else {
                        // The character will be unescaped
                        unescape = true;
                        --i;
                    }
                }
            }

            reference = appendNewReference(reference,
                getNewReference(i, representation, unescape, currentType, referenceParameters, parameters));

            if (parentType != null) {
                currentType = parentType;
            } else {
                currentType = typeSetup.values().iterator().next();
            }

            typeSetup = getTypeSetup(currentType);
        }

        // Handle last entity reference's name
        reference = appendNewReference(reference, getNewReference(representation, true, currentType, parameters));

        // Evaluate keywords when supported ("..", ".")
        reference = evaluateKeywords(reference, parameters);

        return reference;
    }

    private EntityReference evaluateKeywords(EntityReference reference, Object... parameters)
    {
        if (reference == null) {
            return null;
        }

        EntityReference evaluatedReference = reference;

        EntityReference evaluatedParent = evaluateKeywords(reference.getParent());

        if (reference.getName().equals(getSymbolScheme().getCurrentReferenceKeyword(reference.getType()))) {
            if (evaluatedParent == null) {
                // No parent, start from the default reference
                evaluatedReference = getDefaultReference(reference.getType(), parameters);
            } else if (evaluatedParent.getType() != reference.getType()) {
                // Parent type is different, switch parent in default reference
                EntityReference defaultReference = getDefaultReference(reference.getType(), parameters);
                EntityReference defaultParent = defaultReference.extractReference(evaluatedParent.getType());
                evaluatedReference = defaultReference.replaceParent(defaultParent, evaluatedParent);
            } else {
                // Parent type is the same, stay on it
                evaluatedReference = evaluatedParent;
            }
        } else if (reference.getName().equals(getSymbolScheme().getParentReferenceKeyword(reference.getType()))) {
            if (evaluatedParent == null) {
                // No parent
                evaluatedReference = null;
            } else if (evaluatedParent.getType() != reference.getType()) {
                // Parent type is different, stay on it
                evaluatedReference = evaluatedParent;
            } else {
                // Parent type is the same, use its parent
                evaluatedReference = evaluatedParent.getParent();
            }
        } else if (evaluatedParent != reference.getParent()) {
            evaluatedReference = new EntityReference(reference, evaluatedParent);
        }

        return evaluatedReference;
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

    private String unescape(String text)
    {
        return StringUtils.isEmpty(text) ? text
            : StringUtils.replaceEach(text, this.escapeMatching, this.escapeMatchingReplace);
    }

    private EntityReference getNewReference(CharSequence representation, boolean unescape, EntityType type,
        Object... parameters)
    {
        EntityReference newReference;
        if (representation.length() > 0) {
            String name = representation.toString();
            if (unescape) {
                name = unescape(name);
            }
            newReference = new EntityReference(name, type);
        } else {
            newReference = resolveDefaultReference(type, null, parameters);
        }

        return newReference;
    }

    private EntityReference getNewReference(int i, StringBuilder representation, boolean unescape, EntityType type,
        Map<String, Serializable> referenceParameters, Object... parameters)
    {
        EntityReference newReference;

        // Found a valid separator (not escaped), separate content on its left from content on its
        // right
        if (i == representation.length() - 1) {
            newReference = resolveDefaultReference(type, referenceParameters, parameters);
        } else {
            String name = representation.substring(i + 1, representation.length());
            if (unescape) {
                name = unescape(name);
            }
            newReference = new EntityReference(name, type, referenceParameters);
        }

        representation.delete(i < 0 ? 0 : i, representation.length());

        return newReference;
    }

    protected EntityReference resolveDefaultReference(EntityType type, Map<String, Serializable> referenceParameters,
        Object... parameters)
    {
        EntityReference reference = resolveDefaultReference(type, parameters);

        if (referenceParameters != null && !referenceParameters.isEmpty()) {
            reference = new EntityReference(reference, referenceParameters);
        }

        return reference;
    }

    private void parseParameter(StringBuilder representation, int index, Map<String, Serializable> parsedParameters,
        String defaultParameter, Character escapeSymbol)
    {
        String key = null;
        String value = null;

        boolean unescape = false;
        boolean unescapeKey = false;

        int valueIndex = index;

        boolean escaped = false;
        for (int i = index; i < representation.length(); ++i) {
            char c = representation.charAt(i);

            if (escaped) {
                // The character will be unescaped
                unescape = true;
                escaped = false;
            } else {
                if (key == null && c == '=') {
                    key = representation.substring(index, i);
                    valueIndex = i + 1;
                    unescapeKey = unescape;
                    unescape = false;
                } else if (c == escapeSymbol) {
                    escaped = true;
                }
            }
        }

        // Use default parameter if no key is provided
        if (key == null) {
            key = defaultParameter;
        } else if (unescapeKey) {
            // Unescape key
            key = unescape(key);
        }

        // Priority to last parameter
        if (!parsedParameters.containsKey(key)) {
            value = representation.substring(valueIndex, representation.length());

            if (unescape) {
                // Unescape value
                value = unescape(value);
            }

            parsedParameters.put(key != null ? key : defaultParameter, value);
        }
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
