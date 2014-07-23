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
package org.xwiki.filter.instance.internal.input;

import java.lang.reflect.ParameterizedType;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiClassFilter;
import org.xwiki.filter.instance.input.DocumentInstanceInputProperties;
import org.xwiki.filter.instance.input.EntityEventGenerator;
import org.xwiki.filter.instance.internal.BaseClassFilter;

import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

/**
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Singleton
public class BaseClassEventGenerator extends
    AbstractBeanEntityEventGenerator<BaseClass, BaseClassFilter, DocumentInstanceInputProperties>
{
    public static final ParameterizedType ROLE = new DefaultParameterizedType(null, EntityEventGenerator.class,
        BaseClass.class, DocumentInstanceInputProperties.class);

    @Inject
    private EntityEventGenerator<PropertyClass> propertyEventGenerator;

    @Override
    public void write(BaseClass xclass, Object filter, BaseClassFilter xclassFilter,
        DocumentInstanceInputProperties properties) throws FilterException
    {
        // WikiClass

        FilterEventParameters classParameters = new FilterEventParameters();

        classParameters.put(WikiClassFilter.PARAMETER_CUSTOMCLASS, xclass.getCustomClass());
        classParameters.put(WikiClassFilter.PARAMETER_CUSTOMMAPPING, xclass.getCustomMapping());
        classParameters.put(WikiClassFilter.PARAMETER_DEFAULTSPACE, xclass.getDefaultWeb());
        classParameters.put(WikiClassFilter.PARAMETER_NAMEFIELD, xclass.getNameField());
        classParameters.put(WikiClassFilter.PARAMETER_SHEET_DEFAULTEDIT, xclass.getDefaultEditSheet());
        classParameters.put(WikiClassFilter.PARAMETER_SHEET_DEFAULTVIEW, xclass.getDefaultViewSheet());
        classParameters.put(WikiClassFilter.PARAMETER_VALIDATIONSCRIPT, xclass.getValidationScript());

        xclassFilter.beginWikiClass(classParameters);

        // Properties

        // Iterate over values sorted by field name so that the values are
        // exported to XML in a consistent order.
        Iterator<PropertyClass> it = xclass.getSortedIterator();
        while (it.hasNext()) {
            PropertyClass xclassProperty = it.next();

            ((PropertyClassEventGenerator) this.propertyEventGenerator).write(xclassProperty, filter, xclassFilter,
                properties);
        }

        // /WikiClass

        xclassFilter.endWikiClass(classParameters);
    }
}
