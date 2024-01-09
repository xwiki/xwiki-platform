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
package org.xwiki.livedata.script;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.livedata.internal.LiveDataRenderer;
import org.xwiki.livedata.internal.LiveDataRendererParameters;
import org.xwiki.livedata.internal.script.LiveDataConfigHelper;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

/**
 * Test of {@link LiveDataScriptService}.
 *
 * @version $Id$
 * @since 16.0.0RC1
 */
@ComponentTest
class LiveDataScriptServiceTest
{
    @InjectMockComponents
    private LiveDataScriptService scriptService;

    @MockComponent
    private LiveDataSourceManager sourceManager;

    @MockComponent
    private LiveDataConfigHelper configHelper;

    @MockComponent
    private ScriptServiceManager scriptServiceManager;

    @MockComponent
    private LiveDataRenderer liveDataRenderer;

    @Test
    void executeUnknownParam()
    {
        Map<String, Object> parameters = Map.of("a", "b");
        LiveDataException exception =
            assertThrows(LiveDataException.class, () -> this.scriptService.execute(parameters));
        assertThat(exception.getMessage(), matchesPattern(
            "Failed to set property \\[a] with value \\[b] in object " 
                + "\\[org.xwiki.livedata.internal.LiveDataRendererParameters@[^]]+]"));
    }

    @Test
    void execute() throws Exception
    {
        String liveDataId = "ld-id";
        Map<String, Object> parameters = Map.of("id", liveDataId);
        this.scriptService.execute(parameters);
        LiveDataRendererParameters rendererParameters = new LiveDataRendererParameters();
        rendererParameters.setId(liveDataId);
        verify(this.liveDataRenderer).execute(rendererParameters, (Map<?, ?>) null, false);
    }
}
