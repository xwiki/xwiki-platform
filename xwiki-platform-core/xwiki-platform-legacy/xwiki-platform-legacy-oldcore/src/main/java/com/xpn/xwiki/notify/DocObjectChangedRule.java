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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.xwiki.model.reference.DocumentReference;

@Deprecated
public class DocObjectChangedRule extends DocChangeRule
{
    private String className;

    public DocObjectChangedRule()
    {
    }

    public DocObjectChangedRule(XWikiDocChangeNotificationInterface target)
    {
        setTarget(target);
    }

    public DocObjectChangedRule(XWikiDocChangeNotificationInterface target, String classname)
    {
        setTarget(target);
        setClassName(classname);
    }

    public boolean hasEqualsObjectsFromClass(XWikiDocument newdoc, XWikiDocument olddoc, String classname)
    {
        Vector vobj1 = (newdoc == null) ? null : newdoc.getObjects(classname);
        Vector vobj2 = (olddoc == null) ? null : olddoc.getObjects(classname);
        if ((vobj1 == null) && (vobj2 == null))
            return true;
        if ((vobj1 == null) || (vobj2 == null))
            return false;
        if (vobj1.size() != vobj2.size())
            return false;
        for (int i = 0; i < vobj1.size(); i++) {
            if (!((vobj1.get(i) == null) && (vobj2.get(i) == null))) {
                if ((vobj1.get(i) == null) || (vobj2.get(i) == null))
                    return false;
                if (!vobj1.get(i).equals(vobj2.get(i)))
                    return false;
            }
        }
        return true;
    }

    private boolean hasEqualsObjectsFromClass(XWikiDocument newdoc, XWikiDocument olddoc, DocumentReference classreference)
    {
        List<BaseObject> vobj1 = (newdoc == null) ? null : newdoc.getXObjects(classreference);
        List<BaseObject> vobj2 = (olddoc == null) ? null : olddoc.getXObjects(classreference);
        if ((vobj1 == null) && (vobj2 == null))
            return true;
        if ((vobj1 == null) || (vobj2 == null))
            return false;
        if (vobj1.size() != vobj2.size())
            return false;
        for (int i = 0; i < vobj1.size(); i++) {
            if (!((vobj1.get(i) == null) && (vobj2.get(i) == null))) {
                if ((vobj1.get(i) == null) || (vobj2.get(i) == null))
                    return false;
                if (!vobj1.get(i).equals(vobj2.get(i)))
                    return false;
            }
        }
        return true;
    }

    public boolean hasEqualsObjectsForAllClasses(XWikiDocument newdoc, XWikiDocument olddoc)
    {
        Map<DocumentReference, List<BaseObject>> fields1 = newdoc.getXObjects();
        Map<DocumentReference, List<BaseObject>> fields2 = (olddoc == null) ? null : olddoc.getXObjects();
        if ((fields1 == null) && (fields2 == null))
            return true;
        if ((fields1 == null) || (fields2 == null))
            return false;
        if (fields1.size() != fields2.size())
            return false;
        for (Iterator<DocumentReference> it = fields1.keySet().iterator(); it.hasNext();) {
            if (!hasEqualsObjectsFromClass(newdoc, olddoc, it.next()))
                return false;
        }
        return true;
    }

    public String getClassName()
    {
        return className;
    }

    public void setClassName(String className)
    {
        this.className = className;
    }

    public boolean hasEqualObjects(XWikiDocument newdoc, XWikiDocument olddoc, String className)
    {
        if (className == null)
            return hasEqualsObjectsForAllClasses(newdoc, olddoc);
        else
            return hasEqualsObjectsFromClass(newdoc, olddoc, className);
    }

    public void verify(XWikiDocument newdoc, XWikiDocument olddoc, XWikiContext context)
    {
        if (!hasEqualObjects(newdoc, olddoc, className))
            getTarget().notify(this, newdoc, olddoc, XWikiDocChangeNotificationInterface.EVENT_CHANGE, context);
    }

}
