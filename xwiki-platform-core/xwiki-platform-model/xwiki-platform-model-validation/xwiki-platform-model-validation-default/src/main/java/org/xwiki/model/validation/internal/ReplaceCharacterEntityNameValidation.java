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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.validation.AbstractEntityNameValidation;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Name strategy that aims at replacing some forbidden characters with others. In case a character replacement is not
 * defined, the forbidden character will just be removed.
 *
 * @version $Id$
 * @since 12.0RC1
 */
@Component
@Singleton
@Named(ReplaceCharacterEntityNameValidation.COMPONENT_NAME)
public class ReplaceCharacterEntityNameValidation extends AbstractEntityNameValidation
{
    /**
     * An object encapsulating an entity name validation configuration.
     */
    private static class Configuration
    {
        private String[] forbiddenCharacters;

        private String[] replacementCharacters;
    }

    /**
     * Name of the component.
     */
    protected static final String COMPONENT_NAME = "ReplaceCharacterEntityNameValidation";

    /**
     * A map used as a cache of the current configurations of the wikis.
     */
    private final Map<String, Configuration> wikiConfigurationCache = new ConcurrentHashMap<>();

    @Inject
    private ReplaceCharacterEntityNameValidationConfiguration replaceCharacterEntityNameValidationConfiguration;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public String transform(String name)
    {
        return StringUtils.replaceEach(name, this.getForbiddenCharacters(), this.getReplacementCharacters());
    }

    @Override
    public boolean isValid(String name)
    {
        return !StringUtils.containsAny(name, this.getForbiddenCharacters());
    }

    @Override
    public void cleanConfigurationCache(String wikiId)
    {
        this.wikiConfigurationCache.remove(wikiId);
    }

    private String[] getReplacementCharacters()
    {
        return getWikiConfigurationCache().replacementCharacters;
    }

    private String[] getForbiddenCharacters()
    {
        return getWikiConfigurationCache().forbiddenCharacters;
    }

    private Configuration getWikiConfigurationCache()
    {
        String currentWikiId = this.wikiDescriptorManager.getCurrentWikiId();
        return this.wikiConfigurationCache.computeIfAbsent(currentWikiId, wikiId -> {
            Configuration configuration = new Configuration();
            Map<String, String> replacementMap =
                this.replaceCharacterEntityNameValidationConfiguration.getCharacterReplacementMap();
            configuration.forbiddenCharacters = new String[replacementMap.size()];
            configuration.replacementCharacters = new String[replacementMap.size()];

            int index = 0;
            for (Map.Entry<String, String> characterCharacterEntry : replacementMap.entrySet()) {
                configuration.forbiddenCharacters[index] = characterCharacterEntry.getKey();
                if (characterCharacterEntry.getValue() != null) {
                    configuration.replacementCharacters[index] = characterCharacterEntry.getValue();
                } else {
                    configuration.replacementCharacters[index] = "";
                }
                index++;
            }
            return configuration;
        });
    }
}
