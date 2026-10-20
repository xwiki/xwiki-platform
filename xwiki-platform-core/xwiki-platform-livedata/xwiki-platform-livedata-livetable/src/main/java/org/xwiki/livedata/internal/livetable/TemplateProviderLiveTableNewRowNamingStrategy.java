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
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategy;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.pagetemplate.TemplateProvider;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiException;

/**
 * {@link LiveTableNewRowNamingStrategy} that names new entries after their title, and creates them where and how a
 * template provider says, like AppWithinMinutes does. New entries go in the first space the template provider
 * restricts creation to, or else in {@code newRowLocation}, or else in the space of the page containing the live
 * data. They are terminal pages or nested pages as the template provider says, and terminal pages without one.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Named(TemplateProviderLiveTableNewRowNamingStrategy.HINT)
@Singleton
public class TemplateProviderLiveTableNewRowNamingStrategy extends TitleLiveTableNewRowNamingStrategy
{
    /**
     * The hint of this strategy.
     */
    public static final String HINT = "templateProvider";

    private static final String NEW_ROW_LOCATION_PARAMETER = "newRowLocation";

    private static final String CURRENT = "current";

    @Inject
    private NewRowTemplateProviderResolver templateProviderResolver;

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private EntityReferenceProvider entityReferenceProvider;

    @Inject
    @Named(CURRENT)
    private SpaceReferenceResolver<String> currentSpaceReferenceResolver;

    @Inject
    @Named(CURRENT)
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private Logger logger;

    @Override
    public DocumentReference generate(Map<String, Object> parameters, Map<String, Object> entryValues)
        throws LiveDataException, XWikiException
    {
        SpaceReference location = getRequiredLocation(parameters);
        boolean terminal =
            this.templateProviderResolver.resolve(parameters).map(TemplateProvider::isTerminal).orElse(true);
        String name = getName(entryValues);
        String candidateName = name;
        // The title is what the user typed, so two entries can legitimately want the same name. The extra entries get
        // a suffix rather than a failure.
        for (int suffix = 1; isTaken(candidateName, location); suffix++) {
            candidateName = String.format("%s-%d", name, suffix);
        }
        return getReference(candidateName, location, terminal);
    }

    @Override
    public boolean isCreationAllowed(Map<String, Object> parameters)
    {
        try {
            return getLocation(parameters).map(location -> this.authorization.hasAccess(Right.EDIT, location))
                .orElse(false);
        } catch (LiveDataException e) {
            this.logger.warn("Adding entries is disabled because the template provider failed to load: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return false;
        }
    }

    @Override
    public Optional<SpaceReference> getNewEntryLocation(Map<String, Object> parameters)
    {
        try {
            return getLocation(parameters);
        } catch (LiveDataException e) {
            this.logger.warn("Failed to find where new entries are created: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return Optional.empty();
        }
    }

    @Override
    protected SpaceReference getRequiredLocation(Map<String, Object> parameters) throws LiveDataException
    {
        return getLocation(parameters)
            .orElseThrow(() -> new LiveDataException("Missing location for row creation."));
    }

    /**
     * Finds where new entries go: the first space the template provider restricts creation to, or else
     * {@code newRowLocation}, or else the space of the page containing the live data.
     *
     * @param parameters the live data source parameters
     * @return the location of the new entries, or {@link Optional#empty()} when there is none
     * @throws LiveDataException if the template provider cannot be loaded
     */
    private Optional<SpaceReference> getLocation(Map<String, Object> parameters) throws LiveDataException
    {
        Optional<SpaceReference> providerLocation = this.templateProviderResolver.resolve(parameters)
            .flatMap(templateProvider -> templateProvider.getCreationRestrictions().stream().findFirst());
        if (providerLocation.isPresent()) {
            return providerLocation;
        }

        String location = (String) parameters.get(NEW_ROW_LOCATION_PARAMETER);
        if (StringUtils.isNotBlank(location)) {
            return Optional.of(this.currentSpaceReferenceResolver.resolve(location));
        }

        String hostDocument = (String) parameters.get(LiveTableRequestHandler.HOST_DOCUMENT);
        if (StringUtils.isNotBlank(hostDocument)) {
            DocumentReference hostReference = this.currentDocumentReferenceResolver.resolve(hostDocument);
            return Optional.of(hostReference.getLastSpaceReference());
        }

        return Optional.empty();
    }

    // A terminal page and the home page of a space with the same name display the same, so neither form can be used
    // when the other exists.
    private boolean isTaken(String name, SpaceReference location) throws XWikiException
    {
        return this.modelBridge.exists(getReference(name, location, true))
            || this.modelBridge.exists(getReference(name, location, false));
    }

    private DocumentReference getReference(String name, SpaceReference location, boolean terminal)
    {
        if (terminal) {
            return new DocumentReference(name, location);
        }
        String defaultPageName = this.entityReferenceProvider.getDefaultReference(EntityType.DOCUMENT).getName();
        return new DocumentReference(defaultPageName, new SpaceReference(name, location));
    }
}
