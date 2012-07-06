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
package org.xwiki.rendering.internal.macro.chart.source.table;

import org.xwiki.test.AbstractMockingComponentTestCase;

import java.util.Map;
import java.util.HashMap;

import org.jmock.Expectations;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.annotation.MockingRequirement;

import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.renderer.BlockRenderer;

/**
 * @version $Id$
 * @since 4.2M1
 */
public abstract class AbstractMacroContentTableBlockDataSourceTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement(exceptions={ComponentManager.class, BlockRenderer.class})
    private MacroContentTableBlockDataSource source;

    protected MacroContentTableBlockDataSource getDataSource()
    {
        return source;
    }

    @Override
    public void configure() throws Exception
    {
        // Mock components
        final DocumentAccessBridge dab = getComponentManager().getInstance(DocumentAccessBridge.class);
        final DocumentReference currentDocumentReference = new DocumentReference("wiki", "space", "page");
        final DocumentModelBridge dmb = getMockery().mock(DocumentModelBridge.class);
        getMockery().checking(new Expectations() {{
            allowing(dab).getCurrentDocumentReference();
                will(returnValue(currentDocumentReference));
            allowing(dab).getDocument(currentDocumentReference);
                will(returnValue(dmb));
            allowing(dmb).getSyntax();
                will(returnValue(Syntax.XWIKI_2_0));
        }});
    }

    protected Map<String, String> map(String... keyValues)
    {
        Map<String, String> map = new HashMap<String, String>();

        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }

        return map;
    }


}