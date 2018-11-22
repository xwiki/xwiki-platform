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
package org.xwiki.rendering.display.html.script;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.xwiki.displayer.HTMLDisplayerManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.displayer.HTMLDisplayer}.
 *
 * @version $Id$
 */
@ComponentTest
public class HTMLDisplayerScriptServiceTest
{
    @MockComponent
    private HTMLDisplayerManager htmlDisplayerManager;

    @InjectMockComponents
    private HTMLDisplayerScriptService htmlDisplayerScriptService;

    @Test
    public void HTMLDisplayerScriptServiceTest() throws Exception
    {
        Answer answer = i -> {
            String attributes = "";
            if (i.getArguments().length > 2) {
                attributes = i.<Map<String, String>>getArgument(2).entrySet().stream()
                        .map(entry -> entry.getKey() + "='" + entry.getValue() + "'")
                        .collect(Collectors.joining(" "));
            }
            return "<input " + attributes + ">" + i.getArgument(1) + "</input>";
        };
        when(htmlDisplayerManager.display(any(), anyString())).thenAnswer(answer);
        when(htmlDisplayerManager.display(any(), anyString(), anyMap())).thenAnswer(answer);
        when(htmlDisplayerManager.display(any(), anyString(), anyMap(), anyString())).thenAnswer(answer);

        Map<String, String> parameters = new LinkedHashMap<String, String>(){{
            put("id", "testId");
            put("class", "testClass");
        }};

        assertEquals("<input >test</input>", this.htmlDisplayerScriptService.display("test"));
        assertEquals("<input id='testId' class='testClass'>test</input>",
                this.htmlDisplayerScriptService.display("test", parameters));
        assertEquals("<input id='testId' class='testClass'>test</input>",
                this.htmlDisplayerScriptService.display("test", parameters, "view"));
    }
}
