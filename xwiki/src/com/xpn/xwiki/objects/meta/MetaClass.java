/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
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
 * Date: 19 déc. 2003
 * Time: 18:49:14
 */
package com.xpn.xwiki.objects.meta;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.ElementInterface;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;

public class MetaClass extends BaseClass {

    private static MetaClass metaClass = new MetaClass();

    public MetaClass() {
        NumberMetaClass numberclass = new NumberMetaClass();
        safeput(numberclass.getName(), numberclass);
        StringMetaClass stringclass = new StringMetaClass();
        safeput(stringclass.getName(), stringclass);
    }

    public void safeput(String name, ElementInterface property) {
        super.safeput("meta" + name, property);    //To change body of overriden methods use Options | File Templates.
    }

    public ElementInterface safeget(String name) {
        return super.safeget("meta" + name);    //To change body of overriden methods use Options | File Templates.
    }

    public ElementInterface get(String name) {
        return safeget(name);
    }

    public void put(String name, ElementInterface property) {
        safeput(name, property);
    }

    public static MetaClass getMetaClass() {
        return metaClass;
    }

    public static void setMetaClass(MetaClass metaClass) {
        MetaClass.metaClass = metaClass;
    }

    public BaseCollection newObject() {
        return new BaseClass();
    }

}
