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
package com.xpn.xwiki.internal.template;

import java.net.URL;

import org.apache.commons.lang3.NotImplementedException;
import org.xwiki.filter.input.DefaultURLInputSource;
import org.xwiki.filter.input.URLInputSource;

import com.xpn.xwiki.internal.skin.AbstractInputSourceResource;

/**
 * @version $Id$
 * @since 8.3RC1
 */
public class ClassloaderResource extends AbstractInputSourceResource<URLInputSource>
{
    /**
     * @param url the URL of the resource
     * @param resourceName the name of the resource
     */
    public ClassloaderResource(URL url, String resourceName)
    {
        super(url.toExternalForm(), resourceName, null, new DefaultURLInputSource(url));
    }

    @Override
    public String getURL(boolean forceSkinAction) throws Exception
    {
        // Does not make any sense in this case
        throw new NotImplementedException("Method not implemented");
    }
}
