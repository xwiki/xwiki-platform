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
package com.xpn.xwiki.plugin.multiwiki.doc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.applicationmanager.core.doc.objects.classes.DefaultSuperDocument;
import com.xpn.xwiki.doc.XWikiDocument;

public class XWikiServer extends DefaultSuperDocument
{
    protected static final Log LOG = LogFactory.getLog(XWikiServer.class);

    public XWikiServer(XWikiDocument xdoc, XWikiContext context) throws XWikiException
    {
        super(XWikiServerClass.getInstance(context), xdoc, context);
    }

    public void delete() throws XWikiException
    {
        super.delete(context);
    }

    public String getWikiName()
    {
        return sclass.getItemDefaultName(getFullName(), context);
    }

    public void setWikiName(String wikiName)
    {
        getDoc().setFullName(sclass.getItemDocumentDefaultFullName(wikiName, context));
    }

    public String getOwner()
    {
        return getStringValue(XWikiServerClass.FIELD_owner);
    }

    public void setOwner(String owner)
    {
        setStringValue(XWikiServerClass.FIELD_owner, owner);
    }

    public String getDescription()
    {
        return getStringValue(XWikiServerClass.FIELD_description);
    }

    public void setDescription(String description)
    {
        setLargeStringValue(XWikiServerClass.FIELD_description, description);
    }

    public String getServer()
    {
        return getStringValue(XWikiServerClass.FIELD_server);
    }

    public void setServer(String server)
    {
        setStringValue(XWikiServerClass.FIELD_server, server);
    }

    public String getVisibility()
    {
        return getStringValue(XWikiServerClass.FIELD_visibility);
    }

    public void setVisibility(String visibility)
    {
        setStringValue(XWikiServerClass.FIELD_visibility, visibility);
    }

    public String getLanguage()
    {
        return getStringValue(XWikiServerClass.FIELD_language);
    }

    public void setLanguage(String language)
    {
        setStringValue(XWikiServerClass.FIELD_language, language);
    }

    public String getState()
    {
        return getStringValue(XWikiServerClass.FIELD_state);
    }

    public void setState(String state)
    {
        setStringValue(XWikiServerClass.FIELD_state, state);
    }

    /**
     * {@inheritDoc}
     * @see Object#toString()
     */
    public String toString()
    {
        return getFullName() + "," + getServer() + "," + getOwner();
    }
}
