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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Singleton;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AbstractLocalizedEntityReference;

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
    private static class ParameterConfiguration
    {
        /**
         * The character used to separate the entity name from the parameters.
         */
        private Character separator;

        /**
         * The name of the default parameter.
         */
        private String defaultParameter;

        /**
         * The strings which required to be escaped in the parameters keys/values.
         */
        private String[] escapes;

        /**
         * The replaces for the strings to escape.
         */
        private String[] replacements;

        ParameterConfiguration(Character separator, String defaultParameter)
        {
            this.separator = separator;
            this.defaultParameter = defaultParameter;
        }

        ParameterConfiguration(Character separator)
        {
            this.separator = separator;
        }
    }

    /**
     * A backslash character.
     */
    private static final char CESCAPE = '\\';

    /**
     * The separator used between the entity type and the reference.
     * 
     * @since 14.8RC1
     */
    private static final char CENTITYTYPESEP = ':';

    /**
     * A colon character. Colon is used to separate wiki name.
     */
    private static final char CWIKISEP = ':';

    /**
     * A slash character. Slash is used as separator for all pages model elements (except between page and wiki).
     */
    private static final char CPAGESEP = '/';

    /**
     * A semicolon character. Semicolon used as separator between the entity name and its parameters.
     */
    private static final char CPARAMETERSEP = ';';

    /**
     * Separator between the parameter key and value.
     */
    private static final char CPARAMETERVALUESEP = '=';

    /**
     * A dot character. Dot is used to separate space names and document name.
     */
    private static final char CSPACESEP = '.';

    /**
     * An at-sign character. At sign is used to separate attachment name.
     */
    private static final char CATTACHMENTSEP = '@';

    /**
     * An hat sign character. Hat sign is used to separate object name.
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

    private static final Map<EntityType, String> KEYWORDS_CURRENT = new EnumMap<>(EntityType.class);

    private static final Map<EntityType, String> KEYWORDS_PARENT = new EnumMap<>(EntityType.class);

    private static final Map<String, Type> PARAMETER_TYPES = new HashMap<>();

    static {
        SEPARATORS.put(EntityType.WIKI, Collections.emptyMap());

        // Pages
        Map<EntityType, Character> pageSeparators = new EnumMap<>(EntityType.class);
        pageSeparators.put(EntityType.WIKI, CWIKISEP);
        pageSeparators.put(EntityType.PAGE, CPAGESEP);
        SEPARATORS.put(EntityType.PAGE, pageSeparators);
        KEYWORDS_CURRENT.put(EntityType.PAGE, ".");
        KEYWORDS_PARENT.put(EntityType.PAGE, "..");

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

        PARAMETER_TYPES.put(AbstractLocalizedEntityReference.LOCALE, Locale.class);
    }

    private Map<EntityType, ParameterConfiguration> parameterSeparators;

    private Map<EntityType, String[]> escapes;

    private Map<EntityType, String[]> replacements;

    /**
     * Initialize internal data structures.
     */
    public DefaultSymbolScheme()
    {
        initialize(false);
    }

    /**
     * Initialize internal data structures.
     * 
     * @param withParameters true if the parameters syntax should be supported for all entity types
     */
    public DefaultSymbolScheme(boolean withParameters)
    {
        initialize(withParameters);
    }

    /**
     * Initialize internal data structures.
     * 
     * @param withParameters true if the parameters syntax should be supported for all entity types
     */
    private void initialize(boolean withParameters)
    {
        // Initialize parameters setup
        initializeParameters(withParameters);

        // Dynamically create the escape/replacement maps.
        // The characters to escape are all the characters that are separators between the current type and its parent
        // type + the escape symbol itself.
        this.escapes = new EnumMap<>(EntityType.class);
        this.replacements = new EnumMap<>(EntityType.class);

        String escape = Character.toString(getEscapeSymbol());
        for (Map.Entry<EntityType, Map<EntityType, Character>> entry : SEPARATORS.entrySet()) {
            EntityType type = entry.getKey();

            // Add separators escaping
            Map<EntityType, Character> separators = entry.getValue();
            List<String> charactersToEscape = new ArrayList<>();
            List<String> replacementCharacters = new ArrayList<>();
            for (Character characterToEscape : separators.values()) {
                charactersToEscape.add(characterToEscape.toString());
                replacementCharacters.add(escape + characterToEscape);
            }

            // Add parameter escaping
            ParameterConfiguration parameter = parameterSeparators.get(type);
            if (parameter != null && parameter.separator != null) {
                charactersToEscape.add(parameter.separator.toString());
                replacementCharacters.add(escape + parameter.separator);
            }

            // Add escaping character
            charactersToEscape.add(escape);
            replacementCharacters.add(escape + escape);

            String[] escapesArray = new String[charactersToEscape.size()];
            this.escapes.put(type, charactersToEscape.toArray(escapesArray));
            String[] replacementsArray = new String[replacementCharacters.size()];
            this.replacements.put(type, replacementCharacters.toArray(replacementsArray));
        }

        for (Map.Entry<EntityType, ParameterConfiguration> entry : parameterSeparators.entrySet()) {
            EntityType type = entry.getKey();
            ParameterConfiguration configuration = entry.getValue();

            if (configuration.separator != null) {
                String[] escapesArray = this.escapes.get(type);
                configuration.escapes = ArrayUtils.addAll(escapesArray, String.valueOf(CPARAMETERVALUESEP));

                String[] replacementsArray = this.replacements.get(type);
                configuration.replacements = ArrayUtils.addAll(replacementsArray, escape + CPARAMETERVALUESEP);

            }
        }
    }

    private void initializeParameters(boolean withParameters)
    {
        this.parameterSeparators = new EnumMap<>(EntityType.class);
        this.parameterSeparators.put(EntityType.PAGE,
            new ParameterConfiguration(CPARAMETERSEP, AbstractLocalizedEntityReference.LOCALE));
        this.parameterSeparators.put(EntityType.PAGE_ATTACHMENT, new ParameterConfiguration(CPARAMETERSEP));
        this.parameterSeparators.put(EntityType.PAGE_OBJECT, new ParameterConfiguration(CPARAMETERSEP));
        this.parameterSeparators.put(EntityType.PAGE_OBJECT_PROPERTY, new ParameterConfiguration(CPARAMETERSEP));
        this.parameterSeparators.put(EntityType.PAGE_CLASS_PROPERTY, new ParameterConfiguration(CPARAMETERSEP));

        if (withParameters) {
            this.parameterSeparators.put(EntityType.WIKI, new ParameterConfiguration(CPARAMETERSEP));
            this.parameterSeparators.put(EntityType.SPACE, new ParameterConfiguration(CPARAMETERSEP));
            this.parameterSeparators.put(EntityType.DOCUMENT,
                new ParameterConfiguration(CPARAMETERSEP, AbstractLocalizedEntityReference.LOCALE));
            this.parameterSeparators.put(EntityType.ATTACHMENT, new ParameterConfiguration(CPARAMETERSEP));
            this.parameterSeparators.put(EntityType.OBJECT, new ParameterConfiguration(CPARAMETERSEP));
            this.parameterSeparators.put(EntityType.OBJECT_PROPERTY, new ParameterConfiguration(CPARAMETERSEP));
            this.parameterSeparators.put(EntityType.CLASS_PROPERTY, new ParameterConfiguration(CPARAMETERSEP));
        }
    }

    @Override
    public Character getEscapeSymbol()
    {
        return CESCAPE;
    }

    @Override
    public Character getEntityTypeSeparator()
    {
        return CENTITYTYPESEP;
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

    @Override
    public Character getParameterSeparator(EntityType type)
    {
        ParameterConfiguration configuration = parameterSeparators.get(type);

        return configuration != null ? configuration.separator : null;
    }

    @Override
    public String getDefaultParameter(EntityType type)
    {
        ParameterConfiguration configuration = parameterSeparators.get(type);

        return configuration != null ? configuration.defaultParameter : null;
    }

    @Override
    public String[] getParameterSymbolsRequiringEscapes(EntityType type)
    {
        ParameterConfiguration configuration = parameterSeparators.get(type);

        return configuration != null ? configuration.escapes : null;
    }

    @Override
    public String[] getParameterReplacementSymbols(EntityType type)
    {
        ParameterConfiguration configuration = parameterSeparators.get(type);

        return configuration != null ? configuration.replacements : null;
    }

    @Override
    public Serializable resolveParameter(String parameter, String value)
    {
        Type type = PARAMETER_TYPES.get(parameter);

        if (type == Locale.class) {
            return LocaleUtils.toLocale(value);
        }

        return value;
    }

    @Override
    public String getCurrentReferenceKeyword(EntityType type)
    {
        return KEYWORDS_CURRENT.get(type);
    }

    @Override
    public String getParentReferenceKeyword(EntityType type)
    {
        return KEYWORDS_PARENT.get(type);
    }
}
