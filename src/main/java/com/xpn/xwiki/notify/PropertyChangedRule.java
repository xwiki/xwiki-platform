/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
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
 */

package com.xpn.xwiki.notify;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

public class PropertyChangedRule extends DocChangeRule {
    private String className;
    private String propertyName;

    public PropertyChangedRule(XWikiDocChangeNotificationInterface target,String classname, String propertyName) {
        setTarget(target);
        setPropertyName(propertyName);
    }


    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = propertyName;
    }

    public boolean hasEqualProperty(XWikiDocument newdoc, XWikiDocument olddoc,
                                    String className, String propertyName) {
        BaseObject obj1 = newdoc.getxWikiObject();
        BaseObject obj2 = (olddoc==null) ? null : olddoc.getxWikiObject();
        if ((obj1==null) && (obj2==null))
            return true;
        if (obj1==null) {
            if (obj2.safeget(propertyName)==null)
                return true;
            else
                return false;
        }
        if (obj2==null) {
            if (obj1.safeget(propertyName)==null)
                return true;
            else
                return false;
        }
        BaseProperty prop1 = (BaseProperty) obj1.safeget(propertyName);
        BaseProperty prop2 = (BaseProperty) obj2.safeget(propertyName);
        if ((prop1==null)&&(prop2==null))
            return true;
        if ((prop1==null)||(prop2==null))
                    return false;
        return prop1.equals(prop2);
    }


    public void verify(XWikiDocument newdoc, XWikiDocument olddoc, XWikiContext context) {
        if (!hasEqualProperty(newdoc, olddoc, getClassName(), getPropertyName()))
            getTarget().notify(this, newdoc, olddoc, XWikiDocChangeNotificationInterface.EVENT_CHANGE, context);
    }
}
