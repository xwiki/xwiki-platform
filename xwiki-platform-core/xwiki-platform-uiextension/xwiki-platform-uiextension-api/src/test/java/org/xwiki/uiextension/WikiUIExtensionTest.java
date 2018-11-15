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
package org.xwiki.uiextension;

import org.apache.commons.collections.MapUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentScope;
import org.xwiki.component.wiki.internal.bridge.ContentParser;
import org.xwiki.job.JobException;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.RenderingException;
import org.xwiki.rendering.async.AsyncContext;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererConfiguration;
import org.xwiki.rendering.async.internal.block.BlockAsyncRendererExecutor;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.util.ErrorBlockGenerator;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.uiextension.internal.WikiUIExtension;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link WikiUIExtension}.
 *
 * @version $Id$
 * @since 4.2M3
 */
@OldcoreTest
@ReferenceComponentList
public class WikiUIExtensionTest
{
    private static final DocumentReference CLASS_REF = new DocumentReference("xwiki", "XWiki", "UIExtensionClass");

    private static final DocumentReference DOC_REF = new DocumentReference("xwiki", "XWiki", "MyUIExtension");

    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "Admin");

    @MockComponent
    private JobProgressManager jobProgressManager;

    @MockComponent
    private ErrorBlockGenerator errorBlockGenerator;

    @MockComponent
    private AsyncContext asyncContext;

    @MockComponent
    private BlockAsyncRendererExecutor blockAsyncRendererExecutor;

    @MockComponent
    private RenderingContext renderingContext;

    @MockComponent
    private ContentParser contentParser;

    @InjectComponentManager
    private ComponentManager componentManager;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private BaseObject baseObject;

    @BeforeEach
    public void setUp() throws Exception
    {
        XWikiDocument ownerDocument = new XWikiDocument(DOC_REF);
        ownerDocument.setAuthorReference(AUTHOR_REFERENCE);

        this.baseObject = new BaseObject();
        this.baseObject.setOwnerDocument(ownerDocument);
        this.baseObject.setXClassReference(CLASS_REF);
    }

    @Test
    public void createWikiUIExtension()
        throws ComponentLookupException, WikiComponentException, JobException, RenderingException
    {
        when(this.contentParser.parse("", Syntax.XWIKI_2_1, DOC_REF)).thenReturn(XDOM.EMPTY);

        WikiUIExtension wikiUIX = new WikiUIExtension(this.baseObject, "roleHint", "id", "epId", this.componentManager);
        wikiUIX.setScope(WikiComponentScope.WIKI);

        assertEquals("roleHint", wikiUIX.getRoleHint());
        assertEquals("id", wikiUIX.getId());
        assertEquals("epId", wikiUIX.getExtensionPointId());
        assertEquals(AUTHOR_REFERENCE, wikiUIX.getAuthorReference());
        assertEquals(UIExtension.class, wikiUIX.getRoleType());
        assertEquals(WikiComponentScope.WIKI, wikiUIX.getScope());
        assertEquals(MapUtils.EMPTY_MAP, wikiUIX.getParameters());

        when(this.blockAsyncRendererExecutor.execute(any(BlockAsyncRendererConfiguration.class), any(), any(), any()))
            .thenReturn(new WordBlock(""));

        assertEquals(new WordBlock(""), wikiUIX.execute());
    }
}
