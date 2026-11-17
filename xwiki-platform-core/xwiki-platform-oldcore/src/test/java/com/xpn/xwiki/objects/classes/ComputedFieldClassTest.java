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

import java.util.concurrent.Callable;

import javax.script.SimpleScriptContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.script.ScriptContextManager;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ComputedFieldClass}.
 *
 * @version $Id$
 */
@OldcoreTest
class ComputedFieldClassTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private static final DocumentReference CLASS_REFERENCE = new DocumentReference("wiki", "Space", "Class");

    private static final DocumentReference ADMIN_REFERENCE = new DocumentReference("wiki", "XWiki", "Admin");

    private static final DocumentReference USER_REFERENCE = new DocumentReference("wiki", "XWiki", "User");

    private static final String SCRIPT = "{{velocity}}$name{{/velocity}}";

    @MockComponent
    private ScriptContextManager scriptContextManager;

    @MockComponent
    private AuthorExecutor authorExecutor;

    private XWikiDocument contextDocument;

    private BaseObject object;

    @BeforeEach
    void beforeEach() throws Exception
    {
        when(this.scriptContextManager.getCurrentScriptContext()).thenReturn(new SimpleScriptContext());

        // Needed to set the context user.
        this.oldcore.getMocker().registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");

        this.contextDocument = mock();
        this.oldcore.getXWikiContext().setDoc(this.contextDocument);
        this.oldcore.getXWikiContext().setUserReference(USER_REFERENCE);

        // The stored class document, authored by a user with more rights than the current user.
        XWikiDocument storedClassDocument = new XWikiDocument(CLASS_REFERENCE);
        storedClassDocument.setSyntax(Syntax.XWIKI_2_1);
        storedClassDocument.setAuthorReference(ADMIN_REFERENCE);
        this.object = mock();
        when(this.object.getXClass(this.oldcore.getXWikiContext())).thenReturn(storedClassDocument.getXClass());
    }

    private void mockExecution(DocumentReference authorReference, boolean restricted) throws Exception
    {
        when(this.authorExecutor.call(any(), eq(authorReference), eq(CLASS_REFERENCE)))
            .then(invocationOnMock -> {
                Callable<String> callable = invocationOnMock.getArgument(0);
                return callable.call();
            });
        when(this.contextDocument.getRenderedContent(SCRIPT, Syntax.XWIKI_2_1.toIdString(), restricted,
            this.oldcore.getXWikiContext())).thenReturn("result");
    }

    private ComputedFieldClass createComputedField(XWikiDocument classDocument)
    {
        ComputedFieldClass computedFieldClass = new ComputedFieldClass();
        computedFieldClass.setName("prop");
        computedFieldClass.setScript(SCRIPT);
        if (classDocument != null) {
            BaseClass xclass = classDocument.getXClass();
            xclass.addField("prop", computedFieldClass);
            computedFieldClass.setObject(xclass);
        }
        return computedFieldClass;
    }

    @Test
    void getComputedValueWithAuthorOfStoredClass() throws Exception
    {
        mockExecution(ADMIN_REFERENCE, false);

        assertEquals("result", createComputedField(null).getComputedValue("prop", "", this.object,
            this.oldcore.getXWikiContext()));
    }

    @Test
    void getComputedValueWithAuthorOfClassHoldingProperty() throws Exception
    {
        // In-memory version of the class (e.g. being edited) whose author has been changed to the current user.
        XWikiDocument classDocument = new XWikiDocument(CLASS_REFERENCE);
        classDocument.setSyntax(Syntax.XWIKI_2_1);
        classDocument.setNew(false);
        classDocument.setAuthorReference(USER_REFERENCE);
        mockExecution(USER_REFERENCE, false);

        StringBuffer buffer = new StringBuffer();
        createComputedField(classDocument).displayEdit(buffer, "prop", "", this.object,
            this.oldcore.getXWikiContext());

        assertEquals("result", buffer.toString());
    }

    @Test
    void getComputedValueWithRestrictedClassDocument() throws Exception
    {
        XWikiDocument classDocument = new XWikiDocument(CLASS_REFERENCE);
        classDocument.setSyntax(Syntax.XWIKI_2_1);
        classDocument.setNew(false);
        classDocument.setAuthorReference(ADMIN_REFERENCE);
        classDocument.setRestricted(true);
        mockExecution(ADMIN_REFERENCE, true);

        assertEquals("result", createComputedField(classDocument).getComputedValue("prop", "", this.object,
            this.oldcore.getXWikiContext()));
    }

    @Test
    void getComputedValueWithNewClassDocument() throws Exception
    {
        XWikiDocument classDocument = new XWikiDocument(CLASS_REFERENCE);
        classDocument.setSyntax(Syntax.XWIKI_2_1);
        mockExecution(USER_REFERENCE, false);

        assertEquals("result", createComputedField(classDocument).getComputedValue("prop", "", this.object,
            this.oldcore.getXWikiContext()));
    }

    @Test
    void displayViewEscapesErrorMessage()
    {
        when(this.scriptContextManager.getCurrentScriptContext())
            .thenThrow(new RuntimeException("Failed & <b>{{macro}}"));

        ComputedFieldClass computedFieldClass = new ComputedFieldClass();
        computedFieldClass.setName("prop");

        StringBuffer buffer = new StringBuffer();
        computedFieldClass.displayView(buffer, "prop", "", new BaseObject(), this.oldcore.getXWikiContext());

        assertEquals("Failed &#38; &#60;b>&#123;&#123;macro}}", buffer.toString());
    }
}
