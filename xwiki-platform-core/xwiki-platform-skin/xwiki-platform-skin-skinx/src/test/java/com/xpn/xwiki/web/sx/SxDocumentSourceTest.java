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
package com.xpn.xwiki.web.sx;

import java.io.Writer;
import java.util.concurrent.Callable;

import javax.inject.Named;

import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.resources.LESSResourceReferenceFactory;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SxDocumentSource}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class SxDocumentSourceTest
{
    private static final DocumentReference CLASS_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "JavaScriptExtension");

    private static final DocumentReference DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", "MySpace", "MyExtension");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Author");

    private static final String CONTENT = "var x = \"$doc.fullName\";";

    private static final String SCRIPT_RIGHT_WARNING =
        "The Velocity content of the skin extension [Object xwiki:MySpace.MyExtension^XWiki.JavaScriptExtension[0]] "
            + "is ignored because of lack of script right from the author.";

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private DocumentAuthorizationManager authorizationManager;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    private VelocityManager velocityManager;

    @MockComponent
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @MockComponent
    private LESSCompiler lessCompiler;

    @MockComponent
    private LESSResourceReferenceFactory lessResourceReferenceFactory;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    private XWikiDocument document;

    private BaseObject extensionObject;

    @BeforeEach
    void setUp() throws Exception
    {
        UserReference effectiveMetadataAuthor = mock(UserReference.class);
        when(this.userReferenceSerializer.serialize(effectiveMetadataAuthor)).thenReturn(AUTHOR_REFERENCE);

        this.document = new XWikiDocument(DOCUMENT_REFERENCE);
        this.document.getAuthors().setEffectiveMetadataAuthor(effectiveMetadataAuthor);

        this.extensionObject = new BaseObject();
        this.extensionObject.setXClassReference(CLASS_REFERENCE);
        this.extensionObject.setLargeStringValue("code", CONTENT);
        this.document.addXObject(this.extensionObject);

        XWikiContext context = this.oldcore.getXWikiContext();
        context.setDoc(this.document);
    }

    private String getContent()
    {
        return new SxDocumentSource(this.oldcore.getXWikiContext(), new JsExtension()).getContent();
    }

    private void setScriptRight(boolean hasScriptRight)
    {
        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, AUTHOR_REFERENCE,
            DOCUMENT_REFERENCE)).thenReturn(hasScriptRight);
    }

    private void mockVelocityEvaluation(String evaluated) throws Exception
    {
        VelocityEngine velocityEngine = mock(VelocityEngine.class);
        when(this.velocityManager.getVelocityEngine()).thenReturn(velocityEngine);
        when(this.velocityManager.getVelocityContext()).thenReturn(new VelocityContext());
        doAnswer(invocation -> {
            invocation.<Writer>getArgument(1).write(evaluated);
            return true;
        }).when(velocityEngine).evaluate(any(), any(), any(), eq(CONTENT));
        when(this.authorExecutor.call(any(), eq(AUTHOR_REFERENCE), eq(DOCUMENT_REFERENCE)))
            .then(invocation -> invocation.<Callable<?>>getArgument(0).call());
    }

    @Test
    void getContentWithParseAndScriptRight() throws Exception
    {
        this.extensionObject.setIntValue("parse", 1);
        setScriptRight(true);
        mockVelocityEvaluation("var x = \"MySpace.MyExtension\";");

        assertEquals("var x = \"MySpace.MyExtension\";\n", getContent());
    }

    @Test
    void getContentWithParseAndNoScriptRight()
    {
        this.extensionObject.setIntValue("parse", 1);
        setScriptRight(false);

        assertEquals(CONTENT + "\n", getContent());

        verifyNoInteractions(this.authorExecutor);
        verifyNoInteractions(this.velocityManager);
        assertEquals(1, this.logCapture.size());
        assertEquals(SCRIPT_RIGHT_WARNING, this.logCapture.getMessage(0));
    }

    @Test
    void getContentWithoutParseAndNoScriptRight()
    {
        this.extensionObject.setIntValue("parse", 0);
        setScriptRight(false);

        assertEquals(CONTENT + "\n", getContent());

        verifyNoInteractions(this.authorExecutor);
        verifyNoInteractions(this.velocityManager);
    }

    @Test
    void getContentWithLessAndNoScriptRight() throws Exception
    {
        this.extensionObject.setIntValue("parse", 1);
        this.extensionObject.setStringValue("contentType", "LESS");
        setScriptRight(false);

        LESSResourceReference lessResourceReference = mock(LESSResourceReference.class);
        when(this.lessResourceReferenceFactory.createReferenceForXObjectProperty(any(ObjectPropertyReference.class)))
            .thenReturn(lessResourceReference);
        when(this.lessCompiler.compile(eq(lessResourceReference), anyBoolean(), anyBoolean(), anyBoolean()))
            .thenReturn("compiled");

        assertEquals("compiled\n", getContent());

        verify(this.lessCompiler).compile(lessResourceReference, true, false, false);
        assertEquals(1, this.logCapture.size());
        assertEquals(SCRIPT_RIGHT_WARNING, this.logCapture.getMessage(0));
    }
}
