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
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.struts.Globals;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.RequestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.url.ExtendedURL;

/**
 * @version $Id$
 */
public class XWikiRequestProcessor extends org.apache.struts.action.RequestProcessor
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(XWikiRequestProcessor.class);

    private ResourceTypeResolver<ExtendedURL> typeResolver =
        Utils.getComponent(new DefaultParameterizedType(null, ResourceTypeResolver.class, ExtendedURL.class));

    private ResourceReferenceResolver<ExtendedURL> resolver =
        Utils.getComponent(new DefaultParameterizedType(null, ResourceReferenceResolver.class, ExtendedURL.class));

    /**
     * Remove request parameters containing special characters having a meaning for BeanUtils since otherwise BeanUtils
     * throws an exception if they are not used properly (for example if there's an open {@code (} but without a closing
     * {@code )} it'll report a "missing end delimiter" exception. FTR BeanUtils uses those to represent Nested
     * properties, Indexed properties, Mapped properties and Combined properties, see
     * http://commons.apache.org/proper/commons-beanutils/apidocs/org/apache/commons/beanutils/package-summary.html#standard
     */
    public class RequestProcessorServletRequestWrapper extends HttpServletRequestWrapper
    {
        private final char[] FORBIDDEN = new char[] {'(', ')', '[', ']', '.'};

        public RequestProcessorServletRequestWrapper(HttpServletRequest request)
        {
            super(request);
        }

        @Override
        public String getParameter(String name)
        {
            if (!StringUtils.containsAny(name, FORBIDDEN)) {
                return super.getParameter(name);
            } else {
                return null;
            }
        }

        @Override
        public Map<String, String[]> getParameterMap()
        {
            // Remove all forbidden names
            Map<String, String[]> newParameterMap = new HashMap<>();
            for (Map.Entry<String, String[]> entry : super.getParameterMap().entrySet()) {
                if (!StringUtils.containsAny(entry.getKey(), FORBIDDEN)) {
                    newParameterMap.put(entry.getKey(), entry.getValue());
                }
            }
            return Collections.unmodifiableMap(newParameterMap);
        }

        @Override
        public Enumeration<String> getParameterNames()
        {
            return Collections.enumeration(getParameterMap().keySet());
        }

        @Override
        public String[] getParameterValues(String name)
        {
            if (!StringUtils.containsAny(name, FORBIDDEN)) {
                return super.getParameterValues(name);
            } else {
                return null;
            }
        }
    }

    @Override
    protected String processPath(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
        throws IOException
    {
        String url = httpServletRequest.getRequestURL().toString();

        try {
            ExtendedURL extendedURL = new ExtendedURL(new URL(url), httpServletRequest.getContextPath());

            ResourceType type = this.typeResolver.resolve(extendedURL, Collections.<String, Object>emptyMap());

            EntityResourceReference entityResourceReference = (EntityResourceReference) this.resolver.resolve(
                extendedURL, type, Collections.<String, Object>emptyMap());

            return "/" + entityResourceReference.getAction().getActionName() + "/";
        } catch (Exception e) {
            throw new IOException(String.format("Failed to extract the Entity Action from URL [%s]", url), e);
        }
    }

    /**
     * Override the implementation from Struts in order to remove request parameter keys using special BeanUtils syntax
     * that we don't use and that can causes errors to be raised, see {@link RequestProcessorServletRequestWrapper}.
     */
    @Override
    protected void processPopulate(HttpServletRequest request, HttpServletResponse response, ActionForm form,
        ActionMapping mapping) throws ServletException
    {
        if (form == null) {
            return;
        }

        form.setServlet(this.servlet);
        form.reset(mapping, request);

        if (mapping.getMultipartClass() != null) {
            request.setAttribute(Globals.MULTIPART_KEY,
                mapping.getMultipartClass());
        }

        RequestUtils.populate(form, mapping.getPrefix(), mapping.getSuffix(),
            new RequestProcessorServletRequestWrapper(request));

        // Set the cancellation request attribute if appropriate
        if ((request.getParameter(Globals.CANCEL_PROPERTY) != null)
            || (request.getParameter(Globals.CANCEL_PROPERTY_X) != null)) {
            request.setAttribute(Globals.CANCEL_KEY, Boolean.TRUE);
        }
    }
}
