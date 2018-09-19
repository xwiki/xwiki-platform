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
import java.util.Arrays;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rest.Relations;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.rest.resources.classes.ClassPropertyValuesProvider;
import org.xwiki.rest.resources.classes.ClassPropertyValuesResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DBListClass;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ClassPropertyValuesResourceImpl}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
public class ClassPropertyValuesResourceImplTest
{
    @Rule
    public MockitoComponentMockingRule<ClassPropertyValuesResource> mocker =
        new MockitoComponentMockingRule<ClassPropertyValuesResource>(ClassPropertyValuesResourceImpl.class);

    private DocumentReferenceResolver<String> resolver;

    private ContextualAuthorizationManager authorization;

    private ClassPropertyReference propertyReference =
        new ClassPropertyReference("status", new DocumentReference("wiki", Arrays.asList("Path", "To"), "Class"));

    private BaseClass xclass = mock(BaseClass.class);

    private ClassPropertyValuesResource resource;

    @Before
    public void configure() throws Exception
    {
        this.resolver = this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING);
        this.authorization = this.mocker.getInstance(ContextualAuthorizationManager.class);

        when(this.resolver.resolve("Path.To.Class", propertyReference.extractReference(EntityType.WIKI)))
            .thenReturn((DocumentReference) propertyReference.getParent());

        XWikiContext xcontext = mock(XWikiContext.class);
        XWiki xwiki = mock(XWiki.class);
        XWikiDocument classDocument = mock(XWikiDocument.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xwiki.getDocument(propertyReference, xcontext)).thenReturn(classDocument);
        when(classDocument.getXClass()).thenReturn(this.xclass);

        ExecutionContext executionContext = new ExecutionContext();
        executionContext.setProperty("xwikicontext", xcontext);
        Execution execution = mock(Execution.class);
        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(Execution.class)).thenReturn(execution);
        when(execution.getContext()).thenReturn(executionContext);

        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(xcontext);

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUri()).thenReturn(new URI("/xwiki/rest"));
        this.resource = this.mocker.getComponentUnderTest();
        ReflectionUtils.setFieldValue(resource, "uriInfo", uriInfo);
    }

    @Test
    public void getClassPropertyValuesUnauthorized() throws Exception
    {
        try {
            this.resource.getClassPropertyValues("wiki", "Path.To.Class", "status", 6, Arrays.asList("text"), false);
            fail();
        } catch (WebApplicationException expected) {
            assertEquals(Status.UNAUTHORIZED.getStatusCode(), expected.getResponse().getStatus());
        }
    }

    @Test
    public void getClassPropertyValuesNotFound() throws Exception
    {
        when(this.authorization.hasAccess(Right.VIEW, this.propertyReference)).thenReturn(true);

        try {
            this.resource.getClassPropertyValues("wiki", "Path.To.Class", "status", 6, Arrays.asList("text"), false);
            fail();
        } catch (WebApplicationException expected) {
            assertEquals(Status.NOT_FOUND.getStatusCode(), expected.getResponse().getStatus());
        }
    }

    @Test
    public void getClassPropertyValues() throws Exception
    {
        when(this.authorization.hasAccess(Right.VIEW, this.propertyReference)).thenReturn(true);
        when(this.xclass.get("status")).thenReturn(new DBListClass());

        PropertyValues values = new PropertyValues();
        ClassPropertyValuesProvider propertyValuesProvider = this.mocker.getInstance(ClassPropertyValuesProvider.class);
        when(propertyValuesProvider.getValues(this.propertyReference, 6, "one", "two")).thenReturn(values);

        assertSame(values,
            this.resource.getClassPropertyValues("wiki", "Path.To.Class", "status", 6, Arrays.asList("one", "two"),
                false));

        assertEquals(1, values.getLinks().size());
        Link propertyLink = values.getLinks().get(0);
        assertEquals("/xwiki/rest/wikis/wiki/classes/Path.To.Class/properties/status", propertyLink.getHref());
        assertEquals(Relations.PROPERTY, propertyLink.getRel());
    }
}
