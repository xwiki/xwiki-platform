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
package org.xwiki.internal.velocity;

import org.apache.velocity.VelocityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.internal.script.XWikiScriptContextInitializer;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.logging.internal.DefaultLoggerConfiguration;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.properties.ConverterManager;
import org.xwiki.script.internal.DefaultScriptContextManager;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.skin.Skin;
import org.xwiki.skin.SkinManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.internal.DefaultVelocityConfiguration;
import org.xwiki.velocity.internal.VelocityExecutionContextInitializer;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.template.InternalTemplateManager;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link XWikiVelocityManager}.
 *
 * @version $Id$
 */
@ComponentList({
    DefaultScriptContextManager.class,
    XWikiScriptContextInitializer.class,
    DefaultVelocityConfiguration.class,
    DefaultLoggerConfiguration.class
})
@OldcoreTest
public class XWikiVelocityManagerTest
{
    private static final DocumentReference TEMPLATE_DOCUMENT = new DocumentReference("xwiki", "XWiki", "TestMacros");

    private static final DocumentReference SCRIPT_USER = new DocumentReference("xwiki", "XWiki", "Script");

    @MockComponent
    private ContextualLocalizationManager localizationManager;

    @InjectMockComponents
    private XWikiVelocityManager velocityManager;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private SkinManager skinManager;

    @Mock
    private Skin skin;

    @MockComponent
    private InternalTemplateManager templateManager;

    @Mock
    private Template mainMacrosTemplate;

    @Mock
    private TemplateContent mainMacrosTemplateContent;

    @Mock
    private Template skinMacrosTemplate;

    @Mock
    private TemplateContent skinMacrosTemplateContent;

    @MockComponent
    private ConverterManager converterManager;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.oldcore.getExecutionContext().setProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID,
            new VelocityContext());

        when(this.skinManager.getCurrentSkin(true)).thenReturn(this.skin);

        when(this.templateManager.getSkinTemplate("macros.vm")).thenReturn(this.skinMacrosTemplate);
        when(this.skinMacrosTemplate.getId()).thenReturn("testMacros");
        when(this.skinMacrosTemplate.getContent()).thenReturn(this.skinMacrosTemplateContent);
        when(this.skinMacrosTemplateContent.getDocumentReference()).thenReturn(TEMPLATE_DOCUMENT);
        when(this.skinMacrosTemplateContent.getContent()).thenReturn("");

        DocumentAuthorizationManager authorizationManager = this.oldcore.getMockDocumentAuthorizationManager();
        when(authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, SCRIPT_USER, TEMPLATE_DOCUMENT))
            .thenReturn(true);
    }

    // Tests

    @Test
    void getVelocityContext()
    {
        VelocityContext context = this.velocityManager.getVelocityContext();

        assertNull(context.get("doc"));
        assertNull(context.get("sdoc"));

        DocumentReference docReference = new DocumentReference("wiki", "space", "doc");
        DocumentReference sdocReference = new DocumentReference("wiki", "space", "sdoc");

        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(docReference));
        this.oldcore.getXWikiContext().put("sdoc", new XWikiDocument(sdocReference));

        context = this.velocityManager.getVelocityContext();

        Document doc = (Document) context.get("doc");
        assertNotNull(doc);

        Document sdoc = (Document) context.get("sdoc");
        assertNotNull(sdoc);

        // Instances are kept the same when the documents in the context don't change
        context = this.velocityManager.getVelocityContext();
        assertSame(doc, context.get("doc"));
        assertSame(sdoc, context.get("sdoc"));

        // Instances change when the documents in the context change
        docReference = new DocumentReference("wiki", "space", "doc2");
        sdocReference = new DocumentReference("wiki", "space", "sdoc2");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(docReference));
        this.oldcore.getXWikiContext().put("sdoc", new XWikiDocument(sdocReference));

        context = this.velocityManager.getVelocityContext();
        assertNotNull(context.get("doc"));
        assertNotSame(doc, context.get("doc"));
        assertNotNull(context.get("sdoc"));
        assertNotSame(sdoc, context.get("sdoc"));
    }

    @Test
    void checkMacrosInjectionWithoutScriptRights() throws Exception
    {
        VelocityEngine engine = this.velocityManager.getVelocityEngine();
        verify(engine, never()).addGlobalMacros(anyMap());
    }

    @Test
    void checkMacrosInjectionWithScriptRights() throws Exception
    {
        when(this.skinMacrosTemplateContent.getAuthorReference()).thenReturn(SCRIPT_USER);
        VelocityEngine engine = this.velocityManager.getVelocityEngine();
        verify(engine).addGlobalMacros(anyMap());
    }
}
