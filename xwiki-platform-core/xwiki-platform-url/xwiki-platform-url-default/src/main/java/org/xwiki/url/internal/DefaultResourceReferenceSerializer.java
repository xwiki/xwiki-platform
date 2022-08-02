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
package org.xwiki.url.internal;

import java.lang.reflect.ParameterizedType;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLContextManager;

/**
 * Delegates the work to the Resource Reference Serializer specified in the XWiki Configuration
 * (see {@link org.xwiki.url.URLConfiguration#getURLFormatId()}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Singleton
public class DefaultResourceReferenceSerializer implements ResourceReferenceSerializer<ResourceReference, ExtendedURL>
{
    @Inject
    private URLContextManager urlContextManager;

    /**
     * Used to lookup the correct {@link org.xwiki.resource.ResourceReferenceSerializer} component.
     */
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Override
    public ExtendedURL serialize(ResourceReference resource)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        ResourceReferenceSerializer<ResourceReference, ExtendedURL> serializer;
        ParameterizedType type = new DefaultParameterizedType(null, ResourceReferenceSerializer.class,
            ResourceReference.class, ExtendedURL.class);
        try {
            serializer = this.componentManager.getInstance(type, this.urlContextManager.getURLFormatId());
        } catch (ComponentLookupException e) {
            throw new UnsupportedResourceReferenceException(
                String.format("Invalid URL format id [%s]. Cannot serialize Resource Reference [%s].",
                    this.urlContextManager.getURLFormatId(), resource), e);
        }

        return serializer.serialize(resource);
    }
}
