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
package org.xwiki.rest.internal.url.resources;

import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.resources.spaces.SpaceResource;

/**
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Singleton
public class SpaceRestURLGenerator extends AbstractEntityRestURLGenerator<SpaceReference>
{
    @Override
    public URL getURL(SpaceReference reference) throws XWikiRestException
    {
        // The idea is to use the UriBuilder of jax-rs to generate URLs that match the resources paths.
        // So it is consistent.
        try {
            return Utils.createURI(getBaseURI(), SpaceResource.class, reference.getWikiReference().getName(),
                getRestSpaceList(reference)).toURL();
        } catch (MalformedURLException e) {
            throw new XWikiRestException(String.format("Failed to generate a REST URL for the space [%s].", reference),
                e);
        }
    }
}
