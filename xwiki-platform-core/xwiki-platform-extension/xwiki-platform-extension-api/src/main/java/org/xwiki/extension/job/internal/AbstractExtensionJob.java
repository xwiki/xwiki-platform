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
package org.xwiki.extension.job.internal;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.extension.job.ExtensionRequest;

/**
 * Base class for any Job dealing with extensions.
 * 
 * @param <R> the type of the request
 * @version $Id$
 */
public abstract class AbstractExtensionJob<R extends ExtensionRequest> extends AbstractJob<R>
{
    /**
     * @see #getExtraHandlerParameters()
     */
    private Map<String, Object> extra;

    /**
     * @return extra parameters used in {@link org.xwiki.extension.handler.ExtensionHandler} methods
     */
    protected Map<String, ? > getExtraHandlerParameters()
    {
        if (this.extra == null) {
            this.extra = new HashMap<String, Object>();

            R request = getRequest();
            for (String key : request.getPropertyNames()) {
                this.extra.put(key, request.getProperty(key));
            }
        }

        return this.extra;
    }
}
