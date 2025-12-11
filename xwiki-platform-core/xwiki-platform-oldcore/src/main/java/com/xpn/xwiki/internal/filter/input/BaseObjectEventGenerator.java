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
package com.xpn.xwiki.internal.filter.input;

import java.lang.reflect.ParameterizedType;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiObjectFilter;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.instance.input.EntityEventGenerator;
import org.xwiki.filter.instance.internal.input.AbstractBeanEntityEventGenerator;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.filter.BaseObjectFilter;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Singleton
public class BaseObjectEventGenerator
    extends AbstractBeanEntityEventGenerator<BaseObject, BaseObjectFilter, DocumentInstanceInputProperties>
{
    /**
     * The role of this component.
     */
    public static final ParameterizedType ROLE = new DefaultParameterizedType(null, EntityEventGenerator.class,
        BaseObject.class, DocumentInstanceInputProperties.class);

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private EntityEventGenerator<BaseClass> classEventGenerator;

    @Inject
    private EntityEventGenerator<BaseProperty> propertyEventGenerator;

    @Override
    public void write(BaseObject xobject, Object filter, BaseObjectFilter objectFilter,
        DocumentInstanceInputProperties properties) throws FilterException
    {
        XWikiContext context = this.xcontextProvider.get();

        // > WikiObject

        FilterEventParameters objectParameters = new FilterEventParameters();

        objectParameters.put(WikiObjectFilter.PARAMETER_NAME, xobject.getName());
        objectParameters.put(WikiObjectFilter.PARAMETER_CLASS_REFERENCE, xobject.getClassName());
        objectParameters.put(WikiObjectFilter.PARAMETER_GUID, xobject.getGuid());
        objectParameters.put(WikiObjectFilter.PARAMETER_NUMBER, xobject.getNumber());

        objectFilter.beginWikiObject(xobject.getReference() != null ? xobject.getReference().getName() : null,
            objectParameters);

        // Object class

        BaseClass xclass = xobject.getXClass(context);
        ((BaseClassEventGenerator) this.classEventGenerator).write(xclass, filter, objectFilter, properties);

        // Properties

        // Iterate over values/properties sorted by field name so that the values are
        // exported to XML in a consistent order.
        Iterator<BaseProperty<?>> it = xobject.getSortedIterator();
        while (it.hasNext()) {
            BaseProperty<?> xproperty = it.next();

            String pname = xproperty.getName();
            if (StringUtils.isNotBlank(pname)
                && (!(xclass.get(pname) instanceof PropertyClass propertyClass)
                || !properties.getExcludedPropertyTypes().contains(propertyClass.getClassType())))
            {
                ((BasePropertyEventGenerator) this.propertyEventGenerator).write(xproperty, filter,
                    (Map<String, Object>) properties);
            }
        }

        // < WikiObject

        objectFilter.endWikiObject(xobject.getReference().getName(), objectParameters);
    }
}
