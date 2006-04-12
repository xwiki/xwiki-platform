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
 * @author sdumitriu
 */

package com.xpn.xwiki.notify;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

public class XWikiNotificationManager {
    private Vector generalrules = new Vector();
    private Map namedrules = new HashMap();

    public XWikiNotificationManager() {
    }

    public void addGeneralRule(XWikiNotificationRule rule) {
        generalrules.add(rule);
    }

    public void removeGeneralRule(XWikiNotificationRule rule) {
        generalrules.remove(rule);
    }

    public void addNamedRule(String name, XWikiNotificationRule rule) {
        Vector vnamedrules = (Vector) namedrules.get(name);
        if (vnamedrules==null) {
            vnamedrules = new Vector();
            namedrules.put(name, vnamedrules);
        }
        vnamedrules.add(rule);
    }

    public void removeNamedRule(String name) {
        Vector vnamedrules = (Vector) namedrules.get(name);
        if (vnamedrules!=null) {
            vnamedrules.remove(name);
            if (vnamedrules.size()==0)
                namedrules.remove(name);
        }
    }

    public Vector getNamedRules(String name) {
        return (Vector) namedrules.get(name);
    }

    public void verify(XWikiDocument newdoc, XWikiDocument olddoc, int event, XWikiContext context) {
        // Call the document notification function itself..
        newdoc.notify(null, newdoc, olddoc, event, context);

        // Call rules explicitely for any actions of this document
        String name = newdoc.getFullName();
        Vector vnamedrules = getNamedRules(name);
        if (vnamedrules!=null) {
            for (int i=0;i<vnamedrules.size();i++)
               ((XWikiNotificationRule)vnamedrules.get(i)).verify(newdoc, olddoc, context);
        }

        name = context.getDatabase() + ":" + newdoc.getFullName();
        vnamedrules = getNamedRules(name);
        if (vnamedrules!=null) {
            for (int i=0;i<vnamedrules.size();i++)
               ((XWikiNotificationRule)vnamedrules.get(i)).verify(newdoc, olddoc, context);
        }

        for (int i=0;i<generalrules.size();i++)
            ((XWikiNotificationRule)generalrules.get(i)).verify(newdoc, olddoc, context);
    }

    public void verify(XWikiDocument doc, String action, XWikiContext context) {
        // Call rules explicitely for any actions of this document
        String name = doc.getFullName();
        Vector vnamedrules = getNamedRules(name);
        if (vnamedrules!=null) {
            for (int i=0;i<vnamedrules.size();i++)
               ((XWikiNotificationRule)vnamedrules.get(i)).verify(doc, action, context);
        }
        for (int i=0;i<generalrules.size();i++)
            ((XWikiNotificationRule)generalrules.get(i)).verify(doc, action, context);
    }

}
