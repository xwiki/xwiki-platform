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

import org.junit.jupiter.api.Test;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @MockComponent
    private ScriptContextManager scriptContextManager;

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
