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
package org.xwiki.model.validation.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.model.validation.EntityNameValidationConfiguration;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.model.validation.internal.ReplaceCharacterEntityNameValidationConfiguration;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Script service for name strategies.
 *
 * @version $Id$
 * @since 12.0RC1
 */
@Component
@Named(ModelValidationScriptService.ID)
@Singleton
public class ModelValidationScriptService implements ScriptService
{
    /**
     * The id of this script service.
     */
    public static final String ID = "modelvalidation";

    @Inject
    private EntityNameValidationManager entityNameValidationManager;

    @Inject
    private EntityNameValidationConfiguration entityNameValidationConfiguration;

    @Inject
    private ReplaceCharacterEntityNameValidationConfiguration replaceCharacterEntityNameValidationConfiguration;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    private ScriptServiceManager scriptServiceManager;

    /**
     * Return the name strategy manager if the user has programming rights only.
     *
     * @return the manager for name strategies or null.
     */
    public EntityNameValidationManager getManager()
    {
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            return this.entityNameValidationManager;
        } else {
            return null;
        }
    }

    /**
     * Return the name strategy configuration if the user has programming rights only.
     *
     * @return the configuration for name strategies or null.
     */
    public EntityNameValidationConfiguration getConfiguration()
    {
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            return this.entityNameValidationConfiguration;
        } else {
            return null;
        }
    }

    /**
     * @return the configuration replace character entity name configuration
     */
    public ReplaceCharacterEntityNameValidationConfiguration getReplaceCharacterEntityNameValidationConfiguration()
    {
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            return this.replaceCharacterEntityNameValidationConfiguration;
        } else {
            return null;
        }
    }

    /**
     * Transform a name according to the current name strategy, if the configuration is set to use transformation.
     * Else it will just return the given name.
     *
     * @param name the name to transform.
     * @return the transformed named.
     */
    public String transformName(String name)
    {
        if (this.entityNameValidationConfiguration.useTransformation()
            && this.entityNameValidationManager.getEntityReferenceNameStrategy() != null)
        {
            return this.entityNameValidationManager.getEntityReferenceNameStrategy().transform(name);
        } else {
            return name;
        }
    }

    /**
     * Transform an entity reference according to the current name strategy if the configuration is set to use
     * transformation. Else it will just return the given source entity.
     *
     * @param sourceEntity the entity reference to transform.
     * @return the transformed entity reference.
     */
    public EntityReference transformEntityReference(EntityReference sourceEntity)
    {
        if (this.entityNameValidationConfiguration.useTransformation()
            && this.entityNameValidationManager.getEntityReferenceNameStrategy() != null)
        {
            return this.entityNameValidationManager.getEntityReferenceNameStrategy().transform(sourceEntity);
        } else {
            return sourceEntity;
        }
    }

    /**
     * Transform a name according to the given name strategy.
     *
     * @param name the name to transform.
     * @param hint hint of the name strategy component to use.
     * @return a transformed name.
     */
    public String transformName(String name, String hint)
    {
        EntityNameValidation entityReferenceNameStrategy =
            this.entityNameValidationManager.getEntityReferenceNameStrategy(hint);
        if (entityReferenceNameStrategy != null) {
            return entityReferenceNameStrategy.transform(name);
        } else {
            return name;
        }
    }

    /**
     * Transform a source entity according to the given name strategy.
     *
     * @param sourceEntity the source entity to transform.
     * @param hint hint of the name strategy component to use.
     * @return a transformed source entity.
     */
    public EntityReference transformEntityReference(EntityReference sourceEntity, String hint)
    {
        EntityNameValidation entityReferenceNameStrategy =
            this.entityNameValidationManager.getEntityReferenceNameStrategy(hint);
        if (entityReferenceNameStrategy != null) {
            return entityReferenceNameStrategy.transform(sourceEntity);
        } else {
            return sourceEntity;
        }
    }

    /**
     * Validate a name according to the current name strategy, only if the configuration is set to use validation.
     *
     * @param name the name to validate.
     * @return {@code true} if the name is valid, or if the configuration is set to not use validation.
     */
    public boolean isValid(String name)
    {
        if (this.entityNameValidationConfiguration.useValidation()
            && this.entityNameValidationManager.getEntityReferenceNameStrategy() != null)
        {
            return this.entityNameValidationManager.getEntityReferenceNameStrategy().isValid(name);
        } else {
            return true;
        }
    }

    /**
     * Validate a source entity according to the current name strategy, only if the configuration is set to use
     * validation.
     *
     * @param sourceEntity the source entity to validate.
     * @return {@code true} if the source entity is valid, or if the configuration is set to not use validation.
     */
    public boolean isValid(EntityReference sourceEntity)
    {
        if (this.entityNameValidationConfiguration.useValidation()
            && this.entityNameValidationManager.getEntityReferenceNameStrategy() != null)
        {
            return this.entityNameValidationManager.getEntityReferenceNameStrategy().isValid(sourceEntity);
        } else {
            return true;
        }
    }

    /**
     * Validate a name according to the given name strategy.
     *
     * @param name the name to validate.
     * @param hint the hint of name strategy to use.
     * @return {@code true} if the name is valid according to the name strategy.
     */
    public boolean isValid(String name, String hint)
    {
        EntityNameValidation entityReferenceNameStrategy =
            this.entityNameValidationManager.getEntityReferenceNameStrategy(hint);
        if (entityReferenceNameStrategy != null) {
            return entityReferenceNameStrategy.isValid(name);
        } else {
            return true;
        }
    }

    /**
     * Validate a source entity according to the given name strategy.
     *
     * @param sourceEntity the source entity to validate.
     * @param hint the hint of name strategy to use.
     * @return {@code true} if the source entity is valid according to the name strategy.
     */
    public boolean isValid(EntityReference sourceEntity, String hint)
    {
        EntityNameValidation entityReferenceNameStrategy =
            this.entityNameValidationManager.getEntityReferenceNameStrategy(hint);
        if (entityReferenceNameStrategy != null) {
            return entityReferenceNameStrategy.isValid(sourceEntity);
        } else {
            return true;
        }
    }

    /**
     * @param <S> the type of the {@link ScriptService}
     * @param serviceName the name of the sub {@link ScriptService}
     * @return the {@link ScriptService} or null of none could be found
     */
    @SuppressWarnings("unchecked")
    public <S extends ScriptService> S get(String serviceName)
    {
        return (S) this.scriptServiceManager.get(ID + '.' + serviceName);
    }

}
