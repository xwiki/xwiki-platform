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

/**
 * Check if the given request contains parameters that needs conversion and perform the needing conversion.
 *
 * @version $Id$
 * @since 13.5RC1
 */
@Role
public interface RequestParameterConverter
{
    /**
     * Check if the given request needs conversion and perform those conversions.
     * This method creates a mutable request, modifies and returns it. However in case of
     * error it will return an empty optional, and it will handle directly the errors in the response.
     * See {@link #convert(ServletRequest)} for using an exception for handling the errors.
     *
     * @param request the request that might contain parameter needing conversion or an {@link Optional#empty()} in case
     *        of error
     * @param response the response used to redirect or do changes in case of conversion error
     * @return a mutable request with the converted parameters, or an empty optional in case of error
     * @throws IOException in case of problem to write an answer in the response
     */
    Optional<ServletRequest> convert(ServletRequest request, ServletResponse response) throws IOException;

    /**
     * Check if the given request needs conversion and perform those conversions.
     * This method creates a mutable request, modifies it and returns it along with the errors and output that have
     * occurred as part of the conversion, all that holds in the returned {@link RequestParameterConversionResult}.
     * Consumer of this API should always check if the obtained result contains errors or not to know if the conversion
     * properly succeeded.
     *
     * @param request the request that might contain parameter needing conversion
     * @return an instance of {@link RequestParameterConversionResult} containing the modified request and the output
     *         and errors that might have occurred
     * @since 14.10
     */
    RequestParameterConversionResult convert(ServletRequest request);
}
