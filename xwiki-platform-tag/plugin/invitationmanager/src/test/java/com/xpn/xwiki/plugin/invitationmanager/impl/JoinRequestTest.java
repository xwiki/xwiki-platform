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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.invitationmanager.api.JoinRequest;
import com.xpn.xwiki.plugin.invitationmanager.api.JoinRequestStatus;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Unit tests for classes implementing {@link JoinRequest} interface
 */
public abstract class JoinRequestTest extends AbstractBridgedXWikiComponentTestCase
{
    protected JoinRequest joinRequest;

    protected XWiki xwiki;

    protected Mock mockXWikiStore;

    protected Mock mockXWikiRightService;

    protected Map docs = new HashMap();

    protected void setUp() throws Exception
    {
        super.setUp();

        xwiki = new XWiki(new XWikiConfig(), getContext());
        getContext().setWiki(xwiki);

        mockXWikiStore =
            mock(XWikiHibernateStore.class, new Class[] {XWiki.class, XWikiContext.class},
                new Object[] {xwiki, getContext()});
        mockXWikiStore.stubs().method("loadXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument shallowDoc = (XWikiDocument) invocation.parameterValues.get(0);

                    if (docs.containsKey(shallowDoc.getFullName())) {
                        return (XWikiDocument) docs.get(shallowDoc.getFullName());
                    } else {
                        return shallowDoc;
                    }
                }
            });
        this.mockXWikiStore.stubs().method("saveXWikiDoc").will(
            new CustomStub("Implements XWikiStoreInterface.saveXWikiDoc")
            {
                public Object invoke(Invocation invocation) throws Throwable
                {
                    XWikiDocument document = (XWikiDocument) invocation.parameterValues.get(0);
                    document.setNew(false);
                    document.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
                    docs.put(document.getFullName(), document);
                    return null;
                }
            });

        mockXWikiRightService = mock(XWikiRightService.class, new Class[] {}, new Object[] {});
        mockXWikiRightService.stubs().method("hasAccessLevel").withAnyArguments().will(
            returnValue(true));

        xwiki.setStore((XWikiStoreInterface) mockXWikiStore.proxy());
        xwiki.setRightService((XWikiRightService) mockXWikiRightService.proxy());
    }

    /**
     * test for {@link JoinRequest#getMap()}
     */
    public void testMap()
    {
        Map map = new HashMap();
        map.put("allowMailNotifications", "true");
        map.put("notifyChanges", "false");
        joinRequest.setMap(map);
        assertEquals(map, joinRequest.getMap());
    }

    /**
     * test for {@link JoinRequest#getRequestDate()}
     */
    public void testRequestDate()
    {
        Date requestDate = new Date();
        joinRequest.setRequestDate(requestDate);
        assertEquals(requestDate, joinRequest.getRequestDate());
    }

    /**
     * test for {@link JoinRequest#getResponseDate()}
     */
    public void testResponseDate()
    {
        Date responseDate = new Date();
        joinRequest.setResponseDate(responseDate);
        assertEquals(responseDate, joinRequest.getResponseDate());
    }

    /**
     * test for {@link JoinRequest#getRoles()}
     */
    public void testRoles()
    {
        List roles = new ArrayList();
        roles.add("developer");
        roles.add("admin");
        joinRequest.setRoles(roles);
        assertEquals(roles, joinRequest.getRoles());
    }

    /**
     * test for {@link JoinRequest#getSpace()}
     */
    public void testSpace()
    {
        String space = "Blog";
        joinRequest.setSpace(space);
        assertEquals(space, joinRequest.getSpace());
    }

    /**
     * test for {@link JoinRequest#getStatus()}
     */
    public void testStatus()
    {
        int status = JoinRequestStatus.SENT;
        joinRequest.setStatus(status);
        assertEquals(status, joinRequest.getStatus());
    }

    /**
     * test for {@link JoinRequest#getText()}
     */
    public void testText()
    {
        String text = "I love your space!";
        joinRequest.setText(text);
        assertEquals(text, joinRequest.getText());
    }
}
