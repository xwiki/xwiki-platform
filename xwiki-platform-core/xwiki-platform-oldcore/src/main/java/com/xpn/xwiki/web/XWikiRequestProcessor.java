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

import java.util.List;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xwiki.resource.internal.entity.EntityResourceActionLister;
import org.xwiki.url.internal.standard.StandardURLConfiguration;
import org.xwiki.url.ExtendedURL;


/**
 * @version $Id$
 */
public class XWikiRequestProcessor extends org.apache.struts.action.RequestProcessor
{
    private static final String VIEW_ACTION = "view";

    private final EntityResourceActionLister entityResourceActionLister = Utils.getComponent(EntityResourceActionLister.class);

    private StandardURLConfiguration configuration = Utils.getComponent(StandardURLConfiguration.class);

    protected static final Logger LOGGER = LoggerFactory.getLogger(XWikiRequestProcessor.class);

    @Override
    protected String processPath(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
    {
        try {
            /**
             * Extract the action using the same method as in {@link
             * org.xwiki.url.internal.standard.entity.AbstractEntityResourceReferenceResolver}
             *
             * We cannot use the EntityResourceReferenceResolver because the execution context have not been
             * initialized here.
             */
            final String url = httpServletRequest.getRequestURL().toString();

            final ExtendedURL extendedURL = new ExtendedURL(new URL(url), httpServletRequest.getContextPath());

            final List<String> pathSegments = extendedURL.getSegments();

            String action = VIEW_ACTION;

            if (pathSegments.size() > 0) {
                action = pathSegments.get(0);
                if (this.configuration.isViewActionHidden()
                    && !VIEW_ACTION.equals(action)
                    && !this.entityResourceActionLister.listActions().contains(action)) {
                    action = VIEW_ACTION;
                }
            }

            return "/" + action + "/";
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
