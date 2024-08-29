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
package org.xwiki.activeinstalls2.internal.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletContext;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;

import co.elastic.clients.elasticsearch._types.mapping.Property;

/**
 * Provide the Servlet Container's name and version.
 *
 * @version $Id$
 * @since 6.1M1
 */
@Component
@Named("servlet")
@Singleton
public class ServletContainerPingDataProvider extends AbstractPingDataProvider
{
    private static final String SERVLET_INFO_VERSION_SEPARATOR = "/";

    private static final String SERVLET_INFO_OPTIONALSEPARATOR = "(";

    private static final String PROPERTY_SERVLET_CONTAINER_NAME = "name";

    private static final String PROPERTY_SERVLET_CONTAINER_VERSION = "version";

    private static final String PROPERTY_SERVLET = "servlet";

    /**
     * Used to access the Servlet Context.
     */
    @Inject
    private Environment environment;

    @Override
    public Map<String, Property> provideMapping()
    {
        Map<String, Property> propertiesMap = new HashMap<>();
        propertiesMap.put(PROPERTY_SERVLET_CONTAINER_NAME, Property.of(b1 -> b1.keyword(b2 -> b2)));
        propertiesMap.put(PROPERTY_SERVLET_CONTAINER_VERSION, Property.of(b1 -> b1.keyword(b2 -> b2)));

        return Collections.singletonMap(PROPERTY_SERVLET, Property.of(b0 -> b0.object(b1 ->
            b1.properties(propertiesMap))));
    }

    @Override
    public void provideData(Ping ping)
    {
        if (this.environment instanceof ServletEnvironment servletEnvironment) {
            try {
                ServletContext servletContext = servletEnvironment.getServletContext();
                // Format of getServerInfo() is "name/version (text)" where " (text)" is optional.
                String serverInfo = servletContext.getServerInfo();
                ServletContainerPing servletContainerPing = new ServletContainerPing();
                servletContainerPing.setName(
                    StringUtils.trim(StringUtils.substringBefore(serverInfo, SERVLET_INFO_VERSION_SEPARATOR)));
                servletContainerPing.setVersion(StringUtils.trim(StringUtils.substringBefore(
                    StringUtils.substringAfter(serverInfo, SERVLET_INFO_VERSION_SEPARATOR),
                    SERVLET_INFO_OPTIONALSEPARATOR)));
                ping.setServletContainer(servletContainerPing);
            } catch (Exception e) {
                // Ignore, we just don't save that information...
                // However, we log a warning since it's a problem that needs to be seen and looked at.
                logWarning("Failed to compute Servlet container information", e);
            }
        }
    }
}
