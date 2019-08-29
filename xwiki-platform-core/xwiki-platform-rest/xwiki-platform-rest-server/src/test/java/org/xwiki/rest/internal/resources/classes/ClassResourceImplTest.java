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
package org.xwiki.rest.internal.resources.classes;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.Class;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ClassResourceImpl}
 *
 * @version $Id$
 * @since 10.11.10
 * @since 11.3.4
 * @since 11.8RC1
 */
@ComponentTest
public class ClassResourceImplTest
{
    @InjectMockComponents
    private ClassResourceImpl resource;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private ModelFactory modelFactory;

    @Mock
    private XWiki xWiki;

    private List<String> availableClasses = Arrays.asList("Foo.Class1", "XWiki.User", "XWiki.Protected", "Bar.Other");
    private List<DocumentReference> documentReferences = Arrays.asList(
        new DocumentReference("xwiki", "Foo", "Class1"),
        new DocumentReference("xwiki", "XWiki", "User"),
        new DocumentReference("xwiki", "XWiki", "Protected"),
        new DocumentReference("xwiki", "Bar", "Other")
    );
    private List<Class> restClasses;

    XWikiContext xcontext;

    @BeforeComponent
    public void beforeComponent() throws Exception
    {
        this.xcontext = mock(XWikiContext.class);
        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty("xwikicontext", this.xcontext);
        Execution execution = componentManager.registerMockComponent(Execution.class);
        when(execution.getContext()).thenReturn(executionContext);
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
        Utils.setComponentManager(componentManager);
    }

    @BeforeEach
    public void configure() throws Exception
    {
        when(xcontextProvider.get()).thenReturn(xcontext);
        when(xcontext.getWiki()).thenReturn(xWiki);
        when(xWiki.getClassList(xcontext)).thenReturn(availableClasses);
        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUri()).thenReturn(new URI("/xwiki/rest"));
        ReflectionUtils.setFieldValue(resource, "uriInfo", uriInfo);
        when(authorization.hasAccess(eq(Right.VIEW), any())).thenReturn(true);

        this.restClasses = new ArrayList<>();
        for (int i = 0; i < availableClasses.size(); i++) {
            when(resolver.resolve(eq(availableClasses.get(i)), any())).thenReturn(documentReferences.get(i));
            XWikiDocument doc = mock(XWikiDocument.class);
            BaseClass baseClass = mock(BaseClass.class);
            Class zeclass = mock(Class.class);
            when(xWiki.getDocument(documentReferences.get(i), xcontext)).thenReturn(doc);
            when(doc.getXClass()).thenReturn(baseClass);
            when(modelFactory.toRestClass(any(), eq(new com.xpn.xwiki.api.Class(baseClass, xcontext))))
                .thenReturn(zeclass);
            when(zeclass.getId()).thenReturn(availableClasses.get(i));
            restClasses.add(zeclass);
        }

        componentManager.registerComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed", resolver);
    }

    @Test
    public void authorizedClassesOnly() throws XWikiRestException
    {
        when(authorization.hasAccess(eq(Right.VIEW), eq(new DocumentReference("xwiki", "XWiki", "Protected"))))
            .thenReturn(false);

        String protectedClass = "XWiki.Protected";
        for (String availableClass : availableClasses) {
            try {
                Class aClass = resource.getClass("xwiki", availableClass);
                if (availableClass.equals(protectedClass)) {
                    fail();
                } else {
                    assertEquals(availableClass, aClass.getId());
                }
            } catch (WebApplicationException e) {
                if (availableClass.equals(protectedClass)) {
                    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), e.getResponse().getStatus());
                } else {
                    throw e;
                }
            }
        }
    }
}
