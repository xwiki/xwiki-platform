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
package org.xwiki.wikistream.instance.internal;

import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManagerRule;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.BeanInputWikiStreamFactory;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.instance.internal.output.DocumentInstanceOutputEventReader;
import org.xwiki.wikistream.instance.internal.output.InstanceOutputProperties;
import org.xwiki.wikistream.internal.input.DefaultURLInputSource;
import org.xwiki.wikistream.output.BeanOutputWikiStreamFactory;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.output.OutputWikiStreamFactory;
import org.xwiki.wikistream.type.WikiStreamType;
import org.xwiki.wikistream.wikixml.internal.input.WikiXMLInputProperties;

import com.xpn.xwiki.test.MockitoOldcoreRule;

/**
 * Validate {@link DocumentInstanceOutputEventReader}.
 * 
 * @version $Id$
 */
@AllComponents
public class DocumentInstanceOutputEventReaderTest
{
    private MockitoComponentManagerRule mocker = new MockitoComponentManagerRule();

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule(this.mocker);

    private BeanInputWikiStreamFactory<WikiXMLInputProperties> inputWikiStreamFactory;

    private BeanOutputWikiStreamFactory<InstanceOutputProperties> outputWikiStreamFactory;

    @Before
    public void before() throws ComponentLookupException
    {
        this.inputWikiStreamFactory =
            this.mocker.getInstance(InputWikiStreamFactory.class, WikiStreamType.WIKI_XML.serialize());
        this.outputWikiStreamFactory =
            this.mocker.getInstance(OutputWikiStreamFactory.class, WikiStreamType.XWIKI_INSTANCE.serialize());
    }

    private void importFromXML(String resource) throws WikiStreamException
    {
        InstanceOutputProperties outputProperties = new InstanceOutputProperties();

        OutputWikiStream outputWikiStream = this.outputWikiStreamFactory.creaOutputWikiStream(outputProperties);

        URL url = getClass().getResource("/" + resource + ".xml");

        WikiXMLInputProperties properties = new WikiXMLInputProperties();
        properties.setSource(new DefaultURLInputSource(url));

        InputWikiStream inputWikiStream = this.inputWikiStreamFactory.createInputWikiStream(properties);

        inputWikiStream.read(outputWikiStream.getFilter());
    }

    // Tests

    @Test
    public void testDocument() throws WikiStreamException
    {
        importFromXML("document1");
    }
}
