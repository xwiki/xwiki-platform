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
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.container.servlet.filters.SavedRequestManager;
import org.xwiki.container.servlet.filters.SavedRequestManager.SavedRequest;

/**
 * <p>
 * A filter that allows requests to be saved and reused later. For example when the current request contains an expired
 * authentication token, and the authorization module redirects to the login page, all the information sent by the
 * client would be lost; this filter allows to save all that information, and after a successful login, injects the
 * saved data in the new request.
 * </p>
 * <p>
 * The saved data is used as a fallback for the new request, meaning that a parameter value is first searched in the new
 * request, and only if not found it is searched in the saved request. Only the request parameters are stored, along
 * with the request URL needed to verify that the request is reused only in a compatible future request. Multiple
 * requests can be stored, each one identified by a distinct ID. A request is restored only if a valid ID was provided
 * in the new URL, and if the new URL matches the URL of the saved request (except the query string). A saved session is
 * deleted after it is restored, so it cannot be reused more than once.
 * </p>
 * <p>
 * Request data is stored in the current HTTP session, in order to provide a safe temporary storage. The data is only as
 * safe as a session is, and it will not be available after the session is invalidated. Another consequence is that only
 * HTTP requests are saved.
 * </p>
 * <p>
 * While the class is much older, the since annotation was moved to 42.0.0 because it implement a completely
 * different API from Java point of view.
 * 
 * @version $Id$
 * @since 42.0.0
 */
public class SavedRequestRestorerFilter implements Filter
{
    /**
     * Regular expression used for extracting the SRID from the query string. See
     * {@link #getSavedRequest(HttpServletRequest)}.
     */
    private static final Pattern SAVED_REQUEST_REGEXP =
        Pattern.compile("(?:^|&)" + SavedRequestManager.getSavedRequestIdentifier() + "=([^&]++)");

    /**
     * The name of the request attribute that specifies if this filter has already been applied to the current request.
     * This flag is required to prevent prevent processing the same request multiple times. The value of this request
     * attribute is a string. The associated boolean value is determined using {@link Boolean#valueOf(String)}.
     */
    private static final String ATTRIBUTE_APPLIED = SavedRequestRestorerFilter.class.getName() + ".applied";

    /**
     * Request Wrapper that inserts data from a previous request into the current request.
     */
    public static class SavedRequestWrapper extends HttpServletRequestWrapper
    {
        /** The saved request data; may be <code>null</code>, in which case no fallback data is used. */
        private SavedRequest savedRequest;

        /**
         * Simple constructor that forwards the initialization to the default {@link HttpServletRequestWrapper}. No
         * saved data is used.
         * 
         * @param request the new request, the primary object wrapped which contains the actual request data.
         */
        public SavedRequestWrapper(HttpServletRequest request)
        {
            super(request);
        }

        /**
         * Constructor that forwards the new request to the {@link HttpServletRequestWrapper}, and stores the saved
         * request data internally.
         * 
         * @param newRequest the new request, the primary object wrapped which contains the actual request data.
         * @param savedRequest the old request, the secondary object wrapped which contains the saved (fallback) request
         *            parameters.
         */
        public SavedRequestWrapper(HttpServletRequest newRequest, SavedRequest savedRequest)
        {
            super(newRequest);
            this.savedRequest = savedRequest;
        }

        /**
         * Retrieves the value for the parameter, either from the new request, or from the saved data.
         * 
         * @param name the name of the parameter
         * @return a <code>String</code> representing the first value of the parameter, or <code>null</code> if no value
         *         was set in either of the requests.
         * @see javax.servlet.ServletRequest#getParameter(java.lang.String)
         */
        @Override
        public String getParameter(String name)
        {
            String value = super.getParameter(name);
            if (value == null && this.savedRequest != null) {
                value = this.savedRequest.getParameter(name);
            }
            return value;
        }

        /**
         * Retrieves all the values for the parameter, either from the new request, or from the saved data (but not
         * combined).
         * 
         * @param name the name of the parameter
         * @return an array of <code>String</code> objects containing the parameter's values, or <code>null</code> if no
         *         value was set in either of the requests.
         * @see javax.servlet.ServletRequest#getParameterValues(java.lang.String)
         */
        @Override
        public String[] getParameterValues(String name)
        {
            String[] values = super.getParameterValues(name);
            if (values == null && this.savedRequest != null) {
                values = this.savedRequest.getParameterValues(name);
            }
            return values;
        }

        /**
         * Retrieves the combined map of parameter names - values, with the new values overriding the old ones.
         * 
         * @return an immutable Map containing parameter names as keys and parameter values as map values
         * @see javax.servlet.ServletRequest#getParameterMap()
         */
        @Override
        public Map<String, String[]> getParameterMap()
        {
            if (this.savedRequest == null) {
                return super.getParameterMap();
            } else {
                // First put the saved (old) request data in the map, so that the new data overrides it.
                Map<String, String[]> map = new HashMap<String, String[]>(this.savedRequest.getParameterMap());
                map.putAll(super.getParameterMap());
                return Collections.unmodifiableMap(map);
            }
        }

        /**
         * Retrieves the combined list of parameter names, from both the new and saved requests.
         * 
         * @return an <code>Enumeration</code> of <code>String</code> objects, each <code>String</code> containing the
         *         name of a request parameter; or an empty <code>Enumeration</code> if the request has no parameters
         * @see javax.servlet.ServletRequest#getParameterNames()
         */
        @Override
        public Enumeration<String> getParameterNames()
        {
            return Collections.enumeration(getParameterMap().keySet());
        }
    }

    @Override
    public void init(FilterConfig filterConfig)
    {
        // Don't do anything, as this filter does not need any resources.
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException
    {
        ServletRequest filteredRequest = request;
        // This filter works only for HTTP requests, because they are the only ones with a session.
        if (request instanceof HttpServletRequest
            && !Boolean.valueOf((String) request.getAttribute(ATTRIBUTE_APPLIED))) {
            // Get the saved request, if any (returns null if not applicable)
            SavedRequest savedRequest = getSavedRequest((HttpServletRequest) request);
            // Merge the new and the saved request
            filteredRequest = new SavedRequestWrapper((HttpServletRequest) request, savedRequest);
            filteredRequest.setAttribute(ATTRIBUTE_APPLIED, "true");
        }
        // Forward the request
        chain.doFilter(filteredRequest, response);
        // Allow multiple calls to this filter as long as they are not nested.
        filteredRequest.removeAttribute(ATTRIBUTE_APPLIED);
    }

    @Override
    public void destroy()
    {
        // Don't do anything, as this filter does not use any resources.
    }

    /**
     * If this request specifies a saved request (using the srid paramter) and the URL matches the one of the saved
     * request, return the SavedRequest and remove it from the session.
     * 
     * @param request the current request
     * @return the saved request, if one exists, or <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    protected SavedRequest getSavedRequest(HttpServletRequest request)
    {
        // Only do something if the new request contains a Saved Request IDentifier (srid)
        String savedRequestId = null;
        // Using request.getParameter is not good, since in some containers it prevents using request.getInputStream
        // and/or request.getReader. A workaround is to manually extract the srid parameter from the query string, but
        // this means that:
        // - the srid cannot be used in POST requests, but in all current use cases GET is used anyway;
        // - the regular expression used for this is pretty basic, so there might be some URLs that fail to be
        // recognized; so far this wasn't observed.
        Matcher m = SAVED_REQUEST_REGEXP.matcher(StringUtils.defaultString(request.getQueryString()));
        if (m.find()) {
            savedRequestId = m.group(1);
        }

        if (!StringUtils.isEmpty(savedRequestId)) {
            // Saved requests are stored in the request session
            HttpSession session = request.getSession();
            // Get the SavedRequest from the session
            Map<String, SavedRequest> savedRequests =
                (Map<String, SavedRequest>) session.getAttribute(SavedRequestManager.getSavedRequestKey());
            if (savedRequests != null) {
                SavedRequest savedRequest = savedRequests.get(savedRequestId);
                // Only reuse this request if the new request is for the same resource (URL)
                if (savedRequest != null
                    && StringUtils.equals(savedRequest.getRequestUrl(), request.getRequestURL().toString())) {
                    // Remove the saved request from the session
                    savedRequests.remove(savedRequestId);
                    // Return the SavedRequest
                    return savedRequest;
                }
            }
        }
        return null;
    }
}
