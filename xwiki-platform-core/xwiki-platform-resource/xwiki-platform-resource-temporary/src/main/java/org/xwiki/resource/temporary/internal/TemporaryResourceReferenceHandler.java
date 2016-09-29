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
package org.xwiki.resource.temporary.internal;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.servlet.AbstractServletResourceReferenceHandler;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.resource.temporary.TemporaryResourceStore;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Resolves and return the temporary resource ({@link org.xwiki.resource.temporary.TemporaryResourceReference}) in the
 * Servlet Output Stream.
 *
 * @version $Id$
 * @since 6.1M1
 */
@Component
@Named("tmp")
@Singleton
public class TemporaryResourceReferenceHandler
    extends AbstractServletResourceReferenceHandler<TemporaryResourceReference>
{
    @Inject
    private TemporaryResourceStore store;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Override
    public List<ResourceType> getSupportedResourceReferences()
    {
        return Arrays.asList(TemporaryResourceReference.TYPE);
    }

    @Override
    protected boolean isResourceAccessible(TemporaryResourceReference resourceReference)
    {
        return this.authorization.hasAccess(Right.VIEW, resourceReference.getOwningEntityReference());
    }

    @Override
    protected InputStream getResourceStream(TemporaryResourceReference resourceReference)
    {
        try {
            return new FileInputStream(this.store.getTemporaryFile(resourceReference));
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected String getResourceName(TemporaryResourceReference resourceReference)
    {
        return resourceReference.getResourceName();
    }
}
