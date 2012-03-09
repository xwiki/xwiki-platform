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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

public class DBListMetaClass extends ListMetaClass
{
    public DBListMetaClass()
    {
        super();
        setPrettyName("Database List");
        setName(DBListClass.class.getName());

        TextAreaClass sql_class = new TextAreaClass(this);
        sql_class.setName("sql");
        sql_class.setPrettyName("Hibernate Query");
        sql_class.setSize(80);
        sql_class.setRows(5);
        safeput("sql", sql_class);

        StringClass classname_class = new StringClass(this);
        classname_class.setName("classname");
        classname_class.setPrettyName("XWiki Class Name");
        classname_class.setSize(20);
        safeput("classname", classname_class);

        StringClass idfield_class = new StringClass(this);
        idfield_class.setName("idField");
        idfield_class.setPrettyName("Id Field Name");
        idfield_class.setSize(20);
        safeput("idField", idfield_class);

        StringClass valuefield_class = new StringClass(this);
        valuefield_class.setName("valueField");
        valuefield_class.setPrettyName("Value Field Name");
        valuefield_class.setSize(20);
        safeput("valueField", valuefield_class);
    }

    @Override
    public BaseCollection newObject(XWikiContext context)
    {
        return new DBListClass();
    }
}
