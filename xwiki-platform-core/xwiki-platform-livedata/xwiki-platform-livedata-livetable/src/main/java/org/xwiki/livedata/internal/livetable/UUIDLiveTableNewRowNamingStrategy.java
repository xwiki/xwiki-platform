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
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiException;

/**
 * {@link LiveTableNewRowNamingStrategy} that generates a page in a configured location, named using a UUID.
 *
 * @version $Id$
 * @since 18.6.0RC1
 */
@Component
@Named("uuid")
@Singleton
@Unstable
public class UUIDLiveTableNewRowNamingStrategy implements LiveTableNewRowNamingStrategy
{
    private static final String NEW_ROW_LOCATION_PARAMETER_KEY = "newRowLocation";

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @Inject
    @Named("current")
    private SpaceReferenceResolver<String> currentSpaceReferenceResolver;

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Override
    public DocumentReference generate(Map<String, Object> parameters) throws LiveDataException, XWikiException
    {
        String location = (String) parameters.get(NEW_ROW_LOCATION_PARAMETER_KEY);
        if (StringUtils.isBlank(location)) {
            throw new LiveDataException("Missing location for row creation.");
        }
        DocumentReference candidate =
            this.currentDocumentReferenceResolver.resolve(String.format("%s.%s", location, UUID.randomUUID()));
        if (this.modelBridge.exists(candidate)) {
            throw new LiveDataException(String.format("The page [%s] already exists.", candidate));
        }
        return candidate;
    }

    @Override
    public boolean isCreationAllowed(Map<String, Object> parameters)
    {
        String location = (String) parameters.get(NEW_ROW_LOCATION_PARAMETER_KEY);
        if (StringUtils.isBlank(location)) {
            return false;
        }
        return this.authorization.hasAccess(Right.EDIT, this.currentSpaceReferenceResolver.resolve(location));
    }
}
