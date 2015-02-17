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
package org.xwiki.filter.test.xarinstance;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.test.ExtensionPackager;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.input.BeanInputFilterStreamFactory;
import org.xwiki.filter.input.DefaultFileInputSource;
import org.xwiki.filter.input.InputFilterStream;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.instance.internal.output.DocumentInstanceOutputFilterStream;
import org.xwiki.filter.instance.output.DocumentInstanceOutputProperties;
import org.xwiki.filter.instance.output.InstanceOutputProperties;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.OutputFilterStream;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.AllLogRule;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.mockito.OldcoreMatchers;

/**
 * Validate {@link DocumentInstanceOutputFilterStream}.
 * 
 * @version $Id$
 */
@AllComponents
public class XARToInstanceTest
{
    private static ExtensionPackager extensionPackager;

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    @Rule
    public AllLogRule logger = new AllLogRule();

    private BeanInputFilterStreamFactory<XARInputProperties> xarInputFilterStreamFactory;

    private BeanOutputFilterStreamFactory<InstanceOutputProperties> instanceOutputFilterStreamFactory;

    // Tests

    @BeforeClass
    public static void beforeCalss() throws IOException
    {
        // XARs

        File folder = new File("target/test-" + new Date().getTime()).getAbsoluteFile();
        extensionPackager = new ExtensionPackager(null, folder);
        extensionPackager.generateExtensions();
    }

    @Before
    public void before() throws Exception
    {
        this.xarInputFilterStreamFactory =
            this.oldcore.getMocker().getInstance(InputFilterStreamFactory.class,
                FilterStreamType.XWIKI_XAR_11.serialize());
        this.instanceOutputFilterStreamFactory =
            this.oldcore.getMocker().getInstance(OutputFilterStreamFactory.class,
                FilterStreamType.XWIKI_INSTANCE.serialize());

        this.oldcore.getXWikiContext().setWikiId("wiki");
    }

    private void importXAR(XARInputProperties inputProperties, DocumentInstanceOutputProperties outputProperties)
        throws FilterException, IOException
    {
        if (outputProperties == null) {
            outputProperties = new DocumentInstanceOutputProperties();
            outputProperties.setVerbose(false);
        }

        OutputFilterStream outputFilterStream =
            this.instanceOutputFilterStreamFactory.createOutputFilterStream(outputProperties);

        InputFilterStream inputFilterStream = this.xarInputFilterStreamFactory.createInputFilterStream(inputProperties);

        // Import

        inputFilterStream.read(outputFilterStream.getFilter());

        // Cleanup

        inputFilterStream.close();
        outputFilterStream.close();
    }

    @Test
    public void testImportWithFailingToSaveDocument() throws FilterException, XWikiException, IOException
    {
        DocumentReference pageReference =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "space", "page");
        DocumentReference page2Reference =
            new DocumentReference(this.oldcore.getXWikiContext().getWikiId(), "space", "page2");

        // Force save to fail
        doThrow(XWikiException.class).when(this.oldcore.getMockXWiki()).saveDocument(
            OldcoreMatchers.isDocument(pageReference), Matchers.<String>any(), eq(false), Matchers.<XWikiContext>any());

        DocumentInstanceOutputProperties outputProperties = new DocumentInstanceOutputProperties();
        outputProperties.setVersionPreserved(true);
        outputProperties.setVerbose(true);
        outputProperties.setStoppedWhenSaveFail(false);

        XARInputProperties inputProperties = new XARInputProperties();
        inputProperties.setSource(new DefaultFileInputSource(this.extensionPackager.getExtensionFile(new ExtensionId(
            "test1", "1.0"))));
        inputProperties.setVerbose(true);

        importXAR(inputProperties, outputProperties);

        // Page

        assertTrue("Unexpected log " + this.logger.getMessage(0),
            this.logger.getMarker(0).contains(WikiDocumentFilter.LOG_DOCUMENT_ERROR));

        assertTrue(this.oldcore.getMockXWiki().getDocument(pageReference, this.oldcore.getXWikiContext()).isNew());

        // Page2

        assertTrue("Unexpected log " + this.logger.getMessage(1),
            this.logger.getMarker(1).contains(WikiDocumentFilter.LOG_DOCUMENT_CREATED));

        XWikiDocument page2 = this.oldcore.getMockXWiki().getDocument(page2Reference, this.oldcore.getXWikiContext());

        assertFalse(page2.isNew());
    }
}
