/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 5 févr. 2004
 * Time: 16:34:41
 */
package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.classes.DBListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;

public class DBListMetaClass extends ListMetaClass {

    public DBListMetaClass() {
        super();
        setPrettyName("Database List Class");
        setName(DBListClass.class.getName());

        TextAreaClass sql_class = new TextAreaClass(this);
        sql_class.setName("sql");
        sql_class.setPrettyName("Hibernate Query");
        sql_class.setSize(80);
        sql_class.setRows(5);
        safeput("sql", sql_class);
    }

    public BaseCollection newObject() {
        return new DBListClass();
    }
}

