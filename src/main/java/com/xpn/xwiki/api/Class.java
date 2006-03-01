/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author ludovic
 * @author erwan
 * @author sdumitriu
 */


package com.xpn.xwiki.api;

import java.util.Iterator;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;

public class Class extends Collection {

    public Class(BaseClass obj, XWikiContext context) {
        super(obj, context);
    }

    protected BaseClass getBaseClass() {
        return (BaseClass) getCollection();
    }

    /**
     *
     * @return an array with the properties of the class
     */
    public Element[] getProperties() {
        java.util.Collection coll = getCollection().getFieldList();
        if (coll==null)
         return null;
        PropertyClass[] properties = new PropertyClass[coll.size()];
        int i=0;
        for (Iterator it = coll.iterator(); it.hasNext();i++) {
            properties[i] = new PropertyClass((com.xpn.xwiki.objects.classes.PropertyClass) it.next(), context);
        }
        return properties;
    }

    /**
     *
     * @param name the name of the element
     * @return the PropertyClass for the given name
     * @see PropertyClass
     * @see Element
     */
    public Element get(String name) {
       return new PropertyClass((com.xpn.xwiki.objects.classes.PropertyClass) getCollection().safeget(name), context);
    }

    /**
     *
     * @return the BaseClass (without the wrapping) if you have the programming right.
     */
    public BaseClass getXWikiClass() {
        if (checkProgrammingRights())
         return (BaseClass) getCollection();
        else
         return null;
    }

    /**
     *
     * @return a new object from this class
     * @throws XWikiException
     */
    public Object newObject() throws XWikiException {
        BaseObject obj = (BaseObject)getBaseClass().newObject(context);
        return obj.newObjectApi(obj, context);
    }

}