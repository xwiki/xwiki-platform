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
package com.xpn.xwiki.notify;

import java.util.Vector;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * XWikiNotificationRule to check weather an object property has changed.
 * <br>
 * The rules are as follows:
 * <ul>
 * <li>if an object of the specified class is added or deleted a notification is sent</li>
 * <li>if the objects list is the same then the values of the specified property in {@code newdoc} 
 * and {@code olddoc} are checked on corresponding objects - the correspondence is done based on 
 * the index of the objects in the list of objects of type {@code classname}.</li>
 * <li>if {@code classname} or {@code propertyName} are not specified upon instantiation 
 * ({@code null} or empty string), this {@code PropertyChangedRule} will never notify.</li>
 * </ul>
 */
@Deprecated
public class PropertyChangedRule extends DocChangeRule {
    private String className;
    private String propertyName;

    public PropertyChangedRule(XWikiDocChangeNotificationInterface target,String classname, String propertyName) {
        setTarget(target);
        setClassName(classname);
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
        Vector vobj1 = (newdoc == null) ? null : newdoc.getObjects(className);
        Vector vobj2 = (olddoc == null) ? null : olddoc.getObjects(className);
        if ((vobj1 == null) && (vobj2 == null)) {
            return true;
        }
        if ((vobj1 == null) || (vobj2 == null)) {
            return false;
        }
        if (vobj1.size() != vobj2.size()) {
            return false;
        }
        for (int i = 0; i < vobj1.size(); i++) {
            if (!((vobj1.get(i) == null) && (vobj2.get(i) == null))) {
                if (vobj1.get(i) == null) {
                    if (((BaseObject)vobj2.get(i)).safeget(propertyName) == null) {
                        return true;
                    } else {
                        return false;
                    }
                }
                if (vobj2.get(i) == null) {
                    if (((BaseObject)vobj1.get(i)).safeget(propertyName) == null) {
                        return true;
                    } else {
                        return false;
                    }
                }
                BaseProperty prop1 = 
                    (BaseProperty) ((BaseObject)vobj1.get(i)).safeget(propertyName);
                BaseProperty prop2 = 
                    (BaseProperty) ((BaseObject)vobj2.get(i)).safeget(propertyName);
                if ((prop1 == null) && (prop2 == null)) {
                    return true;
                }
                if ((prop1 == null) || (prop2 == null)) {
                    return false;
                }
                if (!prop1.equals(prop2)) {
                    // Found different property on an object, we can stop and return false 
                    return false;
                }
            }
        }
        return true;
    }

    public void verify(XWikiDocument newdoc, XWikiDocument olddoc, XWikiContext context) {
        if (this.className == null || this.className.equals("") 
                || this.propertyName == null || this.propertyName.equals("")) {
            // Not enough information, never notify
            return;
        }
        if (!hasEqualProperty(newdoc, olddoc, getClassName(), getPropertyName()))
            getTarget().notify(this, newdoc, olddoc, XWikiDocChangeNotificationInterface.EVENT_CHANGE, context);
    }
}
