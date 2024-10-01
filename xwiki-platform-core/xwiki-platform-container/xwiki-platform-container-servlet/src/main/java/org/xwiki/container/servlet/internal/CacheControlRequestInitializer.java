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
package org.xwiki.container.servlet.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.cache.CacheControl;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Request;
import org.xwiki.container.RequestInitializer;
import org.xwiki.container.RequestInitializerException;
import org.xwiki.container.servlet.HttpServletUtils;
import org.xwiki.container.servlet.ServletRequest;

/**
 * Configure the {@link CacheControl} based on the input request.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@Component
@Singleton
public class CacheControlRequestInitializer implements RequestInitializer
{
    @Inject
    private CacheControl cacheControl;

    @Override
    public void initialize(Request request) throws RequestInitializerException
    {
        // Indicate if the request explicitly disable getting resources from the cache
        if (request instanceof ServletRequest servletRequest
            && !HttpServletUtils.isCacheReadAllowed(servletRequest.getJakartaHttpServletRequest())) {
            this.cacheControl.setCacheReadAllowed(false);
        }
    }
}
