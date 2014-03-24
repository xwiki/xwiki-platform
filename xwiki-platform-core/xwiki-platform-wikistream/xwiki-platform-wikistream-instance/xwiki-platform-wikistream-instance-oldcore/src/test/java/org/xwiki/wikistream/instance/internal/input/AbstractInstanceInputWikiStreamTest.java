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
package org.xwiki.wikistream.instance.internal.input;

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
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.BeanInputWikiStreamFactory;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.instance.input.InstanceInputProperties;
import org.xwiki.wikistream.instance.internal.AbstractInstanceWikiStreamTest;
import org.xwiki.wikistream.instance.internal.InstanceModel;
import org.xwiki.wikistream.instance.output.InstanceOutputProperties;
import org.xwiki.wikistream.internal.output.StringWriterOutputTarget;
import org.xwiki.wikistream.output.BeanOutputWikiStreamFactory;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.output.OutputWikiStreamFactory;
import org.xwiki.wikistream.type.WikiStreamType;
import org.xwiki.wikistream.wikixml.output.WikiXMLOutputProperties;

/**
 * Base class to validate an instance sub {@link InputInstanceWikiStream}.
 * 
 * @version $Id$
 */
@AllComponents
public class AbstractInstanceInputWikiStreamTest extends AbstractInstanceWikiStreamTest
{
    protected BeanOutputWikiStreamFactory<WikiXMLOutputProperties> xmlOutputWikiStreamFactory;

    protected BeanInputWikiStreamFactory<InstanceInputProperties> inputWikiStreamFactory;

    protected InstanceModel instanceModelMock;

    @Before
    public void before() throws Exception
    {
        super.before();

        this.xmlOutputWikiStreamFactory =
            this.oldcore.getMocker().getInstance(OutputWikiStreamFactory.class, WikiStreamType.WIKI_XML.serialize());
        this.inputWikiStreamFactory =
            this.oldcore.getMocker().getInstance(InputWikiStreamFactory.class,
                WikiStreamType.XWIKI_INSTANCE.serialize());

        // Query manager

        // Wikis
        Mockito.when(this.instanceModelMock.getWikis()).thenAnswer(new Answer<List<String>>()
        {
            @Override
            public List<String> answer(InvocationOnMock invocation) throws Throwable
            {
                Set<String> wikis = new HashSet<String>();

                for (DocumentReference reference : documents.keySet()) {
                    wikis.add(reference.getWikiReference().getName());
                }

                List<String> list = new ArrayList<String>(wikis);
                Collections.sort(list);
                return list;
            }
        });

        // Spaces
        Mockito.when(this.instanceModelMock.getSpaces(Mockito.anyString())).thenAnswer(new Answer<List<String>>()
        {
            @Override
            public List<String> answer(InvocationOnMock invocation) throws Throwable
            {
                String wiki = (String) invocation.getArguments()[0];

                Set<String> spaces = new HashSet<String>();

                for (DocumentReference reference : documents.keySet()) {
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
        Mockito.when(this.instanceModelMock.getDocuments(Mockito.anyString(), Mockito.anyString())).thenAnswer(
            new Answer<List<String>>()
            {
                @Override
                public List<String> answer(InvocationOnMock invocation) throws Throwable
                {
                    String wiki = (String) invocation.getArguments()[0];
                    String space = (String) invocation.getArguments()[1];

                    Set<String> docs = new HashSet<String>();

                    for (DocumentReference reference : documents.keySet()) {
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

    protected void assertXML(String resource, InstanceInputProperties instanceProperties) throws WikiStreamException,
        IOException
    {
        if (instanceProperties == null) {
            instanceProperties = new InstanceInputProperties();
        }

        URL url = getClass().getResource("/" + resource + ".xml");

        String expected = IOUtils.toString(url, "UTF-8");

        expected = StringUtils.removeStart(expected, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n");

        InputWikiStream inputWikiStream = this.inputWikiStreamFactory.createInputWikiStream(instanceProperties);

        StringWriterOutputTarget writer = new StringWriterOutputTarget();

        WikiXMLOutputProperties properties = new WikiXMLOutputProperties();
        properties.setTarget(writer);

        OutputWikiStream outputWikiStream = this.xmlOutputWikiStreamFactory.createOutputWikiStream(properties);

        inputWikiStream.read(outputWikiStream.getFilter());

        inputWikiStream.close();
        outputWikiStream.close();

        Assert.assertEquals(expected, writer.getBuffer().toString());
    }

    protected void assertXML(String resource, InstanceOutputProperties outputProperties,
        InstanceInputProperties inputProperties) throws WikiStreamException, IOException
    {
        importFromXML(resource, outputProperties);
        assertXML(resource, inputProperties);
    }
}
