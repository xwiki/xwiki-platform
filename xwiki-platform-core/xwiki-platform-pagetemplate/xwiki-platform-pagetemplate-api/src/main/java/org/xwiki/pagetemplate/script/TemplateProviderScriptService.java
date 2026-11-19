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
package org.xwiki.pagetemplate.script;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.pagetemplate.TemplateProvider;
import org.xwiki.pagetemplate.TemplateProviderException;
import org.xwiki.pagetemplate.TemplateProviderManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

/**
 * Scripting APIs for the template providers.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Named("templateProvider")
@Singleton
@Unstable
public class TemplateProviderScriptService implements ScriptService
{
    @Inject
    private TemplateProviderManager templateProviderManager;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private Logger logger;

    /**
     * @return the template providers of the current wiki that the current user can view, ordered by the full name of
     *     their document, or an empty list when they cannot be loaded
     */
    public List<TemplateProvider> getTemplateProviders()
    {
        try {
            return this.templateProviderManager.getTemplateProviders().stream()
                .filter(provider -> this.authorization.hasAccess(Right.VIEW, provider.getReference()))
                .toList();
        } catch (TemplateProviderException e) {
            this.logger.warn("Failed to list the template providers. Root cause is [{}].",
                ExceptionUtils.getRootCauseMessage(e));
            return List.of();
        }
    }
}
