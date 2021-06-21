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
package org.xwiki.wysiwyg.converter;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Check if the given request contains parameters that needs conversion and perform the needing conversion.
 *
 * @version $Id$
 * @since 13.5RC1
 */
@Role
@Unstable
public interface RequestParameterConverter
{
    /**
     * Check if the given request needs conversion and perform those conversions.
     * This method is supposed to create a mutable request and to modify and returns that one. However in case of
     * error it won't return the modified request, but it will handle directly the errors in the response.
     *
     * @param request the request that might contain parameter needing conversion
     * @param response the response used to redirect or do changes in case of conversion error
     * @return a mutable request with the converted parameters, or an empty optional in case of error
     * @throws IOException in case of problem to write an answer in the response
     */
    Optional<ServletRequest> convert(ServletRequest request, ServletResponse response) throws IOException;
}
