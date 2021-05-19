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

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.model.validation.EntityNameValidationConfiguration;
import org.xwiki.model.validation.EntityNameValidationManager;

/**
 * Default manager for name strategies.
 *
 * @version $Id$
 * @since 12.0RC1
 */
@Component
@Singleton
public class DefaultEntityNameValidationManager implements EntityNameValidationManager
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private EntityNameValidationConfiguration entityNameValidationConfiguration;

    @Inject
    private Logger logger;

    @Override
    public EntityNameValidation getEntityReferenceNameStrategy()
    {
        return getEntityReferenceNameStrategy(this.entityNameValidationConfiguration.getEntityNameValidation());
    }

    @Override
    public EntityNameValidation getEntityReferenceNameStrategy(String hint)
    {
        try {
            return this.componentManager.getInstance(EntityNameValidation.class, hint);
        } catch (ComponentLookupException e) {
            this.logger.error("Error while getting the EntityReferenceNameStrategy with hint [{}]", hint, e);
            return null;
        }
    }

    @Override
    public Set<String> getAvailableEntityNameValidations()
    {
        try {
            return this.componentManager.getInstanceMap(EntityNameValidation.class).keySet();
        } catch (ComponentLookupException e) {
            this.logger.error("Error while getting the instance map of the EntityNameValidation", e);
            return Collections.emptySet();
        }
    }

    @Override
    public List<EntityNameValidation> getAvailableEntityNameValidationsComponents()
    {
        try {
            return this.componentManager.getInstanceList(EntityNameValidation.class);
        } catch (ComponentLookupException e) {
            this.logger.error("Error while getting the instance list of the EntityNameValidation", e);
            return Collections.emptyList();
        }
    }
}
