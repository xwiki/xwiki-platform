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

import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.PropertyClassInterface;
import com.xpn.xwiki.objects.classes.UsersClass;

/**
 * Defines the meta properties of a users XClass property.
 *
 * @version $Id$
 */
@Component
@Named("Users")
@Singleton
public class UsersMetaClass extends ListMetaClass
{
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor. Initializes the default meta properties of a List of Users XClass property.
     */
    public UsersMetaClass()
    {
        setPrettyName("List of Users");
        setName(getClass().getAnnotation(Named.class).value());

        BooleanClass useListClass = new BooleanClass(this);
        useListClass.setName("usesList");
        useListClass.setPrettyName("Uses List");
        useListClass.setDisplayType("yesno");
        useListClass.setDisplayFormType("checkbox");
        useListClass.setDefaultValue(0);
        safeput(useListClass.getName(), useListClass);
    }

    @Override
    public PropertyClassInterface getInstance()
    {
        return new UsersClass();
    }
}
