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
package org.xwiki.wiki.internal.descriptor.builder;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.internal.descriptor.document.XWikiServerClassDocumentInitializer;
import org.xwiki.wiki.internal.descriptor.properties.WikiPropertyGroupManager;
import org.xwiki.wiki.properties.WikiPropertyGroupException;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.wiki.internal.descriptor.builder.DefaultWikiDescriptorBuilder}.
 *
 * @version $Id$
 * @since 6.0M1
 */
@OldcoreTest
@ReferenceComponentList
public class DefaultWikiDescriptorBuilderTest
{
    @RegisterExtension
    public LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    @Named("user")
    private DocumentReferenceResolver<String> userReferenceResolver;

    @MockComponent
    private WikiPropertyGroupManager wikiPropertyGroupManager;

    @InjectMockComponents
    private DefaultWikiDescriptorBuilder builder;

    @Test
    public void buildDescriptorObject() throws Exception
    {
        // Mocks
        List<BaseObject> objects = new ArrayList<>();
        BaseObject object1 = mock(BaseObject.class);
        BaseObject object2 = mock(BaseObject.class);
        BaseObject object3 = mock(BaseObject.class);
        // Make sure that the first object is null to also verify this case since it can happen that we get holes
        // with the XWikiDocument.getXObjects() API...
        objects.add(null);
        objects.add(object1);
        objects.add(object2);
        objects.add(null);
        objects.add(object3);

        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference documentReference = new DocumentReference("mainWiki", "XWiki", "XWikiServerSubwiki1");
        when(document.getDocumentReference()).thenReturn(documentReference);

        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_SERVER)).thenReturn("subwiki1");
        when(object2.getStringValue(XWikiServerClassDocumentInitializer.FIELD_SERVER)).thenReturn("alias1");
        when(object3.getStringValue(XWikiServerClassDocumentInitializer.FIELD_SERVER)).thenReturn("alias2");

        DocumentReference mainPageReference = new DocumentReference("subwiki1", "Space", "MainPage");

        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_HOMEPAGE)).thenReturn("Space.MainPage");
        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_WIKIPRETTYNAME))
            .thenReturn("myPrettyName");
        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_OWNER)).thenReturn("myOwner");
        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_DESCRIPTION)).thenReturn("myDescription");

        DocumentReference ownerRef = new DocumentReference("subwiki1", "XWiki", "myOwner");
        when(userReferenceResolver.resolve("myOwner", new WikiReference("subwiki1"))).thenReturn(ownerRef);

        // Test
        WikiDescriptor result = this.builder.buildDescriptorObject(objects, document);

        assertEquals("subwiki1", result.getId());
        assertEquals(3, result.getAliases().size());
        assertEquals("subwiki1", result.getAliases().get(0));
        assertEquals("alias1", result.getAliases().get(1));
        assertEquals("alias2", result.getAliases().get(2));
        assertEquals(mainPageReference, result.getMainPageReference());
        assertEquals("myPrettyName", result.getPrettyName());
        assertEquals("subwiki1:XWiki.myOwner", result.getOwnerId());
        assertEquals("myDescription", result.getDescription());

        // Verify
        wikiPropertyGroupManager.loadForDescriptor(any(WikiDescriptor.class));
    }

    @Test
    public void buildDescriptorObjectWhenInvalidWiki() throws Exception
    {
        // Mocks
        List<BaseObject> objects = new ArrayList<>();
        BaseObject object1 = mock(BaseObject.class);
        objects.add(object1);
        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_SERVER)).thenReturn(" ");

        XWikiDocument document = mock(XWikiDocument.class);

        // Test
        WikiDescriptor result = this.builder.buildDescriptorObject(objects, document);
        assertNull(result);
    }

    @Test
    public void buildDescriptorObjectWhenException() throws Exception
    {
        // Mocks
        List<BaseObject> objects = new ArrayList<>();
        BaseObject object1 = mock(BaseObject.class);
        objects.add(object1);

        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference documentReference = new DocumentReference("mainWiki", "XWiki", "XWikiServerSubwiki1");
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_SERVER)).thenReturn("subwiki1");
        when(object1.getStringValue(XWikiServerClassDocumentInitializer.FIELD_HOMEPAGE)).thenReturn("Space.MainPage");

        Exception exception = new WikiPropertyGroupException("error in wikiPropertyGroupManager.loadForDescriptor");
        doThrow(exception).when(wikiPropertyGroupManager).loadForDescriptor(any(WikiDescriptor.class));

        // Test
        this.builder.buildDescriptorObject(objects, document);

        // Verify
        ILoggingEvent log = this.logCapture.getLogEvent(0);
        assertEquals("Failed to load wiki property groups for wiki [{}].", log.getMessage());
        assertEquals("subwiki1", log.getArgumentArray()[0]);
        assertSame(exception.getMessage(), log.getThrowableProxy().getMessage());
    }
}
