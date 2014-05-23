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

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.mockito.Mockito;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.BeanInputWikiStreamFactory;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.InputWikiStreamFactory;
import org.xwiki.wikistream.instance.output.InstanceOutputProperties;
import org.xwiki.wikistream.internal.input.DefaultURLInputSource;
import org.xwiki.wikistream.output.BeanOutputWikiStreamFactory;
import org.xwiki.wikistream.output.OutputWikiStream;
import org.xwiki.wikistream.output.OutputWikiStreamFactory;
import org.xwiki.wikistream.type.WikiStreamType;
import org.xwiki.wikistream.wikixml.input.WikiXMLInputProperties;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcoreRule;

/**
 * Base class to validate an instance sub {@link OutputInstanceWikiStream}.
 * 
 * @version $Id$
 */
@AllComponents
public class AbstractInstanceWikiStreamTest
{
    private static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z", Locale.ENGLISH);

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    protected BeanInputWikiStreamFactory<WikiXMLInputProperties> xmlInputWikiStreamFactory;

    protected BeanOutputWikiStreamFactory<InstanceOutputProperties> outputWikiStreamFactory;

    @Before
    public void before() throws Exception
    {
        this.xmlInputWikiStreamFactory =
            this.oldcore.getMocker().getInstance(InputWikiStreamFactory.class, WikiStreamType.WIKI_XML.serialize());
        this.outputWikiStreamFactory =
            this.oldcore.getMocker().getInstance(OutputWikiStreamFactory.class,
                WikiStreamType.XWIKI_INSTANCE.serialize());

        this.oldcore.getXWikiContext().setWikiId("wiki");

        // XWiki

        Mockito.when(this.oldcore.getMockXWiki().hasAttachmentRecycleBin(Mockito.any(XWikiContext.class))).thenReturn(
            true);
    }

    protected void importFromXML(String resource) throws WikiStreamException
    {
        importFromXML(resource, null);
    }

    protected void importFromXML(String resource, InstanceOutputProperties instanceProperties)
        throws WikiStreamException
    {
        if (instanceProperties == null) {
            instanceProperties = new InstanceOutputProperties();
            instanceProperties.setVerbose(false);
        }

        OutputWikiStream outputWikiStream = this.outputWikiStreamFactory.createOutputWikiStream(instanceProperties);

        URL url = getClass().getResource("/" + resource + ".xml");

        WikiXMLInputProperties properties = new WikiXMLInputProperties();
        properties.setSource(new DefaultURLInputSource(url));

        InputWikiStream inputWikiStream = this.xmlInputWikiStreamFactory.createInputWikiStream(properties);

        inputWikiStream.read(outputWikiStream.getFilter());

        try {
            inputWikiStream.close();
        } catch (IOException e) {
            throw new WikiStreamException("Failed to close input wiki stream", e);
        }
        try {
            outputWikiStream.close();
        } catch (IOException e) {
            throw new WikiStreamException("Failed to close output wiki stream", e);
        }
    }

    protected Date toDate(String date) throws ParseException
    {
        return DATE_PARSER.parse(date);
    }
}
