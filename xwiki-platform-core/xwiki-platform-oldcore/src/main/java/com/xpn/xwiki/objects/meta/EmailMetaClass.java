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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xpn.xwiki.objects.classes.EmailClass;
import com.xpn.xwiki.objects.classes.PropertyClassInterface;

/**
 * Email Field Class allows to create a field for email values. This will allow using a custom displayer assigned to
 * that field by default. The field also includes a default regexp for validation.
 *
 * @version $Id$
 * @since 4.2M2
 */
@Component
@Named("Email")
@Singleton
public class EmailMetaClass extends StringMetaClass
{
    /**
     * Constructor for EmailMetaClass.
     */
    public EmailMetaClass()
    {
        setPrettyName("Email");
        setName(getClass().getAnnotation(Named.class).value());
    }

    @Override
    public PropertyClassInterface getInstance()
    {
        return new EmailClass();
    }
}
