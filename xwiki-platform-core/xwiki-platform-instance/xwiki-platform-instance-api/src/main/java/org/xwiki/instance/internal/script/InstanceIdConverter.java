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
package org.xwiki.instance.internal.script;

import java.lang.reflect.Type;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.instance.InstanceId;
import org.xwiki.properties.converter.AbstractConverter;

/**
 * Helper for scripts to converts a String representing an Instance ID to an {@link InstanceId} object and vice versa.
 *
 * @version $Id$
 * @since 8.4.1
 * @since 9.0RC1
 */
@Component
@Singleton
public class InstanceIdConverter extends AbstractConverter<InstanceId>
{
    @Override
    protected InstanceId convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return null;
        }

        return new InstanceId(value.toString());
    }

    @Override
    protected String convertToString(InstanceId value)
    {
        return value.getInstanceId();
    }
}
