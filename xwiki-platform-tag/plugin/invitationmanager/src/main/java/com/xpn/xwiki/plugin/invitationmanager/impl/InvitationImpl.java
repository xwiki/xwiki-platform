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

import java.util.Collections;
import java.util.List;
import java.util.Map;


import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.invitationmanager.api.Invitation;

/**
 * The default implementation of {@link Invitation}
 * 
 * @version $Id: $
 */
public class InvitationImpl extends JoinRequestImpl implements Invitation
{
    public static interface InvitationFields extends JoinRequestFields
    {
        String INVITEE = "invitee";

        String INVITER = "inviter";

        String CODE = "code";

        String OPEN = "open";
    }

    public InvitationImpl(String invitee, String space, boolean create,
        InvitationManagerImpl manager, XWikiContext context) throws XWikiException
    {
        this(invitee, null, space, false, Collections.EMPTY_LIST, Collections.EMPTY_MAP, create,
            manager, context);
    }

    public InvitationImpl(String invitee, String inviter, String space, boolean open, List roles,
        Map map, boolean create, InvitationManagerImpl manager, XWikiContext context)
        throws XWikiException
    {
        super(manager, context);
        // we initialize the request document
        initRequestDoc(space, invitee);
        // if we are asked to create the request
        if (create) {
            // we created it if it does not yet exist, otherwise pass
            if (isNew())
                createRequestDoc(invitee, inviter, space, open, roles, map);      
        }
    }

    /**
     * Allows to load an invitation from a wiki document
     * @param fullName
     * @param manager
     * @param context
     * @throws XWikiException
     */
    public InvitationImpl(String fullName, InvitationManagerImpl manager, XWikiContext context)
        throws XWikiException
    {
        super(fullName, manager, context);
    }

    protected String getClassName() {
       return manager.getInvitationClassName();
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see Invitation#getCode()
     */
    public String getCode()
    {
        String code = (String) getValue(InvitationFields.CODE, getObject(getClassName()));
        return (code==null) ? "" : code;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Invitation#getInvitee()
     */
    public String getInvitee()
    {
        String invitee = (String) getValue(InvitationFields.INVITEE, getObject(getClassName()));
        return (invitee==null) ? "" : invitee;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Invitation#getInviter()
     */
    public String getInviter()
    {
        String inviter = (String) getValue(InvitationFields.INVITER, getObject(getClassName()));
        return (inviter==null) ? "" : inviter;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Invitation#isOpen()
     */
    public boolean isOpen()
    {
        Integer open = (Integer) getValue(InvitationFields.OPEN, getObject(getClassName()));
        return ((open!=null)&&(open.intValue()>0));
    }

    /**
     * {@inheritDoc}
     * 
     * @see Invitation#setCode(String)
     */
    public void setCode(String code)
    {
        getDoc().getObject(getClassName()).setStringValue(InvitationFields.CODE, code);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Invitation#setInvitee(String)
     */
    public void setInvitee(String invitee)
    {
        getDoc().getObject(getClassName()).setStringValue(InvitationFields.INVITEE, invitee);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Invitation#setInviter(String)
     */
    public void setInviter(String inviter)
    {
        getDoc().getObject(getClassName()).setStringValue(InvitationFields.INVITER, inviter);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Invitation#setOpen(boolean)
     */
    public void setOpen(boolean open)
    {
        getDoc().getObject(getClassName()).setIntValue(InvitationFields.OPEN, open ? 1 : 0);
    }

    protected void initRequestDoc(String space, String invitee) throws XWikiException
    {
        String docName = manager.getJoinRequestDocumentName("Invitation", space, invitee, context);
        doc = context.getWiki().getDocument(docName, context);
    }

    protected void createRequestDoc(String invitee, String inviter, String space, boolean open,
        List roles, Map map)
    {
        XWikiDocument requestDoc = getDoc();
        String className = getClassName();
        BaseObject requestObj = new BaseObject();
        requestObj.setName(doc.getFullName());
        requestObj.setClassName(className);
        requestDoc.addObject(className, requestObj);

        setInvitee(invitee);
        setInviter(inviter);
        setSpace(space);
        setOpen(open);
        setRoles(roles);
        setMap(map);
    }
}
