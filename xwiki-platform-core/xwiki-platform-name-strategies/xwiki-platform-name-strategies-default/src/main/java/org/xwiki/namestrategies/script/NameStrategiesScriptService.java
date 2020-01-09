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
package org.xwiki.namestrategies.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.namestrategies.EntityReferenceNameStrategy;
import org.xwiki.namestrategies.NameStrategyConfiguration;
import org.xwiki.namestrategies.EntityReferenceNameStrategyManager;
import org.xwiki.namestrategies.internal.ReplaceCharacterNameStrategyConfiguration;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

/**
 * Script service for name strategies.
 *
 * @version $Id$
 * @since 12.0RC1
 */
@Component
@Named("namestrategies")
@Singleton
@Unstable
public class NameStrategiesScriptService implements ScriptService
{
    @Inject
    private EntityReferenceNameStrategyManager entityReferenceNameStrategyManager;

    @Inject
    private NameStrategyConfiguration nameStrategyConfiguration;

    @Inject
    private ReplaceCharacterNameStrategyConfiguration replaceCharacterNameStrategyConfiguration;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    /**
     * Return the name strategy manager if the user has programming rights only.
     *
     * @return the manager for name strategies or null.
     */
    public EntityReferenceNameStrategyManager getManager()
    {
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            return this.entityReferenceNameStrategyManager;
        } else {
            return null;
        }
    }

    /**
     * Return the name strategy configuration if the user has programming rights only.
     *
     * @return the configuration for name strategies or null.
     */
    public NameStrategyConfiguration getConfiguration()
    {
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            return this.nameStrategyConfiguration;
        } else {
            return null;
        }
    }

    /**
     * @return the configuration for {@link org.xwiki.namestrategies.internal.ReplaceCharacterNameStrategy}.
     */
    public ReplaceCharacterNameStrategyConfiguration getReplaceCharacterNameStrategyConfiguration()
    {
        if (this.authorizationManager.hasAccess(Right.PROGRAM)) {
            return this.replaceCharacterNameStrategyConfiguration;
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
        if (this.nameStrategyConfiguration.useTransformation()
            && this.entityReferenceNameStrategyManager.getEntityReferenceNameStrategy() != null) {
            return this.entityReferenceNameStrategyManager.getEntityReferenceNameStrategy().transform(name);
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
        if (this.nameStrategyConfiguration.useTransformation()
            && this.entityReferenceNameStrategyManager.getEntityReferenceNameStrategy() != null) {
            return this.entityReferenceNameStrategyManager.getEntityReferenceNameStrategy().transform(sourceEntity);
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
        EntityReferenceNameStrategy entityReferenceNameStrategy =
            this.entityReferenceNameStrategyManager.getEntityReferenceNameStrategy(hint);
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
        EntityReferenceNameStrategy entityReferenceNameStrategy =
            this.entityReferenceNameStrategyManager.getEntityReferenceNameStrategy(hint);
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
        if (this.nameStrategyConfiguration.useValidation()
            && this.entityReferenceNameStrategyManager.getEntityReferenceNameStrategy() != null) {
            return this.entityReferenceNameStrategyManager.getEntityReferenceNameStrategy().isValid(name);
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
        if (this.nameStrategyConfiguration.useValidation()
            && this.entityReferenceNameStrategyManager.getEntityReferenceNameStrategy() != null) {
            return this.entityReferenceNameStrategyManager.getEntityReferenceNameStrategy().isValid(sourceEntity);
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
        EntityReferenceNameStrategy entityReferenceNameStrategy =
            this.entityReferenceNameStrategyManager.getEntityReferenceNameStrategy(hint);
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
        EntityReferenceNameStrategy entityReferenceNameStrategy =
            this.entityReferenceNameStrategyManager.getEntityReferenceNameStrategy(hint);
        if (entityReferenceNameStrategy != null) {
            return entityReferenceNameStrategy.isValid(sourceEntity);
        } else {
            return true;
        }
    }
}
