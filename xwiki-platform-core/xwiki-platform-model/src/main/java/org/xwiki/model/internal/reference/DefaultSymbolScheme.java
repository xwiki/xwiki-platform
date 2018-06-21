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
import java.util.EnumMap;
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
     * A slash string. Slash is used as separator for all pages model elements (except between page and wiki).
     */
    private static final char CPAGESEP = '/';

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
    private static final char COBJECTSEP = '^';

    /**
     * An dot is used to separate object property name.
     */
    private static final char CPROPERTYSEP = CSPACESEP;

    /**
     * An hat sign is used to separate class name.
     */
    private static final char CCLASSPROPSEP = COBJECTSEP;

    private static final Map<EntityType, Map<EntityType, Character>> SEPARATORS = new EnumMap<>(EntityType.class);

    static {
        SEPARATORS.put(EntityType.WIKI, Collections.emptyMap());

        // Pages
        Map<EntityType, Character> pageSeparators = new EnumMap<>(EntityType.class);
        pageSeparators.put(EntityType.WIKI, CWIKISEP);
        pageSeparators.put(EntityType.SPACE, CPAGESEP);
        SEPARATORS.put(EntityType.PAGE, pageSeparators);

        SEPARATORS.put(EntityType.PAGE_ATTACHMENT, Collections.singletonMap(EntityType.PAGE, CPAGESEP));
        SEPARATORS.put(EntityType.PAGE_OBJECT, Collections.singletonMap(EntityType.PAGE, CPAGESEP));
        SEPARATORS.put(EntityType.PAGE_OBJECT_PROPERTY, Collections.singletonMap(EntityType.PAGE_OBJECT, CPAGESEP));
        SEPARATORS.put(EntityType.PAGE_CLASS_PROPERTY, Collections.singletonMap(EntityType.PAGE, CPAGESEP));

        // Documents
        Map<EntityType, Character> spaceSeparators = new EnumMap<>(EntityType.class);
        spaceSeparators.put(EntityType.WIKI, CWIKISEP);
        spaceSeparators.put(EntityType.SPACE, CSPACESEP);
        SEPARATORS.put(EntityType.SPACE, spaceSeparators);

        SEPARATORS.put(EntityType.DOCUMENT, Collections.singletonMap(EntityType.SPACE, CSPACESEP));
        SEPARATORS.put(EntityType.ATTACHMENT, Collections.singletonMap(EntityType.DOCUMENT, CATTACHMENTSEP));
        SEPARATORS.put(EntityType.OBJECT, Collections.singletonMap(EntityType.DOCUMENT, COBJECTSEP));
        SEPARATORS.put(EntityType.OBJECT_PROPERTY, Collections.singletonMap(EntityType.OBJECT, CPROPERTYSEP));
        SEPARATORS.put(EntityType.CLASS_PROPERTY, Collections.singletonMap(EntityType.DOCUMENT, CCLASSPROPSEP));
    }

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
        this.escapes = new EnumMap<>(EntityType.class);
        this.replacements = new EnumMap<>(EntityType.class);

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
