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
package org.xwiki.model.validation.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Configuration component for {@link ReplaceCharacterEntityNameValidation}.
 *
 * @version $Id$
 * @since 12.0RC1
 */
@Component
@Singleton
public class DefaultReplaceCharacterEntityNameValidationConfiguration
    implements ReplaceCharacterEntityNameValidationConfiguration
{
    protected static final String PROPERTY_KEY_FORBIDDENCHARACTERS = "replaceCharacters.forbiddenCharacters";

    protected static final String PROPERTY_KEY_REPLACEMENTCHARACTERS = "replaceCharacters.replacementCharacters";

    @Inject
    @Named("entitynamevalidation")
    private ConfigurationSource configurationSource;

    /**
     * @return the list of forbidden characters.
     */
    private List<String> getForbiddenCharacters()
    {
        return this.configurationSource.getProperty(PROPERTY_KEY_FORBIDDENCHARACTERS, List.class);
    }

    /**
     * @return the list of replacement characters.
     */
    private List<String> getReplacementCharacters()
    {
        return this.configurationSource.getProperty(PROPERTY_KEY_REPLACEMENTCHARACTERS, List.class);
    }

    @Override
    public Map<String, String> getCharacterReplacementMap()
    {
        Map<String, String> result = new HashMap<>();

        List<String> forbiddenCharacters = getForbiddenCharacters();
        List<String> replacementCharacters = getReplacementCharacters();

        if (forbiddenCharacters != null && !forbiddenCharacters.isEmpty()) {
            int replacementCharactersLength = (replacementCharacters != null) ? replacementCharacters.size() : 0;
            for (int i = 0; i < forbiddenCharacters.size(); i++) {
                if (!StringUtils.isEmpty(forbiddenCharacters.get(i))) {
                    String replacementCharacter;
                    if (i >= replacementCharactersLength) {
                        replacementCharacter = "";
                    } else {
                        replacementCharacter = replacementCharacters.get(i);
                    }
                    result.put(forbiddenCharacters.get(i), replacementCharacter);
                }
            }
        }

        return result;
    }
}
