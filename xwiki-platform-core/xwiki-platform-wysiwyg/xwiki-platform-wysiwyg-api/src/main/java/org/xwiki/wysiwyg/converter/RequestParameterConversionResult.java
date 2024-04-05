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

import java.util.LinkedHashMap;
import java.util.Map;

import org.xwiki.wysiwyg.filter.MutableServletRequest;

/**
 * Simple POJO holding the result of a conversion performed with {@link RequestParameterConverter}.
 * More specifically this class contains a mutable request, resulting of the conversion, a map of errors that might have
 * occurred during the conversion for each parameter, and a map of the output of the conversion for each parameter.
 *
 * @version $Id$
 * @since 14.10
 */
public class RequestParameterConversionResult
{
    private MutableServletRequest request;
    private Map<String, Throwable> errors;
    private Map<String, String> output;

    /**
     * Default constructor.
     *
     * @param request a mutable copy of the original request used for the conversion
     */
    public RequestParameterConversionResult(MutableServletRequest request)
    {
        this.request = request;
        this.errors = new LinkedHashMap<>();
        this.output = new LinkedHashMap<>();
    }

    /**
     * @return the mutable request
     */
    public MutableServletRequest getRequest()
    {
        return request;
    }

    /**
     * @return the map of errors indexed by parameters
     */
    public Map<String, Throwable> getErrors()
    {
        return errors;
    }

    /**
     * @return the map of conversion output indexed by parameters
     */
    public Map<String, String> getOutput()
    {
        return output;
    }
}
