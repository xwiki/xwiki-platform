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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

/**
 * Serializes a {@link TemporaryResourceReference} as an {@link ExtendedURL}. The following URL format is used
 * {@code http://<server>/<context>/tmp/<module id>/<owning entity reference>/<module-dependent resource path>}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("standard/tmp")
@Singleton
public class ExtendedURLTemporaryResourceReferenceSerializer
    implements ResourceReferenceSerializer<TemporaryResourceReference, ExtendedURL>
{
    @Inject
    @Named("contextpath")
    private URLNormalizer<ExtendedURL> extendedURLNormalizer;

    @Inject
    @Named("url")
    private EntityReferenceSerializer<String> urlEntityReferenceSerializer;

    @Override
    public ExtendedURL serialize(TemporaryResourceReference reference)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        List<String> segments = new LinkedList<String>();
        segments.add("tmp");
        segments.add(reference.getModuleId());
        segments.add(serialize(reference.getOwningEntityReference()));
        segments.addAll(reference.getResourcePath());
        // A modifiable map is used here so parameters can be added to the URL later.
        Map<String, List<String>> parameters = new HashMap<String, List<String>>(reference.getParameters());
        ExtendedURL result = new ExtendedURL(segments, parameters);
        return this.extendedURLNormalizer.normalize(result);
    }

    private String serialize(EntityReference reference)
    {
        return reference.getType().toString().toLowerCase() + ':'
            + this.urlEntityReferenceSerializer.serialize(reference);
    }
}
