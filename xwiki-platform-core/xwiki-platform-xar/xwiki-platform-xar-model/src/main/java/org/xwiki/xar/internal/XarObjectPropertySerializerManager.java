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
package org.xwiki.xar.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

/**
 * @version $Id$
 * @since 5.4M1
 */
@Component(roles = XarObjectPropertySerializerManager.class)
@Singleton
public class XarObjectPropertySerializerManager
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private XarObjectPropertySerializer defaultPropertySerializer;

    /**
     * @param type the property type for which to find a serializer
     * @return the property serializer, if none could be found for the provided type the default one is returned
     * @throws ComponentLookupException when failing to lookup property serializer
     */
    public XarObjectPropertySerializer getPropertySerializer(String type) throws ComponentLookupException
    {
        if (type != null) {
            ComponentManager componentManager = this.componentManagerProvider.get();

            if (componentManager.hasComponent(XarObjectPropertySerializer.class, type)) {
                // First try to use the specified class type as hint.
                return getInstance(type, componentManager);
            } else {
                // In previous versions the class type was the full Java class name of the property class
                // implementation. Extract the hint by removing the Java package prefix and the Class suffix.
                String simpleType = StringUtils.removeEnd(StringUtils.substringAfterLast(type, "."), "Class");

                if (componentManager.hasComponent(XarObjectPropertySerializer.class, simpleType)) {
                    return getInstance(simpleType, componentManager);
                }
            }
        }

        return this.defaultPropertySerializer;
    }

    private XarObjectPropertySerializer getInstance(String type, ComponentManager componentManager)
        throws ComponentLookupException
    {
        return componentManager.getInstance(XarObjectPropertySerializer.class, type);
    }
}
