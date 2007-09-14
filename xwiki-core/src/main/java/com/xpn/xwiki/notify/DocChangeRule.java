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
 *
 */

package com.xpn.xwiki.notify;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

public class DocChangeRule implements XWikiNotificationRule
{
    private XWikiDocChangeNotificationInterface target;
    private boolean preverify = false;
    private boolean postverify = true;

    public DocChangeRule()
    {
    }

    public DocChangeRule(XWikiDocChangeNotificationInterface target)
    {
        setTarget(target);
    }

    public DocChangeRule(XWikiDocChangeNotificationInterface target, boolean pre, boolean post) {
        setTarget(target);
        setPreverify(pre);
        setPostverify(post);
    }

    public XWikiDocChangeNotificationInterface getTarget()
    {
        return target;
    }

    public void setTarget(XWikiDocChangeNotificationInterface target)
    {
        this.target = target;
    }

    public void verify(XWikiDocument newdoc, XWikiDocument olddoc, XWikiContext context)
    {
        if(!isPostverify()) return;
        getTarget().notify(this, newdoc, olddoc,
            XWikiDocChangeNotificationInterface.EVENT_CHANGE, context);
    }

    public void preverify(XWikiDocument newdoc, XWikiDocument olddoc, XWikiContext context)
    {
        if(!isPreverify()) return;
        getTarget().notify(this, newdoc, olddoc,
            XWikiDocChangeNotificationInterface.EVENT_CHANGE, context);
    }

    public void verify(XWikiDocument doc, String action, XWikiContext context)
    {
    }

    public void preverify(XWikiDocument doc, String action, XWikiContext context)
    {
    }

    public boolean isPostverify() {
        return postverify;
    }

    public void setPostverify(boolean postnotify) {
        this.postverify = postnotify;
    }

    public boolean isPreverify() {
        return preverify;
    }

    public void setPreverify(boolean prenotify) {
        this.preverify = prenotify;
    }
}
