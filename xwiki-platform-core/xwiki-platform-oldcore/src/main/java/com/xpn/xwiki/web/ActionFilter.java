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
package com.xpn.xwiki.web;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.configuration.ConfigurationSource;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.internal.XWikiCfgConfigurationSource;

/**
 * A filter that dispatches requests to the right action, depending on the button that was pressed in the editing form.
 * This is needed since the browser cannot send the form data to different URLs depending on the button pressed, and an
 * XHTML form has only one target URL. In previous versions of XWiki this was accomplished using javascript code, with a
 * fall-back on a pseudo-dispatcher inside the {@link PreviewAction}, which was on obvious case of bad code design.
 * <p>
 * The filter dispatches requests based on the presence of a request parameter starting with <tt>action_</tt> followed
 * by the name of the struts action that should actually process the request. For example, the button that does
 * <tt>Save and Continue</tt> looks like:
 *
 * <pre>
 * &lt;input type=&quot;submit&quot; name=&quot;action_saveandcontinue&quot; value=&quot;...&quot;/&gt;
 * </pre>
 *
 * As a result, when clicking the button, the request is not sent to the form's target (<tt>preview</tt>), but is
 * actually forwarded internally to <tt>/bin/saveandcontinue/The/Document</tt>.
 *
 * @version $Id$
 * @since 1.8M1
 */
public class ActionFilter implements Filter
{
    /** Logging helper. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ActionFilter.class);

    /** The query property name prefix that indicates the target action. */
    private static final String ACTION_PREFIX = "action_";

    /** URL path separator. */
    private static final String PATH_SEPARATOR = "/";

    /**
     * The name of the request attribute that specifies if the action has been already dispatched. This flag is required
     * to prevent recursive dispatch loop and allows us to map this filter to INCLUDE and FORWARD. The value of this
     * request attribute is a string. The associated boolean value is determined using {@link Boolean#valueOf(String)}.
     */
    private static final String ATTRIBUTE_ACTION_DISPATCHED = ActionFilter.class.getName() + ".actionDispatched";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
        ServletException
    {
        // Only HTTP requests can be dispatched.
        if (request instanceof HttpServletRequest
            && !Boolean.valueOf((String) request.getAttribute(ATTRIBUTE_ACTION_DISPATCHED))) {
            HttpServletRequest hrequest = (HttpServletRequest) request;
            Enumeration<String> parameterNames = hrequest.getParameterNames();
            while (parameterNames.hasMoreElements()) {
                String parameter = parameterNames.nextElement();
                if (parameter.startsWith(ACTION_PREFIX)) {
                    String targetURL = getTargetURL(hrequest, parameter);
                    RequestDispatcher dispatcher = hrequest.getRequestDispatcher(targetURL);
                    if (dispatcher != null) {
                        LOGGER.debug("Forwarding request to " + targetURL);
                        request.setAttribute(ATTRIBUTE_ACTION_DISPATCHED, "true");
                        dispatcher.forward(hrequest, response);
                        // Allow multiple calls to this filter as long as they are not nested.
                        request.removeAttribute(ATTRIBUTE_ACTION_DISPATCHED);
                        // If the request was forwarder to another path, don't continue the normal processing chain.
                        return;
                    }
                }
            }
        }
        // Let the request pass through unchanged.
        chain.doFilter(request, response);
    }

    @Override
    public void destroy()
    {
        // No finalization needed.
    }

    /**
     * Compose a new URL path based on the original request and the specified action. The result is relative to the
     * application context, so that it can be used with {@link HttpServletRequest#getRequestDispatcher(String)}. For
     * example, calling this method with a request for <tt>/xwiki/bin/edit/Some/Document</tt> and <tt>action_save</tt>,
     * the result is <tt>/bin/save/Some/Document</tt>.
     *
     * @param request the original request
     * @param action the action parameter, starting with <tt>action_</tt>
     * @return The rebuilt URL path, with the specified action in place of the original Struts action. Note that unlike
     *         the HTTP path, this does not contain the application context part.
     */
    private String getTargetURL(HttpServletRequest request, String action)
    {
        String newAction = PATH_SEPARATOR + action.substring(ACTION_PREFIX.length());

        // Extract the document name from the requested path. We don't use getPathInfo() since it is decoded
        // by the container, thus it will not work when XWiki uses a non-UTF-8 encoding.
        String path = request.getRequestURI();

        // First step, remove the context path, if any.
        path = XWiki.stripSegmentFromPath(path, request.getContextPath());

        // Second step, remove the servlet path, if any.
        String servletPath = request.getServletPath();
        path = XWiki.stripSegmentFromPath(path, servletPath);

        // Third step, remove the struts mapping. This step is mandatory, so this filter will fail if the
        // requested action was a hidden (default) 'view', like in '/bin/Main/'. This is OK, since forms
        // don't use 'view' as a target.
        int index = path.indexOf(PATH_SEPARATOR, 1);

        // We need to also get rid of the wiki name in case of a XEM in usepath mode
        ConfigurationSource configuration =
            Utils.getComponent(ConfigurationSource.class, XWikiCfgConfigurationSource.ROLEHINT);
        if ("1".equals(configuration.getProperty("xwiki.virtual.usepath", "1"))) {
            if (servletPath.equals(PATH_SEPARATOR
                + configuration.getProperty("xwiki.virtual.usepath.servletpath", "wiki"))) {
                // Move the wiki name together with the servlet path
                servletPath += path.substring(0, index);
                index = path.indexOf(PATH_SEPARATOR, index + 1);
            }
        }

        String document = path.substring(index);

        // Compose the target URL starting with the servlet path.
        return servletPath + newAction + document;
    }
}
