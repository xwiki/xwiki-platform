/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 23 janv. 2004
 * Time: 18:30:41
 */
package com.xpn.xwiki.notify;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

public class DocChangeRule implements XWikiNotificationRule {
    private XWikiDocChangeNotificationInterface target;

    public DocChangeRule() {
      }

    public DocChangeRule(XWikiDocChangeNotificationInterface target) {
        setTarget(target);
    }

    public XWikiDocChangeNotificationInterface getTarget() {
        return target;
    }

    public void setTarget(XWikiDocChangeNotificationInterface target) {
        this.target = target;
    }

    public void verify(XWikiDocument newdoc, XWikiDocument olddoc, XWikiContext context) {
        getTarget().notify(this, newdoc, olddoc, XWikiDocChangeNotificationInterface.EVENT_CHANGE, context);
    }

    public void verify(XWikiDocument doc, String action, XWikiContext context) {
    }
}
