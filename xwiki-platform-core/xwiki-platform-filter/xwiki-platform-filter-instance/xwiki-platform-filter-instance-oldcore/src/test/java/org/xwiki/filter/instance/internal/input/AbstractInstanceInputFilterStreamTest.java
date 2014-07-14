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
package org.xwiki.filter.instance.internal.input;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.filterxml.output.FilterXMLOutputProperties;
import org.xwiki.filter.input.BeanInputFilterStreamFactory;
import org.xwiki.filter.input.InputFilterStream;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.instance.input.InstanceInputProperties;
import org.xwiki.filter.instance.internal.AbstractInstanceFilterStreamTest;
import org.xwiki.filter.instance.internal.InstanceModel;
import org.xwiki.filter.instance.output.InstanceOutputProperties;
import org.xwiki.filter.internal.output.StringWriterOutputTarget;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.OutputFilterStream;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;

/**
 * Base class to validate an instance sub {@link InputInstanceFilterStream}.
 * 
 * @version $Id$
 */
@AllComponents
public class AbstractInstanceInputFilterStreamTest extends AbstractInstanceFilterStreamTest
{
    protected BeanOutputFilterStreamFactory<FilterXMLOutputProperties> xmlOutputFilterStreamFactory;

    protected BeanInputFilterStreamFactory<InstanceInputProperties> inputFilterStreamFactory;

    protected InstanceModel instanceModelMock;

    @Before
    public void before() throws Exception
    {
        super.before();

        this.xmlOutputFilterStreamFactory =
            this.oldcore.getMocker().getInstance(OutputFilterStreamFactory.class,
                FilterStreamType.FILTER_XML.serialize());
        this.inputFilterStreamFactory =
            this.oldcore.getMocker().getInstance(InputFilterStreamFactory.class,
                FilterStreamType.XWIKI_INSTANCE.serialize());

        // Query manager

        // Wikis
        when(this.instanceModelMock.getWikis()).thenAnswer(new Answer<List<String>>()
        {
            @Override
            public List<String> answer(InvocationOnMock invocation) throws Throwable
            {
                Set<String> wikis = new HashSet<String>();

                for (DocumentReference reference : oldcore.getDocuments().keySet()) {
                    wikis.add(reference.getWikiReference().getName());
                }

                List<String> list = new ArrayList<String>(wikis);
                Collections.sort(list);
                return list;
            }
        });

        // Spaces
        when(this.instanceModelMock.getSpaces(anyString())).thenAnswer(new Answer<List<String>>()
        {
            @Override
            public List<String> answer(InvocationOnMock invocation) throws Throwable
            {
                String wiki = (String) invocation.getArguments()[0];

                Set<String> spaces = new HashSet<String>();

                for (DocumentReference reference : oldcore.getDocuments().keySet()) {
                    if (reference.getWikiReference().getName().equals(wiki)) {
                        spaces.add(reference.getLastSpaceReference().getName());
                    }
                }

                List<String> list = new ArrayList<String>(spaces);
                Collections.sort(list);
                return list;
            }
        });

        // Documents
        when(this.instanceModelMock.getDocuments(anyString(), anyString())).thenAnswer(new Answer<List<String>>()
        {
            @Override
            public List<String> answer(InvocationOnMock invocation) throws Throwable
            {
                String wiki = (String) invocation.getArguments()[0];
                String space = (String) invocation.getArguments()[1];

                Set<String> docs = new HashSet<String>();

                for (DocumentReference reference : oldcore.getDocuments().keySet()) {
                    if (reference.getWikiReference().getName().equals(wiki)
                        && reference.getLastSpaceReference().getName().equals(space)) {
                        docs.add(reference.getName());
                    }
                }

                List<String> list = new ArrayList<String>(docs);
                Collections.sort(list);
                return list;
            }
        });
    }

    @AfterComponent
    public void afterComponent() throws Exception
    {
        this.instanceModelMock = this.oldcore.getMocker().registerMockComponent(InstanceModel.class);
    }

    protected void assertXML(String resource, InstanceInputProperties instanceProperties) throws FilterException,
        IOException
    {
        if (instanceProperties == null) {
            instanceProperties = new InstanceInputProperties();
            instanceProperties.setVerbose(false);
        }

        URL url = getClass().getResource("/" + resource + ".xml");

        String expected = IOUtils.toString(url, "UTF-8");

        expected = StringUtils.removeStart(expected, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");

        InputFilterStream inputFilterStream = this.inputFilterStreamFactory.createInputFilterStream(instanceProperties);

        StringWriterOutputTarget writer = new StringWriterOutputTarget();

        FilterXMLOutputProperties properties = new FilterXMLOutputProperties();
        properties.setTarget(writer);

        OutputFilterStream outputFilterStream = this.xmlOutputFilterStreamFactory.createOutputFilterStream(properties);

        inputFilterStream.read(outputFilterStream.getFilter());

        inputFilterStream.close();
        outputFilterStream.close();

        Assert.assertEquals(expected, writer.getBuffer().toString());
    }

    protected void assertXML(String resource, InstanceOutputProperties outputProperties,
        InstanceInputProperties inputProperties) throws FilterException, IOException
    {
        importFromXML(resource, outputProperties);
        assertXML(resource, inputProperties);
    }
}
