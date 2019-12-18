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
package com.xpn.xwiki.render;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.logging.internal.DefaultLoggerConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.internal.DefaultScriptContextManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.internal.DefaultVelocityConfiguration;
import org.xwiki.velocity.internal.VelocityExecutionContextInitializer;

import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcoreRule;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Validate {@link DefaultVelocityManager}.
 * 
 * @version $Id$
 */
@ComponentList(value = { DefaultScriptContextManager.class, XWikiScriptContextInitializer.class,
DefaultVelocityConfiguration.class, DefaultLoggerConfiguration.class })
public class DefaultVelocityManagerTest
{
    public MockitoComponentMockingRule<VelocityManager> mocker =
        new MockitoComponentMockingRule<VelocityManager>(DefaultVelocityManager.class);

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule(this.mocker);

    @Before
    public void before() throws Exception
    {
        this.mocker.registerMockComponent(ContextualLocalizationManager.class);

        this.oldcore.getExecutionContext().setProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID,
            new VelocityContext());
    }

    // Tests

    @Test
    public void getVelocityContext() throws ComponentLookupException
    {
        VelocityContext context = this.mocker.getComponentUnderTest().getVelocityContext();

        assertNull(context.get("doc"));
        assertNull(context.get("sdoc"));

        DocumentReference docReference = new DocumentReference("wiki", "space", "doc");
        DocumentReference sdocReference = new DocumentReference("wiki", "space", "sdoc");

        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(docReference));
        this.oldcore.getXWikiContext().put("sdoc", new XWikiDocument(sdocReference));

        context = this.mocker.getComponentUnderTest().getVelocityContext();

        Document doc = (Document) context.get("doc");
        assertNotNull(doc);

        Document sdoc = (Document) context.get("sdoc");
        assertNotNull(sdoc);

        // Instances are kept the same when the documents in the context don't change
        context = this.mocker.getComponentUnderTest().getVelocityContext();
        assertSame(doc, context.get("doc"));
        assertSame(sdoc, context.get("sdoc"));

        // Instances change when the documents in the context change
        docReference = new DocumentReference("wiki", "space", "doc2");
        sdocReference = new DocumentReference("wiki", "space", "sdoc2");
        this.oldcore.getXWikiContext().setDoc(new XWikiDocument(docReference));
        this.oldcore.getXWikiContext().put("sdoc", new XWikiDocument(sdocReference));

        context = this.mocker.getComponentUnderTest().getVelocityContext();
        assertNotNull(context.get("doc"));
        assertNotSame(doc, context.get("doc"));
        assertNotNull(context.get("sdoc"));
        assertNotSame(sdoc, context.get("sdoc"));
    }
}
