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
package com.xpn.xwiki.plugin.invitationmanager.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.plugin.invitationmanager.api.JoinRequest;

/**
 * The default implementation of {@link JoinRequest}
 * 
 * @version $Id: $
 */
public abstract class JoinRequestImpl extends Document implements JoinRequest
{

    public static interface JoinRequestFields
    {
        String REQUEST_DATE = "requestDate";

        String RESPONSE_DATE = "responseDate";

        String ROLES = "roles";

        String SPACE = "space";

        String STATUS = "status";

        String TEXT = "text";

        String MAP = "map";
    }

    protected InvitationManagerImpl manager;

    public JoinRequestImpl(InvitationManagerImpl manager, XWikiContext context)
    {
        super(null, context);
        this.manager = manager;
    }

    public JoinRequestImpl(String fullName, InvitationManagerImpl manager, XWikiContext context)
        throws XWikiException
    {
        this(manager, context);
        // we initialize the request document
        doc = context.getWiki().getDocument(fullName, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see JoinRequest#getMap()
     */
    public Map getMap()
    {
        String content = (String) getValue(JoinRequestFields.MAP, getObject(getClassName()));
        Map map = new HashMap();
        if (content == null)
            return map;
        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String[] mapEntry = lines[i].split("\\|");
            if (mapEntry.length > 1)
                map.put(decode(mapEntry[0]), decode(mapEntry[1]));
        }
        return map;
    }

    private Object decode(String s)
    {
        return s.replaceAll("%\\_\\_%", "|").replaceAll("%NL%", "\n");
    }

    private Object encode(String s)
    {
        return s.replaceAll("\\|", "%__%").replaceAll("\n", "%NL%");
    }

    /**
     * {@inheritDoc}
     * 
     * @see JoinRequest#getRequestDate()
     */
    public Date getRequestDate()
    {
        return (Date) getValue(JoinRequestFields.REQUEST_DATE, getObject(getClassName()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see JoinRequest#getResponseDate()
     */
    public Date getResponseDate()
    {
        return (Date) getValue(JoinRequestFields.RESPONSE_DATE, getObject(getClassName()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see JoinRequest#getRoles()
     */
    public List getRoles()
    {
        return (List) getValue(JoinRequestFields.ROLES, getObject(getClassName()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see JoinRequest#getSpace()
     */
    public String getSpace()
    {
        String space = (String) getValue(JoinRequestFields.SPACE, getObject(getClassName()));
        return (space == null) ? "" : space;
    }

    /**
     * {@inheritDoc}
     * 
     * @see JoinRequest#getStatus()
     */
    public int getStatus()
    {
        String sstatus = (String) getValue(JoinRequestFields.STATUS, getObject(getClassName()));
        if ((sstatus==null)||(sstatus.trim().equals(""))) {
          return 0;
        } else {
         Integer status = Integer.parseInt(sstatus);
         return (int)((status == null) ? 0 : status);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see JoinRequest#getText()
     */
    public String getText()
    {
        return (String) getValue(JoinRequestFields.TEXT, getObject(getClassName()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see JoinRequest#setMap(Map)
     */
    public void setMap(Map map)
    {
        StringBuffer content = new StringBuffer();
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            content.append(encode((String) entry.getKey()) + "|"
                + encode((String) entry.getValue()) + "\n");
        }
        getDoc().getObject(getClassName()).setLargeStringValue(JoinRequestFields.MAP,
            content.toString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see JoinRequest#setRequestDate(Date)
     */
    public void setRequestDate(Date requestDate)
    {
        getDoc().getObject(getClassName()).setDateValue(JoinRequestFields.REQUEST_DATE,
            requestDate);
    }

    /**
     * {@inheritDoc}
     * 
     * @see JoinRequest#setResponseDate(Date)
     */
    public void setResponseDate(Date responseDate)
    {
        getDoc().getObject(getClassName()).setDateValue(JoinRequestFields.RESPONSE_DATE,
            responseDate);
    }

    /**
     * {@inheritDoc}
     * 
     * @see JoinRequest#setRoles(List)
     */
    public void setRoles(List roles)
    {
        getDoc().getObject(getClassName()).setDBStringListValue(JoinRequestFields.ROLES, roles);
    }

    /**
     * {@inheritDoc}
     * 
     * @see JoinRequest#setSpace(String)
     */
    public void setSpace(String space)
    {
        getDoc().getObject(getClassName()).setStringValue(JoinRequestFields.SPACE, space);
    }

    /**
     * {@inheritDoc}
     * 
     * @see JoinRequest#setStatus(int)
     */
    public void setStatus(int status)
    {
        getDoc().getObject(getClassName()).setStringValue(JoinRequestFields.STATUS, ""+status);
    }

    /**
     * {@inheritDoc}
     * 
     * @see JoinRequest#setText(String)
     */
    public void setText(String text)
    {
        getDoc().getObject(getClassName()).setLargeStringValue(JoinRequestFields.TEXT, text);
    }

    protected abstract String getClassName();
}
