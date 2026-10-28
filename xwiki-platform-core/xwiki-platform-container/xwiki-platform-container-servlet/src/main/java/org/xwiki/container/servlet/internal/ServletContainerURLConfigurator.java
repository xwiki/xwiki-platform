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
package org.xwiki.container.servlet.internal;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;

import jakarta.servlet.ServletContext;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configure the servlet container to accept any character in the URLs.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public final class ServletContainerURLConfigurator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ServletContainerURLConfigurator.class);

    private static final String TOMCAT_CATALINA = "Catalina";

    private ServletContainerURLConfigurator()
    {
        // Utility class
    }

    /**
     * Configure the servlet container (Tomcat or Jetty) to allow any character in the URLs (by default they reject some
     * characters, like an encoded slash, as recommended by the Servlet 6 specifications).
     *
     * @param servletContext the servlet context of XWiki, used to access the servlet container
     */
    public static void allowAllCharacters(ServletContext servletContext)
    {
        // Tomcat
        allowAllURLCharactersTomcat();

        // Jetty
        allowAllURLCharactersJetty(servletContext);
    }

    private static void allowAllURLCharactersTomcat()
    {
        try {
            ArrayList<MBeanServer> mbeanServers = MBeanServerFactory.findMBeanServer(null);
            if (!mbeanServers.isEmpty()) {
                MBeanServer mBeanServer = mbeanServers.get(0);
                ObjectName name = new ObjectName(TOMCAT_CATALINA, "type", "Server");
                Object server = mBeanServer.getAttribute(name, "managedResource");
                Object service = MethodUtils.invokeMethod(server, "findService", TOMCAT_CATALINA);
                Object connectors = MethodUtils.invokeMethod(service, "findConnectors");
                for (int i = 0; i < Array.getLength(connectors); ++i) {
                    Object connector = Array.get(connectors, i);

                    // Allow backslash (\)
                    MethodUtils.invokeMethod(connector, "setAllowBackslash", true);
                    // Allow slash/solidus (/)
                    MethodUtils.invokeMethod(connector, "setEncodedSolidusHandling", "passthrough");
                    // Try as much as possible to have XWiki being called directly for any URI
                    MethodUtils.invokeMethod(connector, "setRejectSuspiciousURIs", false);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to configure Tomcat URL constraints", e);
        }
    }

    private static void allowAllURLCharactersJetty(ServletContext servletContext)
    {
        try {
            Object contextHandler = MethodUtils.invokeMethod(servletContext, "getContextHandler");

            // setDecodeAmbiguousURIs
            Object servletHandler = MethodUtils.invokeMethod(contextHandler, "getServletHandler");
            MethodUtils.invokeMethod(servletHandler, "setDecodeAmbiguousURIs", true);

            // URI compliance
            Object server = MethodUtils.invokeMethod(contextHandler, "getServer");
            Object connectors = MethodUtils.invokeMethod(server, "getConnectors");
            Object uriCompliance = null;
            for (int i = 0; i < Array.getLength(connectors); ++i) {
                uriCompliance = configureConnectorJetty(Array.get(connectors, i), uriCompliance);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to configure Jetty URL constraints", e);
        }
    }

    private static Object configureConnectorJetty(Object connector, Object inputUriCompliance)
    {
        Object uriCompliance = inputUriCompliance;

        Object factories;
        try {
            factories = MethodUtils.invokeMethod(connector, "getConnectionFactories");
            if (factories instanceof Collection factoriesCollection) {
                for (Object factory : factoriesCollection) {
                    uriCompliance = configureFactoryJetty(factory, uriCompliance);
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to get factories", e);
        }

        return uriCompliance;
    }

    private static Object configureFactoryJetty(Object factory, Object inputUriCompliance)
    {
        Object uriCompliance = inputUriCompliance;

        try {
            Object httpConfiguration = MethodUtils.invokeMethod(factory, "getHttpConfiguration");

            if (uriCompliance == null) {
                uriCompliance = getUriCompliance(httpConfiguration);
            }

            // Disable various violations applied by default by Jetty to URIs and redirect URIs
            MethodUtils.invokeMethod(httpConfiguration, "setUriCompliance", uriCompliance);
            MethodUtils.invokeMethod(httpConfiguration, "setRedirectUriCompliance", uriCompliance);
        } catch (Exception e) {
            LOGGER.debug("Failed to set the URI compliance", e);
        }

        return uriCompliance;
    }

    private static Object getUriCompliance(Object httpConfiguration)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException
    {
        // Create a UriCompliance with the right class (we cannot use the one in the current classloader since it's
        // coming from XWiki WAR)

        Class<?> uriComplianceClass = MethodUtils.invokeMethod(httpConfiguration, "getUriCompliance").getClass();
        Class violationsClass = getViolationClass(uriComplianceClass);
        Set<Object> violations = new HashSet<>();
        violations.add(Enum.valueOf(violationsClass, "AMBIGUOUS_PATH_SEGMENT"));
        violations.add(Enum.valueOf(violationsClass, "AMBIGUOUS_EMPTY_SEGMENT"));
        violations.add(Enum.valueOf(violationsClass, "AMBIGUOUS_PATH_SEPARATOR"));
        violations.add(Enum.valueOf(violationsClass, "AMBIGUOUS_PATH_PARAMETER"));
        violations.add(Enum.valueOf(violationsClass, "AMBIGUOUS_PATH_ENCODING"));
        violations.add(Enum.valueOf(violationsClass, "SUSPICIOUS_PATH_CHARACTERS"));

        // Allow fragments in redirects (violation introduced in Jetty 12.1.0)
        Object fragmentViolation = EnumUtils.getEnum(violationsClass, "FRAGMENT");
        if (fragmentViolation != null) {
            violations.add(fragmentViolation);
        }

        return ConstructorUtils.invokeConstructor(uriComplianceClass, "XWiki", violations);
    }

    private static Class getViolationClass(Class<?> uriComplianceClass)
    {
        for (Class<?> innerClass : uriComplianceClass.getDeclaredClasses()) {
            // We cannot manipulate the class directly because it would probably be the wrong one, so we use reflection
            @SuppressWarnings("java:S1872")
            boolean isViolationClass = "org.eclipse.jetty.http.UriCompliance$Violation".equals(innerClass.getName());
            if (isViolationClass) {
                return innerClass;
            }
        }

        return null;
    }
}
