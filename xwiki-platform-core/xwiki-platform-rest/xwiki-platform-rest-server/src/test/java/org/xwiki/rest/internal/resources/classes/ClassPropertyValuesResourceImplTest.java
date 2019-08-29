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

import javax.inject.Named;
import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.rest.Relations;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.rest.resources.classes.ClassPropertyValuesProvider;
import org.xwiki.security.authorization.AccessDeniedException;
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
import com.xpn.xwiki.objects.classes.DBListClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ClassPropertyValuesResourceImpl}.
 * 
 * @version $Id$
 * @since 9.8RC1
 */
@ComponentTest
public class ClassPropertyValuesResourceImplTest
{
    @InjectMockComponents
    private ClassPropertyValuesResourceImpl resource;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    @Named("currentmixed")
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    private ClassPropertyReference propertyReference =
        new ClassPropertyReference("status", new DocumentReference("wiki", Arrays.asList("Path", "To"), "Class"));

    @Mock
    private BaseClass xclass;

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
    }

    @BeforeEach
    public void configure() throws Exception
    {
        when(xcontextProvider.get()).thenReturn(xcontext);
        when(this.resolver.resolve("Path.To.Class", propertyReference.extractReference(EntityType.WIKI)))
            .thenReturn((DocumentReference) propertyReference.getParent());

        XWiki xwiki = mock(XWiki.class);
        XWikiDocument classDocument = mock(XWikiDocument.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        when(xwiki.getDocument(propertyReference, xcontext)).thenReturn(classDocument);
        when(classDocument.getXClass()).thenReturn(this.xclass);

        UriInfo uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUri()).thenReturn(new URI("/xwiki/rest"));
        ReflectionUtils.setFieldValue(resource, "uriInfo", uriInfo);
    }

    @Test
    public void getClassPropertyValuesUnauthorized() throws Exception
    {
        doThrow(new AccessDeniedException(xcontext.getUserReference(), this.propertyReference)).when(
            authorization).checkAccess(eq(Right.VIEW), eq(this.propertyReference));
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
        when(this.xclass.get("status")).thenReturn(new DBListClass());

        PropertyValues values = new PropertyValues();
        ClassPropertyValuesProvider propertyValuesProvider = this.componentManager
            .getInstance(ClassPropertyValuesProvider.class);
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
