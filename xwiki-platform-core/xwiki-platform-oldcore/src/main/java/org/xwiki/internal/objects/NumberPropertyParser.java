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
package org.xwiki.internal.objects;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.FloatProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.LongProperty;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Component implementation for all types of number properties.
 *
 * @version $Id$
 * @since 18.2.0RC1
 */
@Component(hints = {
    IntegerProperty.PROPERTY_TYPE,
    FloatProperty.PROPERTY_TYPE,
    LongProperty.PROPERTY_TYPE,
    DoubleProperty.PROPERTY_TYPE
})
@Singleton
public class NumberPropertyParser extends AbstractObjectPropertyParser
{
    @Inject
    private ComponentDescriptor<?> descriptor;

    @Override
    protected PropertyClass getBaseClass() throws XWikiException
    {
        NumberClass numberClass = new NumberClass();
        switch (descriptor.getRoleHint()) {
            case IntegerProperty.PROPERTY_TYPE:
                numberClass.setNumberType(NumberClass.TYPE_INTEGER);
                break;

            case FloatProperty.PROPERTY_TYPE:
                numberClass.setNumberType(NumberClass.TYPE_FLOAT);
                break;

            case LongProperty.PROPERTY_TYPE:
                numberClass.setNumberType(NumberClass.TYPE_LONG);
                break;

            case DoubleProperty.PROPERTY_TYPE:
                numberClass.setNumberType(NumberClass.TYPE_DOUBLE);
                break;

            default:
                throw new XWikiException(
                    XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_UNKNOWN,
                    String.format("Invalid role hint [{}] is not a supported type number.", descriptor.getRoleHint()));
        }

        return numberClass;
    }
}
