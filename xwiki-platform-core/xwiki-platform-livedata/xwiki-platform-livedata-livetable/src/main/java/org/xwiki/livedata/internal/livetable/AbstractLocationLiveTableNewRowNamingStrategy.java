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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategy;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Base class for the {@link LiveTableNewRowNamingStrategy} implementations that create their entries in a given
 * space, by default the one given by the {@code newRowLocation} source parameter.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public abstract class AbstractLocationLiveTableNewRowNamingStrategy implements LiveTableNewRowNamingStrategy
{
    private static final String NEW_ROW_LOCATION_PARAMETER_KEY = "newRowLocation";

    private static final String DESCRIPTION_TRANSLATION_KEY = "livedata.liveTable.newRowNamingStrategy.%s.description";

    @Inject
    @Named("current")
    private SpaceReferenceResolver<String> currentSpaceReferenceResolver;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private Logger logger;

    @Inject
    private ContextualLocalizationManager l10n;

    @Override
    public boolean isCreationAllowed(Map<String, Object> parameters)
    {
        try {
            return getLocation(parameters)
                .map(location -> this.authorization.hasAccess(Right.EDIT, location))
                .orElse(false);
        } catch (LiveDataException e) {
            this.logger.warn("Adding entries is disabled because their location could not be resolved: [{}]",
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

    /**
     * @param hint the hint of the strategy
     * @return the translated description of the strategy with the given hint, or {@code null} when it has none
     */
    protected String getDescription(String hint)
    {
        return this.l10n.getTranslationPlain(String.format(DESCRIPTION_TRANSLATION_KEY, hint));
    }

    /**
     * @param parameters the live data source parameters
     * @return the space the new entries are created in
     * @throws LiveDataException when the location is not configured
     */
    protected SpaceReference getRequiredLocation(Map<String, Object> parameters) throws LiveDataException
    {
        return getLocation(parameters)
            .orElseThrow(() -> new LiveDataException("Missing location for row creation."));
    }

    /**
     * Finds where new entries go. This implementation reads the {@code newRowLocation} source parameter.
     *
     * @param parameters the live data source parameters
     * @return the location of the new entries, or {@link Optional#empty()} when there is none
     * @throws LiveDataException when the location cannot be resolved
     */
    protected Optional<SpaceReference> getLocation(Map<String, Object> parameters) throws LiveDataException
    {
        String location = (String) parameters.get(NEW_ROW_LOCATION_PARAMETER_KEY);
        if (StringUtils.isBlank(location)) {
            return Optional.empty();
        }
        return Optional.of(this.currentSpaceReferenceResolver.resolve(location));
    }
}
