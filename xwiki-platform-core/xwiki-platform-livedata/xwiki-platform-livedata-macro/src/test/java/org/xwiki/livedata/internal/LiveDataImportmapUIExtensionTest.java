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
package org.xwiki.livedata.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.webjars.WebJarsUrlFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.xwiki.rendering.syntax.Syntax.PLAIN_1_0;

/**
 * Test of {@link LiveDataImportmapUIExtension}.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@ComponentTest
class LiveDataImportmapUIExtensionTest
{
    @InjectMockComponents
    private LiveDataImportmapUIExtension liveDataImportmapUIExtension;

    @MockComponent
    private WebJarsUrlFactory webJarsUrlFactory;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void execute()
    {
        when(this.webJarsUrlFactory.url(anyString(), anyString())).thenAnswer(
            invocationOnMock -> invocationOnMock.getArgument(0) + "/" + invocationOnMock.getArgument(1));
        var block = this.liveDataImportmapUIExtension.execute();

        assertEquals(new RawBlock(
            """
                {"imports":{"xwiki-livedata":"org.xwiki.platform:xwiki-platform-livedata-webjar/main.es.js","vue":"org.webjars.npm:vue/dist/vue.runtime.esm-browser.prod.js"}}""",
            PLAIN_1_0), block);
    }
}
