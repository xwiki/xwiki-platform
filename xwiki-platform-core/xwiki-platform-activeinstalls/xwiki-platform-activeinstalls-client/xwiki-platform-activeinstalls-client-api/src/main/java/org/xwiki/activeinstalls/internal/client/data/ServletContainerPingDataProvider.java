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
package org.xwiki.activeinstalls.internal.client.data;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.activeinstalls.internal.client.PingDataProvider;
import org.xwiki.component.annotation.Component;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;

/**
 * Provide the Servlet Container's name and version.
 *
 * @version $Id$
 * @since 6.1M1
 */
@Component
@Named("servlet")
public class ServletContainerPingDataProvider implements PingDataProvider
{
    private static final String SERVLET_INFO_VERSION_SEPARATOR = "/";

    private static final String SERVLET_INFO_OPTIONALSEPARATOR = "(";

    private static final String PROPERTY_SERVLET_CONTAINER_NAME = "servletContainerName";

    private static final String PROPERTY_SERVLET_CONTAINER_VERSION = "servletContainerVersion";

    /**
     * Used to access the Servlet Context.
     */
    @Inject
    private Environment environment;

    @Inject
    private Logger logger;

    @Override
    public Map<String, Object> provideMapping()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "string");
        map.put("index", "not_analyzed");

        Map<String, Object> propertiesMap = new HashMap<>();
        propertiesMap.put(PROPERTY_SERVLET_CONTAINER_NAME, map);
        propertiesMap.put(PROPERTY_SERVLET_CONTAINER_VERSION, map);

        return propertiesMap;
    }

    @Override
    public Map<String, Object> provideData()
    {
        Map<String, Object> jsonMap = new HashMap<>();
        if (this.environment instanceof ServletEnvironment) {
            ServletEnvironment servletEnvironment = (ServletEnvironment) this.environment;
            try {
                ServletContext servletContext = servletEnvironment.getServletContext();
                // Format of getServerInfo() is "name/version (text)" where " (text)" is optional.
                String serverInfo = servletContext.getServerInfo();
                jsonMap.put(PROPERTY_SERVLET_CONTAINER_NAME,
                    StringUtils.trim(StringUtils.substringBefore(serverInfo, SERVLET_INFO_VERSION_SEPARATOR)));
                jsonMap.put(PROPERTY_SERVLET_CONTAINER_VERSION, StringUtils.trim(StringUtils.substringBefore(
                    StringUtils.substringAfter(serverInfo, SERVLET_INFO_VERSION_SEPARATOR),
                    SERVLET_INFO_OPTIONALSEPARATOR)));
            } catch (Throwable e) {
                // Ignore, we just don't save that information...
                // However we log a warning since it's a problem that needs to be seen and looked at.
                this.logger.warn("Failed to compute Servlet container information. "
                        + "This information has not been added to the Active Installs ping data. Reason [{}]",
                    ExceptionUtils.getRootCauseMessage(e)
                );
            }
        }
        return jsonMap;
    }
}
