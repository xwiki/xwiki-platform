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
package org.xwiki.index.internal;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.index.IndexException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultLinksTaskConsumer}.
 *
 * @version $Id$
 * @since 14.2RC1
 */
@ComponentTest
class DefaultLinksTaskConsumerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    private static final String VERSION = "2.5";

    @InjectMockComponents
    private DefaultLinksTaskConsumer defaultLinksTaskConsumer;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument document;

    @Mock
    private XWikiHibernateStore hibernateStore;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.wiki.hasBacklinks(this.context)).thenReturn(true);
        when(this.documentRevisionProvider.getRevision(DOCUMENT_REFERENCE, VERSION)).thenReturn(this.document);
        when(this.wiki.getHibernateStore()).thenReturn(this.hibernateStore);
        when(this.hibernateStore.executeWrite(same(this.context), any())).thenAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                invocation.<HibernateCallback<Void>>getArgument(1).doInHibernate(null);

                return null;
            }
        });
    }

    @Test
    void consumeBacklinksDeactivated() throws Exception
    {
        when(this.wiki.hasBacklinks(this.context)).thenReturn(false);
        this.defaultLinksTaskConsumer.consume(DOCUMENT_REFERENCE, VERSION);
        verifyNoInteractions(this.documentRevisionProvider);
        verify(this.wiki, never()).getHibernateStore();
        assertEquals(1, this.logCapture.size());
        assertEquals("Skipping for document [wiki:space.page] version [2.5] "
            + "because backlinks are not supported in this wiki.", this.logCapture.getMessage(0));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void consume() throws Exception
    {
        this.defaultLinksTaskConsumer.consume(DOCUMENT_REFERENCE, VERSION);
        verify(this.hibernateStore).saveLinks(this.document, this.context, true);
    }

    @Test
    void consumeSaveLinksFailed() throws Exception
    {
        doThrow(XWikiException.class).when(this.hibernateStore).saveLinks(this.document, this.context, true);
        IndexException e = assertThrows(IndexException.class,
            () -> this.defaultLinksTaskConsumer.consume(DOCUMENT_REFERENCE, VERSION));
        assertEquals("Failed to updated links for document [wiki:space.page] version [2.5].", e.getMessage());
    }
}
