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
package org.xwiki.rendering.macro.toc;

import java.util.Arrays;
import java.util.Collections;

import javax.inject.Provider;

import org.junit.runner.RunWith;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.xwiki20.XWiki20ImageReferenceParser;
import org.xwiki.rendering.internal.parser.xwiki20.XWiki20LinkReferenceParser;
import org.xwiki.rendering.internal.parser.xwiki20.XWiki20Parser;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.renderer.reference.link.LinkLabelGenerator;
import org.xwiki.rendering.test.integration.RenderingTestSuite;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 * @since 9.6RC1
 */
@RunWith(RenderingTestSuite.class)
@AllComponents
public class IntegrationTests
{
    @RenderingTestSuite.Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        LinkLabelGenerator linkLabelGenerator = componentManager.registerMockComponent(LinkLabelGenerator.class);

        Provider<XWikiContext> xwikiContextProvider = componentManager.registerMockComponent(
            new DefaultParameterizedType(null, Provider.class, XWikiContext.class));
        XWikiContext xwikiContext = mock(XWikiContext.class);
        when(xwikiContextProvider.get()).thenReturn(xwikiContext);
        XWiki xwiki = mock(XWiki.class);
        when(xwikiContext.getWiki()).thenReturn(xwiki);
        EntityReference reference = new EntityReference("Page", EntityType.DOCUMENT,
            new EntityReference("Space", EntityType.SPACE));
        XWikiDocument document = mock(XWikiDocument.class);
        when(xwiki.getDocument(reference, xwikiContext)).thenReturn(document);
        XDOM xdom = new XDOM(Arrays.asList(new SectionBlock(Arrays.asList(new HeaderBlock(Collections.emptyList(),
            HeaderLevel.LEVEL1)))));
        when(document.getXDOM()).thenReturn(xdom);

//        EntityReferenceSerializer<String> serializer = componentManager.registerMockComponent(
    }
}
