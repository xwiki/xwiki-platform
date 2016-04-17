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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;

/**
 * Default Symbols used for representing {@link org.xwiki.model.reference.EntityReference} as strings.
 *
 * @version $Id$
 * @since 8.1M2
 */
@Component
@Singleton
public class DefaultSymbolScheme implements SymbolScheme
{
    /**
     * A backslash string.
     */
    private static final char CESCAPE = '\\';

    /**
     * A colon string. Colon is used to separate wiki name.
     */
    private static final char CWIKISEP = ':';

    /**
     * A dot string. Dot is used to separate space names and document name.
     */
    private static final char CSPACESEP = '.';

    /**
     * An at-sign string. At sign is used to separate attachment name.
     */
    private static final char CATTACHMENTSEP = '@';

    /**
     * An hat sign string. Hat sign is used to separate object name.
     */
    private static final  char COBJECTSEP = '^';

    /**
     * An dot is used to separate object property name.
     */
    private static final char CPROPERTYSEP = CSPACESEP;

    /**
     * An hat sign is used to separate class name.
     */
    private static final char CCLASSPROPSEP = COBJECTSEP;

    private static final Map<EntityType, Map<EntityType, Character>> SEPARATORS =
        new HashMap<EntityType, Map<EntityType, Character>>()
    {
        {
            put(EntityType.WIKI, Collections.emptyMap());

            Map<EntityType, Character> spaceSeparators = new HashMap<>();
            spaceSeparators.put(EntityType.WIKI, CWIKISEP);
            spaceSeparators.put(EntityType.SPACE, CSPACESEP);
            put(EntityType.SPACE, spaceSeparators);

            put(EntityType.DOCUMENT, Collections.singletonMap(EntityType.SPACE, CSPACESEP));
            put(EntityType.ATTACHMENT, Collections.singletonMap(EntityType.DOCUMENT, CATTACHMENTSEP));
            put(EntityType.OBJECT, Collections.singletonMap(EntityType.DOCUMENT, COBJECTSEP));
            put(EntityType.OBJECT_PROPERTY, Collections.singletonMap(EntityType.OBJECT, CPROPERTYSEP));
            put(EntityType.CLASS_PROPERTY, Collections.singletonMap(EntityType.DOCUMENT, CCLASSPROPSEP));
        }
    };

    private Map<EntityType, String[]> escapes;

    private Map<EntityType, String[]> replacements;

    /**
     * Initialize internal data structures.
     */
    public DefaultSymbolScheme()
    {
        initialize();
    }

    /**
     * Initialize internal data structures.
     */
    public void initialize()
    {
        // Dynamically create the escape/replacement maps.
        // The characters to escape are all the characters that are separators between the current type and its parent
        // type + the escape symbol itself.
        this.escapes = new HashMap<>();
        this.replacements = new HashMap<>();

        String escape = Character.toString(getEscapeSymbol());
        for (Map.Entry<EntityType, Map<EntityType, Character>> entry : SEPARATORS.entrySet()) {
            EntityType type = entry.getKey();
            Map<EntityType, Character> separators = entry.getValue();
            List<String> charactersToEscape = new ArrayList<>();
            List<String> replacementCharacters = new ArrayList<>();
            for (Character characterToEscape : separators.values()) {
                charactersToEscape.add(Character.toString(characterToEscape));
                replacementCharacters.add(escape + Character.toString(characterToEscape));
            }
            charactersToEscape.add(escape);
            replacementCharacters.add(escape + escape);
            String[] escapesArray = new String[charactersToEscape.size()];
            this.escapes.put(type, charactersToEscape.toArray(escapesArray));
            String[] replacementsArray = new String[replacementCharacters.size()];
            this.replacements.put(type, replacementCharacters.toArray(replacementsArray));
        }
    }

    @Override
    public Character getEscapeSymbol()
    {
        return CESCAPE;
    }

    @Override
    public Map<EntityType, Map<EntityType, Character>> getSeparatorSymbols()
    {
        return SEPARATORS;
    }

    @Override
    public String[] getSymbolsRequiringEscapes(EntityType type)
    {
        return this.escapes.get(type);
    }

    @Override
    public String[] getReplacementSymbols(EntityType type)
    {
        return this.replacements.get(type);
    }
}
