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
package com.xpn.xwiki.objects.classes;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.ClassPropertyReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.cache.rendering.RenderingCache;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.objects.meta.TextAreaMetaClass;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the base {@link PropertyClass} class.
 * 
 * @version $Id$
 * @since 2.4M2
 */
@OldcoreTest
public class PropertyClassTest
{
    protected static final String CUSTOM_DISPLAY = "test";

    @InjectMockitoOldcore
    private MockitoOldcore oldCore;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    @Named("configured")
    private DocumentDisplayer documentDisplayer;

    @MockComponent
    private RenderingContext renderingContext;

    // Needed for XWikiDocument#getRenderedContent.
    @MockComponent
    private JobProgressManager jobProgressManager;

    @MockComponent
    private RenderingCache renderingCache;

    @MockComponent
    @Named("html/5.0")
    private BlockRenderer htmlRenderer;

    private BaseClass xclass = new BaseClass();

    @BeforeEach
    public void before() throws Exception
    {
        DocumentReference classReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Class");
        XWikiDocument classDocument = new XWikiDocument(classReference);
        classDocument.setSyntax(Syntax.XWIKI_2_1);
        this.xclass.setOwnerDocument(classDocument);

        this.oldCore.getMocker().registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        this.oldCore.getMocker().registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        this.oldCore.getMocker().registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        this.oldCore.getMocker().registerMockComponent(ContextualLocalizationManager.class);

        DocumentReference contextDocumentReference = new DocumentReference("wiki", "XWiki", "Context");
        this.oldCore.getXWikiContext().setDoc(new XWikiDocument(contextDocumentReference));

        when(this.renderingContext.getTargetSyntax()).thenReturn(Syntax.HTML_5_0);
    }

    /** Test the {@link PropertyClass#compareTo(PropertyClass)} method. */
    @Test
    public void testCompareTo()
    {
        PropertyClass one = new PropertyClass();
        PropertyClass two = new PropertyClass();
        // Random numbers to be used as property indexes.
        int n1, n2;

        one.setName("first");
        two.setName("second");

        // Since the test might randomly succeed, run it several times to be safer.
        Random random = new Random();
        for (int i = 0; i < 20; ++i) {
            n1 = random.nextInt();
            n2 = random.nextInt();
            one.setNumber(n1);
            two.setNumber(n2);

            if (n1 == n2) {
                assertEquals(Math.signum(one.compareTo(two)), -1.0);
                assertEquals(Math.signum(two.compareTo(one)), 1.0);
            } else {
                assertEquals(0, Float.compare(Math.signum(one.compareTo(two)), Math.signum(n1 - n2)));
                assertEquals(0, Float.compare(Math.signum(two.compareTo(one)), Math.signum(n2 - n1)));
            }
        }

        // Also test that the comparison takes into account the name in case the two numbers are identical
        one.setNumber(42);
        two.setNumber(42);
        assertEquals(Math.signum(one.compareTo(two)), -1.0);
        assertEquals(Math.signum(two.compareTo(one)), 1.0);
    }

    @Test
    public void displayCustomWithClassDisplayer() throws Exception
    {
        DocumentReference authorReference = new DocumentReference("wiki", "XWiki", "Alice");
        this.xclass.getOwnerDocument().setAuthorReference(authorReference);

        displayCustomWithAuthor(authorReference, this.xclass.getDocumentReference());
    }

    @Test
    void displayCustomWithRestrictedDocument() throws Exception
    {
        DocumentReference authorReference = new DocumentReference("wiki", "XWiki", "Bob");

        this.xclass.getOwnerDocument().setRestricted(true);
        this.xclass.getOwnerDocument().setAuthorReference(authorReference);

        displayCustomWithAuthor(authorReference, this.xclass.getDocumentReference());

        verify(this.documentDisplayer).display(argThat(doc -> CUSTOM_DISPLAY.equals(doc.getContent())),
            argThat(DocumentDisplayerParameters::isTransformationContextRestricted));
    }

    @Test
    public void displayCustomWithClassDisplayerAndClassIsNew() throws Exception
    {
        DocumentReference userReference = new DocumentReference("wiki", "XWiki", "Alice");
        this.oldCore.getXWikiContext().setUserReference(userReference);

        displayCustomWithAuthor(userReference, this.xclass.getDocumentReference());
    }

    @Test
    public void displayCustomWithClassDisplayerAndGuestAuthor() throws Exception
    {
        DocumentReference userReference = new DocumentReference("wiki", "XWiki", "Alice");
        this.oldCore.getXWikiContext().setUserReference(userReference);

        this.xclass.getOwnerDocument().setNew(false);

        displayCustomWithAuthor(null, this.xclass.getDocumentReference());
    }

    private void displayCustomWithAuthor(DocumentReference authorReference, DocumentReference secureDocument)
        throws Exception
    {
        when(this.authorExecutor.call(any(), eq(authorReference), eq(secureDocument)))
            .then(invocationOnMock -> {
                Callable<String> callable = invocationOnMock.getArgument(0);
                return callable.call();
            });

        String output = "output";
        XDOM mockXDOM = mock();
        when(this.documentDisplayer.display(any(), any())).thenReturn(mockXDOM);

        doAnswer(invocationOnMock -> {
            WikiPrinter printer = invocationOnMock.getArgument(1);
            printer.print(output);
            return null;
        }).when(this.htmlRenderer).render(same(mockXDOM), any());

        PropertyClass propertyClass = new PropertyClass();
        propertyClass.setCustomDisplay(CUSTOM_DISPLAY);
        propertyClass.setObject(this.xclass);

        StringBuffer buffer = new StringBuffer();

        propertyClass.displayCustom(buffer, "date", "Path.To.Class_0_", "edit", new BaseObject(),
            this.oldCore.getXWikiContext());

        assertEquals(output, buffer.toString());
    }

    @Test
    public void getFieldFullNameForClassProperty() throws Exception
    {
        PropertyClass propertyClass = new PropertyClass();
        propertyClass.setName("tags");
        propertyClass.setObject(this.xclass);

        EntityReferenceSerializer<String> localEntityReferenceSerializer =
            this.oldCore.getMocker().getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localEntityReferenceSerializer.serialize(this.xclass.getDocumentReference())).thenReturn("XWiki.TagClass");

        assertEquals("XWiki.TagClass_tags", propertyClass.getFieldFullName());
    }

    @Test
    public void getFieldFullNameForMetaProperty()
    {
        PropertyClass propertyClass = new PropertyClass();
        propertyClass.setName("editor");
        PropertyMetaClass metaClass = new TextAreaMetaClass();
        propertyClass.setxWikiClass(metaClass);
        assertEquals("TextArea_editor", propertyClass.getFieldFullName());
    }

    @Test
    void getReference()
    {
        PropertyClass propertyClass = new PropertyClass();
        propertyClass.setName("tags");
        propertyClass.setObject(this.xclass);
        assertEquals(new ClassPropertyReference("tags", this.xclass.getReference()), propertyClass.getReference());

        // Modify the property name.
        propertyClass.setName("users");
        assertEquals(new ClassPropertyReference("users", this.xclass.getReference()), propertyClass.getReference());

        // Modify the class reference.
        DocumentReference newClassReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "NewClass");
        BaseClass newClass = new BaseClass();
        newClass.setOwnerDocument(new XWikiDocument(newClassReference));
        propertyClass.setObject(newClass);
        assertEquals(new ClassPropertyReference("users", newClass.getReference()), propertyClass.getReference());
    }
}
