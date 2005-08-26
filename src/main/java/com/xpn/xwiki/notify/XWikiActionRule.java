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
 * Date: 31 juil. 2004
 * Time: 13:20:13
 */
package com.xpn.xwiki.notify;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

public class XWikiActionRule implements XWikiNotificationRule {
    private XWikiActionNotificationInterface target;

    public XWikiActionRule() {
    }

    public XWikiActionRule(XWikiActionNotificationInterface target) {
        setTarget(target);
    }

    public XWikiActionNotificationInterface getTarget() {
        return target;
    }

    public void setTarget(XWikiActionNotificationInterface target) {
        this.target = target;
    }

    public void verify(XWikiDocument newdoc, XWikiDocument olddoc, XWikiContext context) {
    }

    public void verify(XWikiDocument doc, String action, XWikiContext context) {
        try {
            getTarget().notify(this, doc, action, context);
        } catch (Throwable e) {
            // Notification should never fail
            // Just report an error
            e.printStackTrace();
        }
    }

}