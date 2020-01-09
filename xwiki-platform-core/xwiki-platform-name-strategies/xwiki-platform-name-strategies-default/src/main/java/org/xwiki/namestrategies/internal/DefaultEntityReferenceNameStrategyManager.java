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

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.namestrategies.EntityReferenceNameStrategy;
import org.xwiki.namestrategies.NameStrategyConfiguration;
import org.xwiki.namestrategies.EntityReferenceNameStrategyManager;

/**
 * Default manager for name strategies.
 *
 * @version $Id$
 * @since 12.0RC1
 */
@Component
@Singleton
public class DefaultEntityReferenceNameStrategyManager implements EntityReferenceNameStrategyManager
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private NameStrategyConfiguration nameStrategyConfiguration;

    @Inject
    private ReplaceCharacterNameStrategyConfiguration replaceCharacterNameStrategyConfiguration;

    @Inject
    private Logger logger;

    @Inject
    @Named(ReplaceCharacterNameStrategy.COMPONENT_NAME)
    private EntityReferenceNameStrategy replaceCharacterValidator;

    @Override
    public EntityReferenceNameStrategy getEntityReferenceNameStrategy()
    {
        return getEntityReferenceNameStrategy(this.nameStrategyConfiguration.getEntityReferenceNameStrategy());
    }

    @Override
    public EntityReferenceNameStrategy getEntityReferenceNameStrategy(String hint)
    {
        try {
            return this.componentManager.getInstance(EntityReferenceNameStrategy.class, hint);
        } catch (ComponentLookupException e) {
            this.logger.error("Error while getting the EntityReferenceNameStrategy with hint [{}]", hint, e);
            return null;
        }
    }

    @Override
    public Set<String> getAvailableEntityReferenceNameStrategies()
    {
        try {
            return this.componentManager.getInstanceMap(EntityReferenceNameStrategy.class).keySet();
        } catch (ComponentLookupException e) {
            this.logger.error("Error while getting the instance map of the EntityReferenceNameStrategies", e);
            return Collections.emptySet();
        }
    }

    @Override
    public void resetStrategies()
    {
        ((ReplaceCharacterNameStrategy) this.replaceCharacterValidator)
            .setReplacementCharacters(this.replaceCharacterNameStrategyConfiguration.getCharacterReplacementMap());
    }
}
