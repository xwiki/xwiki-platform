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

import com.xpn.xwiki.objects.classes.PropertyClassInterface;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.StringClass;

/**
 * Defines the meta properties of a static list XClass property.
 *
 * @version $Id$
 */
@Component
@Named("StaticList")
@Singleton
public class StaticListMetaClass extends ListMetaClass
{
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor. Initializes the default meta properties of a Static List XClass property.
     */
    public StaticListMetaClass()
    {
        setPrettyName("Static List");
        setName(getClass().getAnnotation(Named.class).value());

        StringClass valuesClass = new StringClass(this);
        valuesClass.setName("values");
        valuesClass.setPrettyName("Values");
        valuesClass.setSize(40);
        safeput(valuesClass.getName(), valuesClass);
    }

    @Override
    public PropertyClassInterface getInstance()
    {
        return new StaticListClass();
    }
}
