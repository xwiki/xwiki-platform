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
package org.xwiki.rendering.async.internal;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.stream.Collectors;

/**
 * Used as return value for {@link AsyncRendererExecutor#render(AsyncRenderer, java.util.Set)}.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
public class AsyncRendererExecutorResponse
{
    private final AsyncRendererJobStatus status;

    private final Long asyncClientId;

    /**
     * @param status the status of the execution
     */
    public AsyncRendererExecutorResponse(AsyncRendererJobStatus status)
    {
        this.status = status;
        this.asyncClientId = null;
    }

    /**
     * @param status the status of the execution
     * @param clientId the generated client identifier
     */
    public AsyncRendererExecutorResponse(AsyncRendererJobStatus status, long clientId)
    {
        this.status = status;
        this.asyncClientId = clientId;
    }

    /**
     * @return the status of the execution
     */
    public AsyncRendererJobStatus getStatus()
    {
        return this.status;
    }

    /**
     * @return the generated client identifier
     */
    public Long getAsyncClientId()
    {
        return this.asyncClientId;
    }

    /**
     * @return the value to set in the {@code "data-xwiki-async-id"} parameter
     */
    public String getJobIdHTTPPath()
    {
        return getStatus().getRequest().getId().stream().map(this::encodeURL).collect(Collectors.joining("/"));
    }

    private String encodeURL(String element)
    {
        try {
            return URLEncoder.encode(element, "UTF8");
        } catch (UnsupportedEncodingException e) {
            // If Java does not support UTF8 we probably won't reach this point anyway
            throw new RuntimeException("UTF8 encoding is not supported", e);
        }
    }
}
