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
 *
 */
package com.xpn.xwiki.web;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

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
 * 
 * @version $Id$
 */
public class SavedRequestRestorerFilter implements Filter
{
    /** The name of the parameter used for identifying a saved request in a new request. */
    public static final String SAVED_REQUESTS_IDENTIFIER = "srid";

    /** The key used for storing request data in the HTTP session. */
    private static final String SAVED_REQUESTS_KEY = SavedRequest.class.getCanonicalName() + "_SavedRequests";

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
         * @return a <code>String</code> representing the first value of the parameter, or <code>null</code> if no
         *         value was set in either of the requests.
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
         * @return an array of <code>String</code> objects containing the parameter's values, or <code>null</code>
         *         if no value was set in either of the requests.
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
        @SuppressWarnings("unchecked")
        @Override
        public Map getParameterMap()
        {
            if (this.savedRequest == null) {
                return super.getParameterMap();
            } else {
                // First put the saved (old) request data in the map, so that the new data overrides it.
                Map map = new HashMap(this.savedRequest.getParameterMap());
                map.putAll(super.getParameterMap());
                return Collections.unmodifiableMap(map);
            }
        }

        /**
         * Retrieves the combined list of parameter names, from both the new and saved requests.
         * 
         * @return an <code>Enumeration</code> of <code>String</code> objects, each <code>String</code> containing
         *         the name of a request parameter; or an empty <code>Enumeration</code> if the request has no
         *         parameters
         * @see javax.servlet.ServletRequest#getParameterNames()
         */
        @SuppressWarnings("unchecked")
        @Override
        public Enumeration getParameterNames()
        {
            return Collections.enumeration(getParameterMap().keySet());
        }
    }

    /**
     * Saved request data. Only request parameter are stored, along with the requested URL.
     */
    public static class SavedRequest
    {
        /** Saved request data. */
        private Map<String, String[]> parameters;

        /**
         * The request URL; does not include the query string. The data is reused only if the new URL matches this
         * value.
         */
        private String requestUrl;

        /**
         * Constructor that copies the needed information from a request.
         * 
         * @param request the request that needs to be saved
         */
        @SuppressWarnings("unchecked")
        public SavedRequest(HttpServletRequest request)
        {
            this.parameters = new HashMap<String, String[]>(request.getParameterMap());
            this.requestUrl = request.getRequestURL().toString();
        }

        /**
         * Gets the value for a parameter, just like {@link javax.servlet.ServletRequest#getParameter(String)}.
         * 
         * @param name the name of the parameter
         * @return The first value for this parameter, or <code>null</code> if no value was sent for this parameter.
         * @see javax.servlet.ServletRequest#getParameter(String)
         * @see #getParameterValues(String)
         */
        public String getParameter(String name)
        {
            String[] values = this.parameters.get(name);
            if (values != null && values.length > 0) {
                return values[0];
            }
            return null;
        }

        /**
         * Gets all the values stored for a parameter, just like
         * {@link javax.servlet.ServletRequest#getParameterValues(String)}.
         * 
         * @param name the name of the parameter
         * @return All the values for this parameter, or <code>null</code> if no value was sent for this parameter.
         * @see javax.servlet.ServletRequest#getParameterValues(String)
         * @see #getParameter(String)
         */
        public String[] getParameterValues(String name)
        {
            return this.parameters.get(name);
        }

        /**
         * Gets all the stored parameters, just like {@link javax.servlet.ServletRequest#getParameterMap()}.
         * 
         * @return A map with all the stored parameters.
         * @see javax.servlet.ServletRequest#getParameterMap()
         */
        @SuppressWarnings("unchecked")
        public Map getParameterMap()
        {
            return this.parameters;
        }

        /**
         * Retrieves the original URL used for this request, as a future request will be able to reuse this data only if
         * it is for the same document. Does not contain the query string.
         * 
         * @return A <code>String</code> representation of the URL corresponding to this request.
         */
        public String getRequestUrl()
        {
            return this.requestUrl;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.Filter#init(javax.servlet.FilterConfig)
     */
    public void init(FilterConfig filterConfig)
    {
        // Don't do anything, as this filter does not need any resources.
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse,
     *      javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
        ServletException
    {
        // This filter works only for HTTP requests, because they are the only ones with a session.
        if (request instanceof HttpServletRequest) {
            // Get the saved request, if any (returns null if not applicable)
            SavedRequest savedRequest = getSavedRequest((HttpServletRequest) request);
            // Merge the new and the saved request
            request = new SavedRequestWrapper((HttpServletRequest) request, savedRequest);
        }
        // Forward the request
        chain.doFilter(request, response);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.servlet.Filter#destroy()
     */
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
        String savedRequestId = request.getParameter(SAVED_REQUESTS_IDENTIFIER);
        if (!StringUtils.isEmpty(savedRequestId)) {
            // Saved requests are stored in the request session
            HttpSession session = request.getSession();
            // Get the SavedRequest from the session
            Map<String, SavedRequest> savedRequests =
                (Map<String, SavedRequest>) session.getAttribute(SAVED_REQUESTS_KEY);
            if (savedRequests != null) {
                SavedRequest savedRequest = savedRequests.get(savedRequestId);
                // Only reuse this request if the new request is for the same resource (URL)
                if (savedRequest != null
                    && StringUtils.equals(savedRequest.getRequestUrl(), request.getRequestURL().toString()))
                {
                    // Remove the saved request from the session
                    savedRequests.remove(savedRequestId);
                    // Return the SavedRequest
                    return savedRequest;
                }
            }
        }
        return null;
    }

    /**
     * Saves the data from a request and stores it in the current session. This method is not thread safe, and does not
     * guarantee that saved requests are not overwritten, but given that this should only happen sparingly, and that
     * each client uses his own session to save this kind of information, this is not a real issue.
     * 
     * @param request the request to save
     * @return the identifier of the saved request
     */
    @SuppressWarnings("unchecked")
    public static String saveRequest(HttpServletRequest request)
    {
        // Saved requests are stored in the request session
        HttpSession session = request.getSession();
        // Retrieve (and eventually initialize) the list of stored requests
        Map<String, SavedRequest> savedRequests = (Map<String, SavedRequest>) session.getAttribute(SAVED_REQUESTS_KEY);
        if (savedRequests == null) {
            savedRequests = new HashMap<String, SavedRequest>();
            session.setAttribute(SAVED_REQUESTS_KEY, savedRequests);
        }
        // Save the request data
        SavedRequest savedRequest = new SavedRequest(request);
        // Generate a random key to identify this request
        String key;
        do {
            key = RandomStringUtils.randomAlphanumeric(8);
        } while (savedRequests.containsKey(key));
        // Store the saved request
        savedRequests.put(key, savedRequest);
        // Return the generated identifier
        return key;
    }

    /**
     * Retrieves the original URL requested before a detour. This method returns something different from
     * <code>null</code> only when there's a <em>rsid</em> parameter in the current request, indicating that there
     * was another request whose data was saved, related to the current request.
     * 
     * @param request the current request
     * @return the original requested URL that triggered a detour, or <code>null</code> if there isn't any original
     *         request information
     */
    @SuppressWarnings("unchecked")
    public static String getOriginalUrl(HttpServletRequest request)
    {
        HttpSession session = request.getSession();
        Map<String, SavedRequest> savedRequests = (Map<String, SavedRequest>) session.getAttribute(SAVED_REQUESTS_KEY);
        if (savedRequests != null) {
            String identifier = request.getParameter(SAVED_REQUESTS_IDENTIFIER);
            if (!StringUtils.isEmpty(identifier)) {
                SavedRequest savedRequest = savedRequests.get(request.getParameter(SAVED_REQUESTS_IDENTIFIER));
                if (savedRequest != null) {
                    return savedRequest.getRequestUrl() + "?srid=" + identifier;
                }
            }
        }
        return null;
    }
}
