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
package com.xpn.xwiki.internal.filter.input;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.filterxml.output.FilterXMLOutputProperties;
import org.xwiki.filter.input.BeanInputFilterStreamFactory;
import org.xwiki.filter.input.InputFilterStream;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.instance.input.InstanceInputProperties;
import org.xwiki.filter.instance.internal.InstanceModel;
import org.xwiki.filter.instance.internal.input.InstanceInputFilterStream;
import org.xwiki.filter.instance.output.InstanceOutputProperties;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.OutputFilterStream;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.output.StringWriterOutputTarget;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceTree;
import org.xwiki.model.reference.EntityReferenceTreeNode;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.AfterComponent;

import com.xpn.xwiki.internal.filter.AbstractInstanceFilterStreamTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Base class to validate an instance sub {@link InstanceInputFilterStream}.
 * 
 * @version $Id$
 */
public abstract class AbstractInstanceInputFilterStreamTest extends AbstractInstanceFilterStreamTest
{
    protected BeanOutputFilterStreamFactory<FilterXMLOutputProperties> xmlOutputFilterStreamFactory;

    protected BeanInputFilterStreamFactory<InstanceInputProperties> inputFilterStreamFactory;

    protected InstanceModel instanceModelMock;

    @BeforeEach
    @Override
    public void beforeEach() throws Exception
    {
        super.beforeEach();

        this.xmlOutputFilterStreamFactory = this.oldcore.getMocker().getInstance(OutputFilterStreamFactory.class,
            FilterStreamType.FILTER_XML.serialize());
        this.inputFilterStreamFactory = this.oldcore.getMocker().getInstance(InputFilterStreamFactory.class,
            FilterStreamType.XWIKI_INSTANCE.serialize());

        // Query manager

        // Wikis
        when(this.instanceModelMock.getWikiReferences()).thenAnswer(new Answer<List<WikiReference>>()
        {
            @Override
            public List<WikiReference> answer(InvocationOnMock invocation) throws Throwable
            {
                Set<WikiReference> wikis = new HashSet<WikiReference>();

                for (DocumentReference reference : oldcore.getDocuments().keySet()) {
                    wikis.add(reference.getWikiReference());
                }

                List<WikiReference> list = new ArrayList<WikiReference>(wikis);
                Collections.sort(list);

                return list;
            }
        });

        // Spaces
        when(this.instanceModelMock.getSpaceReferences(any(WikiReference.class)))
            .thenAnswer(new Answer<EntityReferenceTreeNode>()
            {
                @Override
                public EntityReferenceTreeNode answer(InvocationOnMock invocation) throws Throwable
                {
                    WikiReference wiki = (WikiReference) invocation.getArguments()[0];

                    Set<SpaceReference> spaces = new HashSet<SpaceReference>();

                    for (DocumentReference reference : oldcore.getDocuments().keySet()) {
                        if (reference.getWikiReference().equals(wiki)) {
                            spaces.add(reference.getLastSpaceReference());
                        }
                    }

                    return new EntityReferenceTree(spaces).getChildren().iterator().next();
                }
            });

        // Documents
        when(this.instanceModelMock.getDocumentReferences(any(SpaceReference.class)))
            .thenAnswer(new Answer<List<DocumentReference>>()
            {
                @Override
                public List<DocumentReference> answer(InvocationOnMock invocation) throws Throwable
                {
                    SpaceReference space = (SpaceReference) invocation.getArguments()[0];

                    Set<DocumentReference> docs = new HashSet<DocumentReference>();

                    for (DocumentReference reference : oldcore.getDocuments().keySet()) {
                        if (reference.getLastSpaceReference().equals(space)) {
                            docs.add(reference);
                        }
                    }

                    List<DocumentReference> list = new ArrayList<DocumentReference>(docs);
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

    protected void assertXML(String resource, InstanceInputProperties instanceProperties)
        throws FilterException, IOException
    {
        if (instanceProperties == null) {
            instanceProperties = new InstanceInputProperties();
            instanceProperties.setVerbose(false);
        }

        URL url = getClass().getResource("/filter/" + resource + ".xml");

        String expected = IOUtils.toString(url, "UTF-8");

        expected = StringUtils.removeStart(expected, "<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n\n");

        String actual = toXML(instanceProperties);

        assertEquals(expected, actual);
    }

    protected String toXML(InstanceInputProperties instanceProperties) throws FilterException, IOException
    {
        InputFilterStream inputFilterStream = this.inputFilterStreamFactory.createInputFilterStream(instanceProperties);

        StringWriterOutputTarget writer = new StringWriterOutputTarget();

        FilterXMLOutputProperties properties = new FilterXMLOutputProperties();
        properties.setTarget(writer);

        OutputFilterStream outputFilterStream = this.xmlOutputFilterStreamFactory.createOutputFilterStream(properties);

        inputFilterStream.read(outputFilterStream.getFilter());

        inputFilterStream.close();
        outputFilterStream.close();

        return writer.getBuffer().toString();
    }

    protected void assertXML(String resource, InstanceOutputProperties outputProperties,
        InstanceInputProperties inputProperties) throws FilterException, IOException
    {
        importFromXML(resource, outputProperties);
        assertXML(resource, inputProperties);
    }
}
