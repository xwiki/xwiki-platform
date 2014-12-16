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
package org.xwiki.icon.internal;

import java.io.Writer;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.icon.IconException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link org.xwiki.icon.internal.VelocityRenderer}.
 *
 * @since 6.4M1
 * @version $Id$
 */
public class VelocityRendererTest
{
    @Rule
    public MockitoComponentMockingRule<VelocityRenderer> mocker =
            new MockitoComponentMockingRule<>(VelocityRenderer.class);

    private VelocityManager velocityManager;

    @Before
    public void setUp() throws Exception
    {
        velocityManager = mocker.getInstance(VelocityManager.class);
    }

    @Test
    public void renderTest() throws Exception
    {
        // Mocks
        VelocityEngine engine = mock(VelocityEngine.class);
        when(velocityManager.getVelocityEngine()).thenReturn(engine);
        when(engine.evaluate(any(VelocityContext.class), any(Writer.class), anyString(), eq("myCode"))).thenAnswer(
                new Answer<Object>()
                {
                    @Override
                    public Object answer(InvocationOnMock invocation) throws Throwable
                    {
                        // Get the writer
                        Writer writer = (Writer) invocation.getArguments()[1];
                        writer.write("Rendered code");
                        return true;
                    }
                });

        // Test
        assertEquals("Rendered code", mocker.getComponentUnderTest().render("myCode"));

        // Verify
        verify(engine).startedUsingMacroNamespace("IconVelocityRenderer_" + Thread.currentThread().getId());
        verify(engine).stoppedUsingMacroNamespace("IconVelocityRenderer_" + Thread.currentThread().getId());
    }

    @Test
    public void renderWithException() throws Exception
    {
        //  Mocks
        Exception exception = new XWikiVelocityException("exception");
        when(velocityManager.getVelocityEngine()).thenThrow(exception);

        // Test
        IconException caughtException = null;
        try {
            mocker.getComponentUnderTest().render("myCode");
        } catch(IconException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("Failed to render the icon.", caughtException.getMessage());
        assertEquals(exception, caughtException.getCause());
    }

    @Test
    public void renderWhenEvaluateReturnsFalse() throws Exception
    {
        //  Mocks
        VelocityEngine engine = mock(VelocityEngine.class);
        when(velocityManager.getVelocityEngine()).thenReturn(engine);
        when(engine.evaluate(any(VelocityContext.class), any(Writer.class), anyString(),
            eq("myCode"))).thenReturn(false);

        // Test
        IconException caughtException = null;
        try {
            mocker.getComponentUnderTest().render("myCode");
        } catch(IconException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("Failed to render the icon. See the Velocity runtime log.", caughtException.getMessage());

        verify(engine).startedUsingMacroNamespace("IconVelocityRenderer_" + Thread.currentThread().getId());
        verify(engine).stoppedUsingMacroNamespace("IconVelocityRenderer_" + Thread.currentThread().getId());
    }

}
