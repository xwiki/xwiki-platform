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
package org.xwiki.rendering.internal.transformation;

import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.skin.Skin;
import org.xwiki.skin.SkinManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiRenderingContext}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiRenderingContextTest
{
    private static final String NAMESPACE = "wiki:Space.Page";

    @InjectMockComponents
    private XWikiRenderingContext renderingContext;

    @MockComponent
    private Execution execution;

    @MockComponent
    private Provider<SkinManager> skinManagerProvider;

    @MockComponent
    private VelocityManager velocityManager;

    @Mock
    private VelocityEngine velocityEngine;

    @Mock
    private Transformation transformation;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void beforeEach()
    {
        when(this.execution.getContext()).thenReturn(new ExecutionContext());
    }

    @Test
    void pushAndPopWithNamespace() throws Exception
    {
        when(this.velocityManager.getVelocityEngine()).thenReturn(this.velocityEngine);

        this.renderingContext.push(this.transformation, new XDOM(List.of()), Syntax.XWIKI_2_1, NAMESPACE, false,
            Syntax.XHTML_1_0);
        verify(this.velocityEngine).startedUsingMacroNamespace(NAMESPACE);

        this.renderingContext.pop();
        verify(this.velocityEngine).stoppedUsingMacroNamespace(NAMESPACE);
    }

    @Test
    void pushAndPopWithoutNamespace()
    {
        this.renderingContext.push(this.transformation, new XDOM(List.of()), Syntax.XWIKI_2_1, null, false,
            Syntax.XHTML_1_0);
        this.renderingContext.pop();

        // Without a transformation id there is no macro namespace to notify the Velocity engine about.
        verifyNoInteractions(this.velocityManager);
    }

    @Test
    void getTargetSyntaxFromContext() throws Exception
    {
        when(this.velocityManager.getVelocityEngine()).thenReturn(this.velocityEngine);

        this.renderingContext.push(this.transformation, new XDOM(List.of()), Syntax.XWIKI_2_1, NAMESPACE, false,
            Syntax.XHTML_1_0);

        assertEquals(Syntax.XHTML_1_0, this.renderingContext.getTargetSyntax());
        verifyNoInteractions(this.skinManagerProvider);
    }

    @Test
    void getTargetSyntaxFallsBackToSkin()
    {
        SkinManager skinManager = mock();
        Skin skin = mock();
        when(this.skinManagerProvider.get()).thenReturn(skinManager);
        when(skinManager.getCurrentSkin(true)).thenReturn(skin);
        when(skin.getOutputSyntax()).thenReturn(Syntax.XHTML_1_0);

        assertEquals(Syntax.XHTML_1_0, this.renderingContext.getTargetSyntax());
    }

    @Test
    void pushAndPopWhenVelocityEngineFails() throws Exception
    {
        when(this.velocityManager.getVelocityEngine()).thenThrow(new XWikiVelocityException("failure"));

        this.renderingContext.push(this.transformation, new XDOM(List.of()), Syntax.XWIKI_2_1, NAMESPACE, false,
            Syntax.XHTML_1_0);
        this.renderingContext.pop();

        assertEquals("Failed to notify Velocity Macro cache for opening the [" + NAMESPACE
            + "] namespace. Reason = [XWikiVelocityException: failure]", this.logCapture.getMessage(0));
        assertEquals("Failed to notify Velocity Macro cache for closing the [" + NAMESPACE
            + "] namespace. Reason = [XWikiVelocityException: failure]", this.logCapture.getMessage(1));
    }
}
