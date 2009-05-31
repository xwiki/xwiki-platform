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
package org.xwiki.rest;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import javax.ws.rs.core.Application;

import org.restlet.Context;

/**
 * This class is used to configure JAX-RS resources and providers. Currently it builds resources and providers lists
 * from servlet parameters. TODO: Write an automatic discovery mechanism as it is done in Jersey.
 * 
 * @version $Id$
 */
public class XWikiJaxRsApplication extends Application
{
    private static final String RESOURCES_PARAMETER = "resources";

    private static final String PROVIDERS_PARAMETER = "providers";

    private Set<Class< ? >> jaxRsClasses;

    public XWikiJaxRsApplication(Context context)
    {
        this.jaxRsClasses = new HashSet<Class< ? >>();

        /* Retrieve resources list from servlet parameters */
        String resourcesParameter = context.getParameters().getFirstValue(RESOURCES_PARAMETER);
        if (resourcesParameter != null) {
            String[] resourceClassNames = resourcesParameter.split(";");
            for (String resourceClassName : resourceClassNames) {
                try {
                    resourceClassName = resourceClassName.trim();
                    Class< ? > resourceClass = this.getClass().getClassLoader().loadClass(resourceClassName);
                    jaxRsClasses.add(resourceClass);

                    context.getLogger().log(Level.FINE, String.format("Added resource %s", resourceClassName));
                } catch (ClassNotFoundException e) {
                    context.getLogger().log(Level.WARNING, String.format("Cannot load class %s", resourceClassName));
                }
            }
        }

        /* Retrieve providers list from servlet parameters */
        String providersParameter = context.getParameters().getFirstValue(PROVIDERS_PARAMETER);
        if (providersParameter != null) {
            String[] providerClassNames = providersParameter.split(";");
            for (String providerClassName : providerClassNames) {
                try {
                    providerClassName = providerClassName.trim();
                    Class< ? > providerClass = this.getClass().getClassLoader().loadClass(providerClassName);
                    jaxRsClasses.add(providerClass);

                    context.getLogger().log(Level.FINE, String.format("Added provider %s", providerClassName));
                } catch (ClassNotFoundException e) {
                    context.getLogger().log(Level.WARNING, String.format("Cannot load class %s", providerClassName));
                }
            }
        }

        context.getLogger().log(Level.INFO, "RESTful API subsystem initialized.");
    }

    @Override
    public Set<Class< ? >> getClasses()
    {
        return jaxRsClasses;
    }

}
