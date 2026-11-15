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
package org.xwiki.export;

import org.xwiki.component.annotation.Role;
import org.xwiki.container.Request;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Handles an export format for the export action. The component hint is the value of the {@code format} request
 * parameter the handler is used for, {@code xar} being the default format (used when the request doesn't specify the
 * format). A handler takes precedence over the export action, so it can add a new export format or overwrite an
 * export format supported natively by the export action ({@code xar}, {@code html}, {@code pdf} or one of the office
 * formats).
 *
 * @version $Id$
 * @since 18.4.7
 * @since 18.9.0RC1
 */
@Role
@Unstable
public interface ExportFormatHandler
{
    /**
     * Checks if the export request is valid, i.e. if the user intended to perform the export. An export that consumes
     * a lot of resources should be performed only when the user's intent is clear, i.e. when the user submits a form,
     * and not when a GET request is made, e.g. by a resource loaded with a relative URL from a page served by the
     * export action. That's why the default implementation accepts only POST requests. An export format that only
     * renders a page, like a regular view request, can accept GET requests. Note that resources loaded with relative
     * URLs usually don't specify the export format, so a handler for the default {@code xar} format should not accept
     * GET requests.
     * <p>
     * When the export request is not valid, the export action responds with the 405 (Method Not Allowed) status code
     * and offers the user to perform the same export again with a POST request.
     *
     * @param request the export request to validate
     * @return {@code true} if the export request is valid, {@code false} otherwise
     */
    default boolean validate(Request request)
    {
        return request instanceof ServletRequest servletRequest
            && "POST".equalsIgnoreCase(servletRequest.getRequest().getMethod());
    }

    /**
     * Handles the export request. The requested document revision, if any, is already set on the given context.
     *
     * @param context the XWiki context
     * @return the name of the template to render, or {@code null} if the response has been written already
     * @throws XWikiException if the export fails
     */
    String export(XWikiContext context) throws XWikiException;
}
