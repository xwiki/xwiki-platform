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
package org.xwiki.container;

import java.io.IOException;

/**
 * Decorator for the {@link Response} interface adding the capacity for components using this interface to send
 * redirects.
 *
 * @version $Id$
 * @since 10.0
 * @deprecated merged with {@link Response}
 */
@Deprecated(since = "42.0.0")
public interface RedirectResponse extends Response
{
    /**
     * Sends a temporary redirect response to the client using the specified redirect location URL.
     *
     * @param location the redirect URL
     * @throws IOException if an error happens
     */
    void sendRedirect(String location) throws IOException;
}
