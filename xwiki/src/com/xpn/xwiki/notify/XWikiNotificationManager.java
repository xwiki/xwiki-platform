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
 * Date: 23 janv. 2004
 * Time: 18:10:49
 */
package com.xpn.xwiki.notify;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocInterface;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

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

    public void verify(XWikiDocInterface newdoc, XWikiDocInterface olddoc, int event, XWikiContext context) {
        // Call the document notification function itself..
        newdoc.notify(null, newdoc, olddoc, event, context);

        // Call rules explicitely for any modifications of this document
        String name = newdoc.getFullName();
        Vector vnamedrules = getNamedRules(name);
        if (vnamedrules!=null) {
            for (int i=0;i<vnamedrules.size();i++)
               ((XWikiNotificationRule)vnamedrules.get(i)).verify(newdoc, olddoc, context);
        }
        for (int i=0;i<generalrules.size();i++)
            ((XWikiNotificationRule)generalrules.get(i)).verify(newdoc, olddoc, context);
    }
}
