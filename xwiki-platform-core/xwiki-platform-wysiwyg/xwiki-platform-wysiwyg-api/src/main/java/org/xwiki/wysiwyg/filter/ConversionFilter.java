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
package org.xwiki.wysiwyg.filter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.wysiwyg.converter.RequestParameterConverter;

import com.xpn.xwiki.web.Utils;

/**
 * This filter is used to convert the values of request parameters that require HTML conversion before being processed.
 * A HTML editor can use this filter to convert its output to a specific syntax before it is saved.
 * <p>
 * While the class is much older, the since annotation was moved to 17.0.0RC1 because it implement a completely
 * different API from Java point of view.
 * 
 * @version $Id$
 * @since 17.0.0RC1
 */
public class ConversionFilter implements Filter
{
    @Override
    public void destroy()
    {
        // No cleanup needed.
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
        throws IOException, ServletException
    {
        Execution execution = Utils.getComponent(Execution.class);
        execution.pushContext(new ExecutionContext());
        Optional<ServletRequest> servletRequest = Optional.of(req);
        try {
            servletRequest = convert(servletRequest, res);
        } finally {
            execution.popContext();
        }

        if (servletRequest.isPresent()) {
            chain.doFilter(servletRequest.get(), res);
        }
    }

    private Optional<ServletRequest> convert(Optional<ServletRequest> originalRequest, ServletResponse res)
        throws IOException
    {
        List<RequestParameterConverter> converters = Utils.getComponentList(RequestParameterConverter.class);
        Iterator<RequestParameterConverter> iterator = converters.iterator();
        Optional<ServletRequest> convertedRequest = originalRequest;
        while (convertedRequest.isPresent() && iterator.hasNext()) {
            RequestParameterConverter converter = iterator.next();
            convertedRequest = converter.convert(convertedRequest.get(), res);
        }
        return convertedRequest;
    }

    @Override
    public void init(FilterConfig config) throws ServletException
    {
        // No initialization needed.
    }
}
