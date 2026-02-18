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

import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.classes.ListClass;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Component implementation for all list types.
 *
 * @version $Id$
 * @since 18.2.0RC1
 */
@Component(hints = {
    DBStringListProperty.PROPERTY_TYPE,
    StringListProperty.PROPERTY_TYPE
})
@Singleton
public class ListPropertyParser implements ObjectPropertyParser
{
    @Inject
    private ComponentDescriptor<ObjectPropertyParser> descriptor;

    private BaseProperty<?> createProperty() throws XWikiException
    {
        switch (descriptor.getRoleHint()) {
            case DBStringListProperty.PROPERTY_TYPE ->
            {
                return  new DBStringListProperty();
            }

            case StringListProperty.PROPERTY_TYPE ->
            {
                return  new StringListProperty();
            }

            default -> throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_UNKNOWN,
                String.format("Unsupported hint [%s]", descriptor.getRoleHint()));
        }
    }

    @Override
    public BaseProperty<?> fromString(String value) throws XWikiException
    {
        BaseProperty<?> result = createProperty();
        result.setValue(ListClass.getListFromString(value));
        return result;
    }

    @Override
    public BaseProperty<?> fromValue(Object value) throws XWikiException
    {
        if (value instanceof String stringValue) {
            return fromString(stringValue);
        } else if (value instanceof List<?> listValue) {
            BaseProperty<?> result = createProperty();
            result.setValue(listValue);
            return result;
        } else {
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_UNKNOWN, String.format(
                "Value of type [%s] is not supported for a list. Value content was [%s]", value.getClass(), value));
        }
    }
}
