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
package org.xwiki.webjars.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.io.IOUtils;
import org.xwiki.action.AbstractAction;
import org.xwiki.action.ActionChain;
import org.xwiki.action.ActionException;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.resource.ActionId;
import org.xwiki.resource.EntityResource;
import org.xwiki.resource.Resource;

/**
 * Handles {@code webjars} action.
 * <p>
 * At the moment we're mapping calls to the "webjars" URL as an EntityResource.
 * In the future it would be cleaner to register some new URL factory instead of reusing the format for Entity
 * Resources. Since we're reusing it we're going to do the following mapping:
 * <code>
 *   http://server/bin/webjars/resource/path?value=(resource name)
 * </code>
 * (for example: http://localhost:8080/bin/webjars/resource/path?value=angularjs/1.1.5/angular.js)
 * So this means that the resource name will be parsed as a query string "value" parameter (with a fixed space of
 * "resource" and a fixed page name of "path").
 * </p>
 *
 * @version $Id$
 * @since 6.0M1
 */
@Component
@Named("webjars")
public class WebJarsAction extends AbstractAction
{
    /**
     * The WebJars Action Id.
     */
    public static final ActionId WEBJARS = new ActionId("webjars");

    /**
     * Prefix for locating JS resources in the classloader.
     */
    private static final String WEBJARS_RESOURCE_PREFIX = "META-INF/resources/webjars/";

    @Inject
    private Container container;

    @Override
    public List<ActionId> getSupportedActionIds()
    {
        return Arrays.asList(WEBJARS);
    }

    @Override
    public void execute(Resource resource, ActionChain chain) throws ActionException
    {
        EntityResource entityResource = (EntityResource) resource;
        String resourceName = entityResource.getParameterValue("value");
        String resourcePath = String.format("%s%s", WEBJARS_RESOURCE_PREFIX, resourceName);

        InputStream resourceStream = getClassLoader().getResourceAsStream(resourcePath);

        if (resourceStream != null) {
            try {
                IOUtils.copy(resourceStream, this.container.getResponse().getOutputStream());
            } catch (IOException e) {
                throw new ActionException(String.format("Failed to read resource [%s]", resourceName), e);
            } finally {
                IOUtils.closeQuietly(resourceStream);
            }
        }

        // Be a good citizen, continue the chain, in case some lower-priority action has something to do for this
        // action id.
        chain.executeNext(resource);
    }

    /**
     * @return the Class Loader from which to look for WebJars resources
     */
    protected ClassLoader getClassLoader()
    {
        // Load the resource from the context class loader in order to support webjars located in XWiki Extensions
        // loaded by the Extension Manager.
        return Thread.currentThread().getContextClassLoader();
    }
}
