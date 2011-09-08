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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

@Deprecated
public class XWikiActionRule implements XWikiNotificationRule
{
    protected static final Log LOG = LogFactory.getLog(XWikiActionRule.class);

    private XWikiActionNotificationInterface target;

    private boolean preverify = false;

    private boolean postverify = true;

    public XWikiActionRule()
    {
    }

    public XWikiActionRule(XWikiActionNotificationInterface target)
    {
        setTarget(target);
    }

    public XWikiActionRule(XWikiActionNotificationInterface target, boolean pre, boolean post)
    {
        setTarget(target);
        setPreverify(pre);
        setPostverify(post);
    }

    public XWikiActionNotificationInterface getTarget()
    {
        return this.target;
    }

    public void setTarget(XWikiActionNotificationInterface target)
    {
        this.target = target;
    }

    public void verify(XWikiDocument newdoc, XWikiDocument olddoc, XWikiContext context)
    {
    }

    public void preverify(XWikiDocument newdoc, XWikiDocument olddoc, XWikiContext context)
    {
    }

    public void verify(XWikiDocument doc, String action, XWikiContext context)
    {
        if (!isPostverify())
            return;
        try {
            getTarget().notify(this, doc, action, context);
        } catch (Throwable e) {
            // protect from bad listeners
            LOG.error("Fail to notify target [" + getTarget() + "] for event [" + this + ", doc=" + doc + ", action="
                + action + "]", e);
        }
    }

    public void preverify(XWikiDocument doc, String action, XWikiContext context)
    {
        if (!isPreverify())
            return;
        try {
            getTarget().notify(this, doc, action, context);
        } catch (Throwable e) {
            // protect from bad listeners
            LOG.error("Fail to notify target [" + getTarget() + "] for pre event [" + this + ", doc=" + doc
                + ", action=" + action + "]", e);
        }
    }

    public boolean isPostverify()
    {
        return this.postverify;
    }

    public void setPostverify(boolean postnotify)
    {
        this.postverify = postnotify;
    }

    public boolean isPreverify()
    {
        return this.preverify;
    }

    public void setPreverify(boolean prenotify)
    {
        this.preverify = prenotify;
    }

}
