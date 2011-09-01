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
public class DocChangeRule implements XWikiNotificationRule
{
    protected static final Log LOG = LogFactory.getLog(DocChangeRule.class);

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

    public DocChangeRule(XWikiDocChangeNotificationInterface target, boolean pre, boolean post)
    {
        setTarget(target);
        setPreverify(pre);
        setPostverify(post);
    }

    public XWikiDocChangeNotificationInterface getTarget()
    {
        return this.target;
    }

    public void setTarget(XWikiDocChangeNotificationInterface target)
    {
        this.target = target;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.notify.XWikiNotificationRule#verify(com.xpn.xwiki.doc.XWikiDocument,
     *      com.xpn.xwiki.doc.XWikiDocument, com.xpn.xwiki.XWikiContext)
     */
    public void verify(XWikiDocument newdoc, XWikiDocument olddoc, XWikiContext context)
    {
        if (!isPostverify()) {
            return;
        }

        try {
            getTarget().notify(this, newdoc, olddoc, XWikiDocChangeNotificationInterface.EVENT_CHANGE, context);
        } catch (Exception e) {
            // protect from bad listeners
            LOG.error("Fail to notify target [" + getTarget() + "] for event [" + this + ", newdoc=" + newdoc
                + ", olddoc=" + olddoc + "]", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.notify.XWikiNotificationRule#preverify(com.xpn.xwiki.doc.XWikiDocument,
     *      com.xpn.xwiki.doc.XWikiDocument, com.xpn.xwiki.XWikiContext)
     */
    public void preverify(XWikiDocument newdoc, XWikiDocument olddoc, XWikiContext context)
    {
        if (!isPreverify()) {
            return;
        }

        try {
            getTarget().notify(this, newdoc, olddoc, XWikiDocChangeNotificationInterface.EVENT_CHANGE, context);
        } catch (Exception e) {
            // protect from bad listeners
            LOG.error("Fail to notify target [" + getTarget() + "] for pre event [" + this + ", newdoc=" + newdoc
                + ", olddoc=" + olddoc + "]", e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.notify.XWikiNotificationRule#verify(com.xpn.xwiki.doc.XWikiDocument, java.lang.String,
     *      com.xpn.xwiki.XWikiContext)
     */
    public void verify(XWikiDocument doc, String action, XWikiContext context)
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.notify.XWikiNotificationRule#preverify(com.xpn.xwiki.doc.XWikiDocument, java.lang.String,
     *      com.xpn.xwiki.XWikiContext)
     */
    public void preverify(XWikiDocument doc, String action, XWikiContext context)
    {
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
