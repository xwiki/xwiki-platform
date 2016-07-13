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
package com.xpn.xwiki.internal.objects.classes;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.internal.DefaultModelConfiguration;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.DefaultStringDocumentReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceResolver;
import org.xwiki.model.internal.reference.DefaultStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.LocalUidStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.internal.DefaultObservationManager;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.ComponentList;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XClassPropertyEventGeneratorListener;
import com.xpn.xwiki.internal.model.reference.CurrentEntityReferenceProvider;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentStringDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentStringEntityReferenceResolver;
import com.xpn.xwiki.internal.store.PropertyConverter;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.test.MockitoOldcoreRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link XClassMigratorListener}.
 * 
 * @version $Id$
 */
@ComponentList({
    XClassMigratorListener.class,
    DefaultObservationManager.class,
    LocalStringEntityReferenceSerializer.class,
    DefaultStringDocumentReferenceResolver.class,
    PropertyConverter.class,
    CurrentReferenceDocumentReferenceResolver.class,
    CurrentReferenceEntityReferenceResolver.class,
    CurrentEntityReferenceProvider.class,
    DefaultModelConfiguration.class,
    DefaultStringEntityReferenceResolver.class,
    DefaultEntityReferenceProvider.class,
    XClassPropertyEventGeneratorListener.class,
    CurrentStringDocumentReferenceResolver.class,
    CurrentStringEntityReferenceResolver.class,
    LocalUidStringEntityReferenceSerializer.class,
    DefaultStringEntityReferenceSerializer.class,
    DefaultSymbolScheme.class
})
public class XClassMigratorListenerTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    private XWikiDocument xobjectDocument;

    private XWikiDocument xclassDocument;

    private QueryManager mockQueryManager;

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.mockQueryManager = this.oldcore.getMocker().registerMockComponent(QueryManager.class);
    }

    @Before
    public void before() throws Exception
    {
        this.xclassDocument = new XWikiDocument(new DocumentReference("wiki", "Space", "Class"));
        this.xclassDocument.setSyntax(Syntax.PLAIN_1_0);

        this.xobjectDocument = new XWikiDocument(new DocumentReference("wiki", "Space", "Page"));
        this.xobjectDocument.setSyntax(Syntax.PLAIN_1_0);
        BaseObject xobject = new BaseObject();
        xobject.setXClassReference(this.xclassDocument.getDocumentReference());
        this.xobjectDocument.addXObject(xobject);

        Query query = mock(Query.class);
        when(this.mockQueryManager.createQuery("from doc.object(Space.Class) as obj", Query.XWQL)).thenReturn(query);
        when(query.<String>execute()).thenReturn(Arrays.asList("Space.Page"));

        // We need document modification notifications
        this.oldcore.notifyDocumentUpdatedEvent(true);
    }

    @Test
    public void migrateProperty() throws Exception
    {
        this.xclassDocument.getXClass().addTextField("property", "property", 30);
        saveXClassDocument();
        this.xobjectDocument.setStringValue(this.xclassDocument.getDocumentReference(), "property", "42");
        this.oldcore.getSpyXWiki().saveDocument(this.xobjectDocument, this.oldcore.getXWikiContext());

        // Modify the class
        this.xclassDocument.getXClass().removeField("property");
        this.xclassDocument.getXClass().addNumberField("property", "property", 30, "integer");
        saveXClassDocument();

        // Verify the document has been modified
        this.xobjectDocument =
            this.oldcore.getSpyXWiki().getDocument(this.xobjectDocument.getDocumentReference(),
                this.oldcore.getXWikiContext());

        assertEquals(42, ((BaseProperty) this.xobjectDocument.getXObject(this.xclassDocument.getDocumentReference())
            .get("property")).getValue());
    }

    private void saveXClassDocument() throws XWikiException
    {
        this.oldcore.getSpyXWiki().saveDocument(this.xclassDocument, this.oldcore.getXWikiContext());
        this.xclassDocument =
            this.oldcore.getSpyXWiki().getDocument(this.xclassDocument.getDocumentReference(),
                this.oldcore.getXWikiContext());
    }
}
