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
package org.xwiki.livedata.internal.livetable;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.internal.web.PageTemplateRequiredRightsChecker;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.pagetemplate.TemplateProvider;
import org.xwiki.pagetemplate.TemplateProviderException;
import org.xwiki.pagetemplate.TemplateProviderManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Finds the template provider new entries are created from.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component(roles = NewRowTemplateProviderResolver.class)
@Singleton
public class NewRowTemplateProviderResolver
{
    private static final String TEMPLATE_PROVIDER_PARAMETER = "newRowTemplateProvider";

    private static final String CLASS_NAME_PARAMETER = "className";

    /**
     * Disables the template provider.
     */
    private static final LocalDocumentReference NO_TEMPLATE_PROVIDER =
        new LocalDocumentReference("XWiki", "NoTemplateProvider");

    @Inject
    private TemplateProviderManager templateProviderManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private PageTemplateRequiredRightsChecker requiredRightsChecker;

    @Inject
    private Logger logger;

    /**
     * Finds the template provider of a live data: the one given by {@code newRowTemplateProvider}, or else the one
     * named after the class, the way AppWithinMinutes names it ({@code Space.MyClass} gives
     * {@code Space.MyTemplateProvider}).
     *
     * @param parameters the live data source parameters
     * @return the template provider, or {@link Optional#empty()} when there is none, it is disabled with
     *         {@code newRowTemplateProvider=XWiki.NoTemplateProvider}, or the current user cannot view it
     * @throws LiveDataException if the template provider cannot be loaded
     */
    public Optional<TemplateProvider> resolve(Map<String, Object> parameters) throws LiveDataException
    {
        String explicitReference = (String) parameters.get(TEMPLATE_PROVIDER_PARAMETER);
        DocumentReference reference;
        if (StringUtils.isNotBlank(explicitReference)) {
            reference = this.currentDocumentReferenceResolver.resolve(explicitReference);
            if (NO_TEMPLATE_PROVIDER.equals(reference.getLocalDocumentReference())) {
                return Optional.empty();
            }
        } else {
            String className = (String) parameters.get(CLASS_NAME_PARAMETER);
            if (StringUtils.isBlank(className)) {
                return Optional.empty();
            }
            DocumentReference classReference = this.currentDocumentReferenceResolver.resolve(className);
            reference = new DocumentReference(Strings.CS.removeEnd(classReference.getName(), "Class")
                + "TemplateProvider", classReference.getLastSpaceReference());
        }

        if (!this.authorization.hasAccess(Right.VIEW, reference)) {
            return Optional.empty();
        }

        Optional<TemplateProvider> templateProvider;
        try {
            templateProvider = this.templateProviderManager.getTemplateProvider(reference);
        } catch (TemplateProviderException e) {
            throw new LiveDataException(String.format("Failed to load the template provider [%s].", reference), e);
        }
        if (templateProvider.isEmpty() && StringUtils.isNotBlank(explicitReference)) {
            this.logger.warn("New entries are created without template because [{}] is not a template provider.",
                reference);
        }
        return templateProvider;
    }

    /**
     * Finds the template new entries are created from.
     *
     * @param parameters the live data source parameters
     * @return the template of the template provider, or {@link Optional#empty()} when there is none or the current
     *         user cannot view it
     * @throws LiveDataException if the template provider cannot be loaded
     */
    public Optional<DocumentReference> getTemplate(Map<String, Object> parameters) throws LiveDataException
    {
        return resolve(parameters).flatMap(TemplateProvider::getTemplateReference)
            .filter(template -> this.authorization.hasAccess(Right.VIEW, template));
    }

    /**
     * Checks whether the current user has the required rights of the template where a new entry is created.
     *
     * @param parameters the live data source parameters
     * @param target the new entry, or the space it is created in
     * @return {@code true} if the user has those rights, or there is no template
     * @throws LiveDataException if the template provider cannot be loaded
     */
    public boolean hasRequiredRights(Map<String, Object> parameters, EntityReference target)
        throws LiveDataException
    {
        return getTemplate(parameters).map(template -> this.requiredRightsChecker.hasRequiredRights(template, target))
            .orElse(true);
    }
}
