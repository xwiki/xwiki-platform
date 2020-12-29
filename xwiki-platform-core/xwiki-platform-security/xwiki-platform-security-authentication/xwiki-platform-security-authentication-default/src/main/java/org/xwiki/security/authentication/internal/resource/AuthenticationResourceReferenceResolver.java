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
package org.xwiki.security.authentication.internal.resource;

import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.security.authentication.api.AuthenticationResourceReference;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.internal.AbstractResourceReferenceResolver;

/**
 * Default resolver for {@link AuthenticationResourceReference}.
 * This resolver looks for the {@link AuthenticationResourceReference.AuthenticationAction} request parameter and build
 * a {@link AuthenticationResourceReference} with it.
 *
 * @version $Id$
 * @since 13.0RC1
 */
@Component
@Named(AuthenticationResourceReference.RESOURCE_TYPE_ID)
@Singleton
public class AuthenticationResourceReferenceResolver extends AbstractResourceReferenceResolver
{
    @Override
    public AuthenticationResourceReference resolve(ExtendedURL representation, ResourceType resourceType,
        Map<String, Object> parameters) throws CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        AuthenticationResourceReference result;
        List<String> segments = representation.getSegments();
        if (segments.size() == 1) {
            String actionName = segments.get(0);
            try {
                AuthenticationResourceReference.AuthenticationAction authenticationAction =
                    AuthenticationResourceReference.AuthenticationAction.getFromRequestParameter(actionName);
                result = new AuthenticationResourceReference(authenticationAction);
                copyParameters(representation, result);
                return result;
            } catch (IllegalArgumentException e) {
                throw new CreateResourceReferenceException(
                    String.format("Cannot find an authentication action for name [%s]", actionName));
            }
        } else {
            throw new CreateResourceReferenceException(
                String.format("Invalid Authentication URL format: [%s]", resourceType.toString()));
        }
    }
}
