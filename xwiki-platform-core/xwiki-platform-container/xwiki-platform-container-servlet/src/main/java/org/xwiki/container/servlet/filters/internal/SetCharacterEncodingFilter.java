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
package org.xwiki.container.servlet.filters.internal;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * <p>
 * Filter that sets the character encoding to be used in parsing the incoming request,
 * either unconditionally or only if the client did not specify a character encoding. Further, the default encoding of
 * the response is set to the same character encoding. Configuration of this filter is based on the following
 * initialization parameters:
 * </p>
 * <ul>
 * <li><strong>encoding</strong> - The character encoding to be configured for this request,
 * either conditionally or unconditionally based on the <code>ignore</code> initialization
 * parameter. This same encoding is also used as default for the response. This parameter is required, so there is no
 * default.</li>
 * <li><strong>ignore</strong> - If set to "true", any character encoding specified by the client
 * is ignored, and the value returned by the <code>selectEncoding()</code> method is set. If set
 * to "false, <code>selectEncoding()</code> is called <strong>only</strong> if the client has not
 * already specified an encoding. By default, this parameter is set to "true".</li>
 * </ul>
 * <p>
 * Although this filter can be used unchanged, it is also easy to subclass it and make the
 * <code>selectEncoding()</code> method more intelligent about what encoding to choose, based on
 * characteristics of the incoming request (such as the values of the <code>Accept-Language</code>
 * and <code>User-Agent</code> headers, or a value stashed in the current user's session.
 * </p>
 * 
 * @author Craig McClanahan
 * @version $Id$
 */
public class SetCharacterEncodingFilter implements Filter
{

    // ----------------------------------------------------- Instance Variables

    /**
     * The default character encoding to set for requests that pass through this filter.
     */
    protected String encoding = null;

    /**
     * The filter configuration object we are associated with. If this value is null, this filter
     * instance is not currently configured.
     */
    protected FilterConfig filterConfig = null;

    /**
     * Should a character encoding specified by the client be ignored?
     */
    protected boolean ignore = true;

    // --------------------------------------------------------- Public Methods

    /**
     * Take this filter out of service.
     */
    @Override
    public void destroy()
    {
        this.encoding = null;
        this.filterConfig = null;
    }

    /**
     * Select and set (if specified) the character encoding to be used to interpret request
     * parameters for this request.
     * 
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param chain The filter chain we are processing
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        // Conditionally select and set the character encoding to be used
        if (ignore || (request.getCharacterEncoding() == null)) {
            String selectedEncoding = selectEncoding(request);
            if (selectedEncoding != null) {
                request.setCharacterEncoding(selectedEncoding);
            }
        }
        // Set the default encoding for the response. Use the same encoding as the request as this is supposed to be
        // the encoding that XWiki uses in general. It is not clear that XWiki in general supports any encoding besides
        // UTF-8, though.
        response.setCharacterEncoding(this.encoding);
        // Pass control on to the next filter
        chain.doFilter(request, response);
    }

    /**
     * Place this filter into service.
     * 
     * @param filterConfig The filter configuration object
     */
    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        this.filterConfig = filterConfig;
        this.encoding = filterConfig.getInitParameter("encoding");
        String value = filterConfig.getInitParameter("ignore");
        if (value == null) {
            this.ignore = true;
        } else if (value.equalsIgnoreCase("true")) {
            this.ignore = true;
        } else if (value.equalsIgnoreCase("yes")) {
            this.ignore = true;
        } else {
            this.ignore = false;
        }

    }

    // ------------------------------------------------------ Protected Methods

    /**
     * Select an appropriate character encoding to be used, based on the characteristics of the
     * current request and/or filter initialization parameters. If no character encoding should be
     * set, return <code>null</code>.
     * <p>
     * The default implementation unconditionally returns the value configured by the
     * <strong>encoding</strong> initialization parameter for this filter.
     * 
     * @param request The servlet request we are processing
     */
    protected String selectEncoding(ServletRequest request)
    {
        return (this.encoding);
    }
}
