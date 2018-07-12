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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
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
    @InjectMockitoOldcore
    private MockitoOldcore oldCore;

    @MockComponent
    private AuthorExecutor authorExecutor;

    private BaseClass xclass = new BaseClass();

    @BeforeEach
    public void before() throws Exception
    {
        DocumentReference classReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Class");
        XWikiDocument classDocument = new XWikiDocument(classReference);
        classDocument.setSyntax(Syntax.XWIKI_2_1);
        doReturn(classDocument).when(this.oldCore.getSpyXWiki()).getDocument(classReference,
            this.oldCore.getXWikiContext());

        this.xclass.setOwnerDocument(classDocument);

        this.oldCore.getMocker().registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        this.oldCore.getMocker().registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
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

        displayCustomWithAuthor(authorReference);
    }

    @Test
    public void displayCustomWithClassDisplayerAndClassIsNew() throws Exception
    {
        DocumentReference userReference = new DocumentReference("wiki", "XWiki", "Alice");
        this.oldCore.getXWikiContext().setUserReference(userReference);

        displayCustomWithAuthor(userReference);
    }

    @Test
    public void displayCustomWithClassDisplayerAndGuestAuthor() throws Exception
    {
        DocumentReference userReference = new DocumentReference("wiki", "XWiki", "Alice");
        this.oldCore.getXWikiContext().setUserReference(userReference);

        this.xclass.getOwnerDocument().setNew(false);

        displayCustomWithAuthor(null);
    }

    private void displayCustomWithAuthor(DocumentReference authorReference) throws Exception
    {
        when(this.authorExecutor.call(any(Callable.class), eq(authorReference))).thenReturn("output");

        PropertyClass propertyClass = new PropertyClass();
        propertyClass.setCustomDisplay("test");
        propertyClass.setObject(this.xclass);

        StringBuffer buffer = new StringBuffer();

        propertyClass.displayCustom(buffer, "date", "Path.To.Class_0_", "edit", new BaseObject(),
            this.oldCore.getXWikiContext());

        assertEquals("output", buffer.toString());
    }
}
