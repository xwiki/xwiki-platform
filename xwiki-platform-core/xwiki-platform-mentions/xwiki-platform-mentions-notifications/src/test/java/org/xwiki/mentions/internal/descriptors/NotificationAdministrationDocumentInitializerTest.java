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
package org.xwiki.mentions.internal.descriptors;

import java.util.Arrays;
import java.util.Date;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link NotificationAdministrationDocumentInitializer}.
 *
 * @version $Id$
 * @since 14.8
 * @since 14.4.5
 * @since 13.10.10
 */
@ComponentTest
class NotificationAdministrationDocumentInitializerTest
{
    private static final String CONTEXT_MENTION_STARTDATE =
        "XWiki.Notifications.Code.NotificationAdministration.mention.startDate";

    private static final String START_DATE_PROPERTY = "startDate";

    @InjectMockComponents
    private NotificationAdministrationDocumentInitializer initializer;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private Execution execution;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Mock
    private XWikiDocument document;

    @Mock
    private ExecutionContext context;

    @Mock
    private BaseObject baseObject;

    @BeforeEach
    void setUp() throws Exception
    {
        XWikiContext xWikiContext = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(xWikiContext);
        when(this.document.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "Space", "Doc"));
        when(this.document.getParentReference()).thenReturn(new DocumentReference("xwiki", "Space", "WebHome"));
        when(this.execution.getContext()).thenReturn(this.context);
        LocalDocumentReference notificationPreferenceClass =
            new LocalDocumentReference(Arrays.asList("XWiki", "Notifications", "Code"), "NotificationPreferenceClass");
        when(this.document.createXObject(notificationPreferenceClass, xWikiContext)).thenReturn(12);
        when(this.document.getXObject(notificationPreferenceClass, 12)).thenReturn(this.baseObject);
    }

    @Test
    void updateDocumentContextNotInitialized()
    {
        when(this.context.hasProperty(CONTEXT_MENTION_STARTDATE)).thenReturn(false);
        Date date = new Date();
        when(this.context.getProperty(CONTEXT_MENTION_STARTDATE)).thenReturn(date);
        this.initializer.updateDocument(this.document);
        verify(this.context).setProperty(eq(CONTEXT_MENTION_STARTDATE), any(Date.class));
        verify(this.context).getProperty(CONTEXT_MENTION_STARTDATE);
        verify(this.baseObject).setDateValue("startDate", date);
    }

    @Test
    void updateDocumentContextInitialized()
    {
        when(this.context.hasProperty(CONTEXT_MENTION_STARTDATE)).thenReturn(true);
        Date date = new Date();
        when(this.context.getProperty(CONTEXT_MENTION_STARTDATE)).thenReturn(date);
        this.initializer.updateDocument(this.document);
        verify(this.context, never()).setProperty(any(), any());
        verify(this.context).getProperty(CONTEXT_MENTION_STARTDATE);
        verify(this.baseObject).setDateValue(START_DATE_PROPERTY, date);
    }
}
