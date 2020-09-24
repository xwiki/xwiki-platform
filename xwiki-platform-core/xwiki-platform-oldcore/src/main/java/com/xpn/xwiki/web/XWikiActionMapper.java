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

import java.net.URL;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.dispatcher.mapper.ActionMapper;
import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.ResourceTypeResolver;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.stability.Unstable;
import org.xwiki.url.ExtendedURL;

import com.opensymphony.xwork2.config.ConfigurationManager;

/**
 * Make sure to lead to to the right action depending on the XWiki configuration (for example leading to view action by
 * default if enabled, etc.).
 * 
 * @version $Id$
 * @since 12.9RC1
 */
@Unstable
public class XWikiActionMapper implements ActionMapper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiActionMapper.class);

    private ResourceTypeResolver<ExtendedURL> typeResolver =
        Utils.getComponent(new DefaultParameterizedType(null, ResourceTypeResolver.class, ExtendedURL.class));

    private ResourceReferenceResolver<ExtendedURL> resolver =
        Utils.getComponent(new DefaultParameterizedType(null, ResourceReferenceResolver.class, ExtendedURL.class));

    @Override
    public ActionMapping getMappingFromActionName(String actionName)
    {
        ActionMapping mapping = new ActionMapping();
        mapping.setName(actionName);
        return mapping;
    }

    @Override
    public ActionMapping getMapping(HttpServletRequest request, ConfigurationManager configManager)
    {
        String url = request.getRequestURL().toString();

        ActionMapping mapping = new ActionMapping();

        try {
            ExtendedURL extendedURL = new ExtendedURL(new URL(url), request.getContextPath());

            ResourceType type = this.typeResolver.resolve(extendedURL, Collections.<String, Object>emptyMap());

            EntityResourceReference entityResourceReference = (EntityResourceReference) this.resolver
                .resolve(extendedURL, type, Collections.<String, Object>emptyMap());

            mapping.setName(entityResourceReference.getAction().getActionName());
        } catch (Exception e) {
            LOGGER.error("Failed to extract the Entity Action from URL [{}]", url, e);
        }

        return mapping;
    }

    @Override
    public String getUriFromActionMapping(ActionMapping mapping)
    {
        // This is never really used in the context of XWiki so implementing it does no worth it
        return "";
    }
}
