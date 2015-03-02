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
package org.xwiki.filter.xar;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.test.ExtensionPackager;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.event.model.WikiDocumentFilter;
import org.xwiki.filter.filterxml.output.FilterXMLOutputProperties;
import org.xwiki.filter.input.BeanInputFilterStreamFactory;
import org.xwiki.filter.input.DefaultFileInputSource;
import org.xwiki.filter.input.InputFilterStream;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.OutputFilterStream;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.output.StringWriterOutputTarget;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.filter.xar.input.XARInputProperties;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.test.AllLogRule;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManagerRule;

@AllComponents
public class XARInputFilterStreamTest
{
    @Rule
    public MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    @Rule
    public AllLogRule allLogRule = new AllLogRule();

    private static final File FOLDER = new File("target/test-" + new Date().getTime()).getAbsoluteFile();

    private static ExtensionPackager extensionPackager;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        extensionPackager = new ExtensionPackager(null, FOLDER);
        extensionPackager.generateExtensions();
    }

    private void assertXML(String resource, XARInputProperties xarProperties) throws FilterException, IOException,
        ComponentLookupException
    {
        URL url = getClass().getResource("/xar/" + resource + ".output.xml");

        String expected = IOUtils.toString(url, "UTF-8");

        expected = StringUtils.removeStart(expected, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");

        BeanInputFilterStreamFactory<XARInputProperties> inputFilterStreamFactory =
            this.mocker.getInstance(InputFilterStreamFactory.class, FilterStreamType.XWIKI_XAR_11.serialize());
        InputFilterStream inputFilterStream = inputFilterStreamFactory.createInputFilterStream(xarProperties);

        StringWriterOutputTarget writer = new StringWriterOutputTarget();

        FilterXMLOutputProperties properties = new FilterXMLOutputProperties();
        properties.setTarget(writer);

        BeanOutputFilterStreamFactory<FilterXMLOutputProperties> xmlOutputFilterStreamFactory =
            this.mocker.getInstance(OutputFilterStreamFactory.class, FilterStreamType.FILTER_XML.serialize());
        OutputFilterStream outputFilterStream = xmlOutputFilterStreamFactory.createOutputFilterStream(properties);

        inputFilterStream.read(outputFilterStream.getFilter());

        inputFilterStream.close();
        outputFilterStream.close();

        Assert.assertEquals(expected, writer.getBuffer().toString());
    }

    @Test
    public void testSkipFirstDocument() throws FilterException, IOException, ComponentLookupException
    {
        XARInputProperties xarProperties = new XARInputProperties();

        xarProperties.setSource(new DefaultFileInputSource(extensionPackager.getExtensionFile(new ExtensionId("xar1",
            "1.0"))));
        EntityReferenceSet entities = new EntityReferenceSet();
        entities.includes(new LocalDocumentReference("space2", "page2"));
        xarProperties.setEntities(entities);

        assertXML("testSkipFirstDocument", xarProperties);

        assertTrue(this.allLogRule.getMarker(0).contains(WikiDocumentFilter.LOG_DOCUMENT_SKIPPED));
    }
}
