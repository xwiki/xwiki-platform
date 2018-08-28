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

import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PageClass;
import com.xpn.xwiki.objects.classes.PropertyClassInterface;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

/**
 * Defines the meta properties of a database list XClass property.
 *
 * @version $Id$
 */
@Component
@Named("DBList")
@Singleton
public class DBListMetaClass extends ListMetaClass
{
    /**
     * Default constructor. Initializes the default meta properties of a Database List XClass property.
     */
    public DBListMetaClass()
    {
        setPrettyName("Database List");
        setName(getClass().getAnnotation(Named.class).value());

        TextAreaClass sqlClass = new TextAreaClass(this);
        sqlClass.setName("sql");
        sqlClass.setPrettyName("Hibernate Query");
        sqlClass.setEditor("PureText");
        sqlClass.setSize(80);
        sqlClass.setRows(5);
        safeput(sqlClass.getName(), sqlClass);

        PageClass classNameClass = new PageClass(this);
        classNameClass.setName("classname");
        classNameClass.setPrettyName("XWiki Class Name");
        classNameClass.setSize(20);
        classNameClass.setDisplayType(ListClass.DISPLAYTYPE_INPUT);
        classNameClass.setMultiSelect(false);
        safeput(classNameClass.getName(), classNameClass);

        StringClass idFieldClass = new StringClass(this);
        idFieldClass.setName("idField");
        idFieldClass.setPrettyName("Id Field Name");
        idFieldClass.setSize(20);
        safeput(idFieldClass.getName(), idFieldClass);

        StringClass valueFieldClass = new StringClass(this);
        valueFieldClass.setName("valueField");
        valueFieldClass.setPrettyName("Value Field Name");
        valueFieldClass.setSize(20);
        safeput(valueFieldClass.getName(), valueFieldClass);
    }

    @Override
    public PropertyClassInterface getInstance()
    {
        return new DBListClass();
    }
}
