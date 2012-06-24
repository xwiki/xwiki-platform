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
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public class MetaClass extends BaseClass
{
    private static MetaClass metaClass = new MetaClass();

    public MetaClass()
    {
        NumberMetaClass numberclass = new NumberMetaClass();
        safeput(numberclass.getName(), numberclass);
        StringMetaClass stringclass = new StringMetaClass();
        safeput(stringclass.getName(), stringclass);
        TextAreaMetaClass textareaclass = new TextAreaMetaClass();
        safeput(textareaclass.getName(), textareaclass);
        PasswordMetaClass passwdclass = new PasswordMetaClass();
        safeput(passwdclass.getName(), passwdclass);
        BooleanMetaClass booleanclass = new BooleanMetaClass();
        safeput(booleanclass.getName(), booleanclass);
        StaticListMetaClass listclass = new StaticListMetaClass();
        safeput(listclass.getName(), listclass);
        DBListMetaClass dblistclass = new DBListMetaClass();
        safeput(dblistclass.getName(), dblistclass);
        DBTreeListMetaClass dbtreelistclass = new DBTreeListMetaClass();
        safeput(dbtreelistclass.getName(), dbtreelistclass);
        DateMetaClass dateclass = new DateMetaClass();
        safeput(dateclass.getName(), dateclass);
        GroupsMetaClass groupsclass = new GroupsMetaClass();
        safeput(groupsclass.getName(), groupsclass);
        UsersMetaClass usersclass = new UsersMetaClass();
        safeput(usersclass.getName(), usersclass);
        LevelsMetaClass levelsclass = new LevelsMetaClass();
        safeput(levelsclass.getName(), levelsclass);
    }

    @Override
    public void safeput(String name, PropertyInterface property)
    {
        addField("meta" + name, property);
        if (property instanceof PropertyClass) {
            ((PropertyClass) property).setObject(this);
            ((BaseProperty) property).setName(name);
        }
    }

    @Override
    public PropertyInterface safeget(String name)
    {
        return super.safeget("meta" + name);
    }

    @Override
    public PropertyInterface get(String name)
    {
        return safeget(name);
    }

    @Override
    public void put(String name, PropertyInterface property)
    {
        safeput(name, property);
    }

    public static MetaClass getMetaClass()
    {
        return metaClass;
    }

    public static void setMetaClass(MetaClass metaClass)
    {
        MetaClass.metaClass = metaClass;
    }

    @Override
    public BaseCollection newObject(XWikiContext context)
    {
        return new BaseClass();
    }
}
