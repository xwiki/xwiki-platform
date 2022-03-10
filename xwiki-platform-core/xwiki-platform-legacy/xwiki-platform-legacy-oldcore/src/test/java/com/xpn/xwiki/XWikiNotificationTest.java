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
package com.xpn.xwiki;

import java.lang.reflect.InvocationTargetException;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.DocChangeRule;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationManager;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Verify that notifications are correctly sent in the {@link XWiki} class.
 * 
 * @version $Id$
 */
@Deprecated
public class XWikiNotificationTest extends AbstractBridgedXWikiComponentTestCase
{
    public class TestListener implements XWikiDocChangeNotificationInterface
    {
        public boolean hasListenerBeenCalled = false;

        public boolean expectedNewStatus = true;

        public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc, int event,
            XWikiContext context)
        {
            assertEquals("Space.Page", newdoc.getFullName());
            assertNotNull("Shouldn't have been null", olddoc);
            assertEquals("Should have been new, since this is a new document", this.expectedNewStatus, olddoc.isNew());
            this.hasListenerBeenCalled = true;
        }
    }

    XWiki xwiki;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.xwiki = new XWiki();
        getContext().setWiki(this.xwiki);

        Mock mockStore = mock(XWikiStoreInterface.class);
        mockStore.stubs().method("getLimitSize").will(returnValue(255));
        mockStore.expects(atLeastOnce()).method("saveXWikiDoc");
        mockStore.stubs().method("loadXWikiDoc").will(new CustomStub("Implements XWikiStoreInterface.loadXWikiDoc")
        {
            public Object invoke(Invocation invocation) throws Throwable
            {
                return invocation.parameterValues.get(0);
            }
        });
        this.xwiki.setStore((XWikiStoreInterface) mockStore.proxy());
    }

    private XWikiNotificationManager getNotificationManager() throws IllegalArgumentException, SecurityException,
        IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        return (XWikiNotificationManager) XWiki.class.getMethod("getNotificationManager")
            .invoke(getContext().getWiki());
    }

    /**
     * We only verify here that the saveDocument API calls the Notification Manager. Detailed tests of the notification
     * classes are implemented in the notification package.
     */
    public void testSaveDocumentSendNotifications() throws Exception
    {
        TestListener listener = new TestListener();
        getNotificationManager().addGeneralRule(new DocChangeRule(listener));

        XWikiDocument document = new XWikiDocument(new DocumentReference("WikiDescriptor", "Space", "Page"));

        this.xwiki.saveDocument(document, getContext());
        assertTrue("Listener not called", listener.hasListenerBeenCalled);
    }

    /**
     * We only verify here that the saveDocument API calls the Notification Manager. Detailed tests of the notification
     * classes are implemented in the notification package.
     */
    public void testSaveDocumentFromAPIUsesCorrectOriginalDocument() throws Exception
    {
        Mock mockRights = mock(XWikiRightService.class);
        mockRights.stubs().method("hasAccessLevel").will(returnValue(true));
        mockRights.stubs().method("hasProgrammingRights").will(returnValue(true));
        this.xwiki.setRightService((XWikiRightService) mockRights.proxy());

        Mock mockUserReferenceResolver = mock(UserReferenceResolver.class);
        Mock userReference = mock(UserReference.class);
        mockUserReferenceResolver.stubs().method("resolve").will(returnValue(userReference.proxy()));

        DefaultComponentDescriptor<UserReferenceResolver<CurrentUserReference>> userReferenceResolverDescriptor =
            new DefaultComponentDescriptor<>();
        userReferenceResolverDescriptor.setRoleType(
            new DefaultParameterizedType(null, UserReferenceResolver.class, CurrentUserReference.class));
        userReferenceResolverDescriptor.setRoleHint("default");
        getComponentManager().registerComponent(userReferenceResolverDescriptor,
            (UserReferenceResolver<CurrentUserReference>) mockUserReferenceResolver.proxy());

        Mock mockDocumentUserReferenceResolver = mock(UserReferenceResolver.class);
        DefaultComponentDescriptor<UserReferenceResolver<DocumentReference>> documentUserReferenceResolverDescriptor =
            new DefaultComponentDescriptor<>();
        documentUserReferenceResolverDescriptor.setRoleType(
            new DefaultParameterizedType(null, UserReferenceResolver.class, DocumentReference.class));
        documentUserReferenceResolverDescriptor.setRoleHint("document");
        getComponentManager().registerComponent(documentUserReferenceResolverDescriptor,
            (UserReferenceResolver<DocumentReference>) mockDocumentUserReferenceResolver.proxy());

        Mock mockDocumentUserReferenceSerializer = mock(UserReferenceSerializer.class);
        DefaultComponentDescriptor<UserReferenceSerializer<DocumentReference>>
            documentUserReferenceSerializerDescriptor = new DefaultComponentDescriptor<>();
        documentUserReferenceSerializerDescriptor.setRoleType(
            new DefaultParameterizedType(null, UserReferenceSerializer.class, DocumentReference.class));
        documentUserReferenceSerializerDescriptor.setRoleHint("document");
        getComponentManager().registerComponent(documentUserReferenceSerializerDescriptor,
            (UserReferenceSerializer<DocumentReference>) mockDocumentUserReferenceSerializer.proxy());
        mockDocumentUserReferenceSerializer.stubs().method("serialize")
            .will(returnValue(new DocumentReference("wiki", "XWiki", "User")));

        TestListener listener = new TestListener();
        listener.expectedNewStatus = false;
        getNotificationManager().addGeneralRule(new DocChangeRule(listener));

        XWikiDocument original = new XWikiDocument(new DocumentReference("WikiDescriptor", "Space", "Page"));
        original.setNew(false);
        original.setContent("Old content");
        XWikiDocument document = original.clone();
        document.setContent("New content");
        document.setOriginalDocument(original);

        Document api = new Document(document, getContext());
        api.save();
        assertTrue("Listener not called", listener.hasListenerBeenCalled);
    }
}
