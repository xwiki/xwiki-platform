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
package org.xwiki.captcha.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.SerializeResourceReferenceException;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

/**
 * Converts a {@link JCaptchaResourceReference} into a relative {@link ExtendedURL} (with the Context Path added).
 *
 * @since 11.10
 * @version $Id$
 */
@Component
@Singleton
public class JCaptchaResourceReferenceSerializer implements
    ResourceReferenceSerializer<JCaptchaResourceReference, ExtendedURL>
{
    @Inject
    @Named("contextpath")
    private URLNormalizer<ExtendedURL> extendedURLNormalizer;

    @Override
    public ExtendedURL serialize(JCaptchaResourceReference resource)
        throws SerializeResourceReferenceException, UnsupportedResourceReferenceException
    {
        List<String> segments = new ArrayList<>();

        // Add the resource type segment.
        segments.add("jcaptcha");
        segments.add(resource.getCaptchaType());
        segments.add(resource.getEngine());

        // Add all optional parameters
        ExtendedURL extendedURL = new ExtendedURL(segments, resource.getParameters());

        // Normalize the URL to add the Context Path since we want a full relative URL to be returned.
        return this.extendedURLNormalizer.normalize(extendedURL);
    }
}
