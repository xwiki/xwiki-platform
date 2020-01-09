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
package org.xwiki.namestrategies.internal;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.namestrategies.AbstractEntityReferenceNameStrategy;

/**
 * Name strategy that aims at replacing some forbidden characters with others.
 * In case a character replacement is not defined, the forbidden character will just be removed.
 *
 * @version $Id$
 * @since 12.0RC1
 */
@Component
@Singleton
@Named(ReplaceCharacterNameStrategy.COMPONENT_NAME)
public class ReplaceCharacterNameStrategy extends AbstractEntityReferenceNameStrategy
    implements Initializable
{
    /**
     * Name of the component.
     */
    protected static final String COMPONENT_NAME = "ReplaceCharacterNameStrategy";

    private String[] forbiddenCharacters;
    private String[] replacementCharacters;

    @Inject
    private ReplaceCharacterNameStrategyConfiguration replaceCharacterNameStrategyConfiguration;

    @Override
    public void initialize() throws InitializationException
    {
        this.setReplacementCharacters(this.replaceCharacterNameStrategyConfiguration.getCharacterReplacementMap());
    }

    /**
     * Configure the component based on a replacement map.
     *
     * @param replacementMap keys of the map are the forbidden characters and values are the replacement characters.
     *                      If a value is null, then the forbidden character will be removed during transformation.
     */
    public void setReplacementCharacters(Map<String, String> replacementMap)
    {
        this.forbiddenCharacters = new String[replacementMap.size()];
        this.replacementCharacters = new String[replacementMap.size()];
        int index = 0;

        for (Map.Entry<String, String> characterCharacterEntry : replacementMap.entrySet()) {
            this.forbiddenCharacters[index] = characterCharacterEntry.getKey();
            if (characterCharacterEntry.getValue() != null) {
                this.replacementCharacters[index] = characterCharacterEntry.getValue();
            } else {
                this.replacementCharacters[index] = "";
            }
            index++;
        }
    }

    @Override
    public String transform(String name)
    {
        return StringUtils.replaceEach(name, this.forbiddenCharacters, this.replacementCharacters);
    }

    @Override
    public boolean isValid(String name)
    {
        return !StringUtils.containsAny(name, this.forbiddenCharacters);
    }
}
