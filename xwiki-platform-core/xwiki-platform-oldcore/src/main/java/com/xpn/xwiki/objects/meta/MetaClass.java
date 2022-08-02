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
package com.xpn.xwiki.objects.meta;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.objects.classes.PropertyClassProvider;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.web.Utils;

/**
 * A pseudo XClass whose fields are meta properties. In other words, each field of this XClass defines a type of
 * property that can be added to a standard XClass. This class is being used to lookup XClass property types. New code
 * should lookup {@link PropertyClassProvider} implementations instead using the component manager.
 *
 * @version $Id$
 */
public class MetaClass extends BaseClass
{
    private static final long serialVersionUID = 1L;

    /**
     * The prefix prepended to the property name when setting or retrieving a property meta class.
     */
    private static final String PROPERTY_NAME_PREFIX = "meta";

    /**
     * Logging helper object.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaClass.class);

    /**
     * A cached instance of this class that can be used to quickly lookup XClass property types.
     */
    private static MetaClass metaClass;

    /**
     * Creates a new instance that has a property for each available property type.
     */
    public MetaClass()
    {
        try {
            List<PropertyClassProvider> providers =
                Utils.getContextComponentManager().getInstanceList(PropertyClassProvider.class);
            for (PropertyClassProvider provider : providers) {
                PropertyInterface property = provider.getDefinition();
                safeput(property.getName(), property);
            }
        } catch (ComponentLookupException e) {
            LOGGER.error("Failed to initialize the meta class.", e);
        }
    }

    @Override
    public void safeput(String name, PropertyInterface property)
    {
        addField(PROPERTY_NAME_PREFIX + name, property);
        if (property instanceof PropertyClass) {
            ((PropertyClass) property).setObject(this);
            ((PropertyClass) property).setName(name);
        }
    }

    @Override
    public PropertyInterface safeget(String name)
    {
        return super.safeget(PROPERTY_NAME_PREFIX + name);
    }

    @Override
    public PropertyInterface get(String name)
    {
        PropertyInterface property = safeget(name);
        if (property == null) {
            // In previous versions the property name was the full Java class name of the property class implementation.
            // Extract the actual property name (the hint used to lookup the property class provider) by removing the
            // Java package prefix and the Class suffix.
            property = safeget(StringUtils.removeEnd(StringUtils.substringAfterLast(name, "."), "Class"));
        }
        return property;
    }

    @Override
    public void put(String name, PropertyInterface property)
    {
        safeput(name, property);
    }

    /**
     * @return a cached instance of this class that can be used to quickly lookup XClass property types
     */
    public static MetaClass getMetaClass()
    {
        if (metaClass == null) {
            metaClass = new MetaClass();
        }
        return metaClass;
    }

    /**
     * Sets the cached instance of this class.
     *
     * @param metaClass the cached instance
     */
    public static void setMetaClass(MetaClass metaClass)
    {
        MetaClass.metaClass = metaClass;
    }

    @Override
    public BaseCollection newObject(XWikiContext context)
    {
        return new BaseClass();
    }
}
