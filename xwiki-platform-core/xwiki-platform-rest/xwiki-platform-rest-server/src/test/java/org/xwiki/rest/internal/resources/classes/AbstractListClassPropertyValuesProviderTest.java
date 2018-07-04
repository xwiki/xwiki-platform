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

import java.util.Arrays;

import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryBuilder;
import org.xwiki.query.QueryParameter;
import org.xwiki.rest.resources.classes.ClassPropertyValuesProvider;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Base class for writing unit tests for {@link ClassPropertyValuesProvider}s.
 *
 * @version $Id$
 * @since 9.8
 */
public abstract class AbstractListClassPropertyValuesProviderTest
{
    protected Query allowedValuesQuery = mock(Query.class, "allowed");

    protected Query usedValuesQuery = mock(Query.class, "used");

    protected XWikiContext xcontext = mock(XWikiContext.class);

    protected DocumentReference classReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "MyClass");

    protected XWikiDocument classDocument = mock(XWikiDocument.class);

    @MockComponent
    @Named("usedValues")
    protected QueryBuilder<ListClass> usedValuesQueryBuilder;

    @InjectComponentManager
    protected ComponentManager componentManager;

    public void configure() throws Exception
    {
        Provider<XWikiContext> xcontextProvider = this.componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
        XWiki xwiki = mock(XWiki.class);
        BaseClass xclass = mock(BaseClass.class);
        DocumentReference authorReference = new DocumentReference("wiki", "Users", "Alice");

        when(xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(xwiki);
        when(this.classDocument.getXClass()).thenReturn(xclass);
        when(this.classDocument.getDocumentReference()).thenReturn(this.classReference);
        when(this.classDocument.getAuthorReference()).thenReturn(authorReference);

        QueryParameter queryParameter = mock(QueryParameter.class);
        when(this.allowedValuesQuery.bindValue("text")).thenReturn(queryParameter);
        when(this.usedValuesQuery.bindValue("text")).thenReturn(queryParameter);
        when(queryParameter.anyChars()).thenReturn(queryParameter);
        when(queryParameter.literal("foo")).thenReturn(queryParameter);
    }

    protected void addProperty(String name, PropertyClass definition, boolean withQueryBuilders) throws Exception
    {
        XWiki xwiki = this.xcontext.getWiki();
        BaseClass xclass = this.classDocument.getXClass();
        ClassPropertyReference propertyReference = new ClassPropertyReference(name, this.classReference);

        when(xwiki.getDocument(propertyReference, this.xcontext)).thenReturn(this.classDocument);
        when(xclass.get(name)).thenReturn(definition);
        definition.setOwnerDocument(this.classDocument);

        if (withQueryBuilders) {
            DefaultParameterizedType allowedValuesQueryBuilderType =
                new DefaultParameterizedType(null, QueryBuilder.class, definition.getClass());
            QueryBuilder allowedValuesQueryBuilder = this.componentManager.getInstance(allowedValuesQueryBuilderType);
            when(allowedValuesQueryBuilder.build(definition)).thenReturn(this.allowedValuesQuery);

            if (definition instanceof ListClass) {
                when(this.usedValuesQueryBuilder.build((ListClass) definition)).thenReturn(this.usedValuesQuery);
            }
        }
    }
}
