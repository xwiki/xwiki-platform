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
package org.xwiki.filter.instance.internal;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.Before;
import org.junit.Rule;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.filterxml.input.FilterXMLInputProperties;
import org.xwiki.filter.input.BeanInputFilterStreamFactory;
import org.xwiki.filter.input.DefaultURLInputSource;
import org.xwiki.filter.input.InputFilterStream;
import org.xwiki.filter.input.InputFilterStreamFactory;
import org.xwiki.filter.instance.output.InstanceOutputProperties;
import org.xwiki.filter.output.BeanOutputFilterStreamFactory;
import org.xwiki.filter.output.OutputFilterStream;
import org.xwiki.filter.output.OutputFilterStreamFactory;
import org.xwiki.filter.type.FilterStreamType;
import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.test.MockitoOldcoreRule;

import static com.xpn.xwiki.test.mockito.OldcoreMatchers.anyXWikiContext;
import static org.mockito.Mockito.doReturn;

/**
 * Base class to validate an instance sub {@link OutputInstanceFilterStream}.
 * 
 * @version $Id$
 */
@AllComponents
public class AbstractInstanceFilterStreamTest
{
    private static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z", Locale.ENGLISH);

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    protected BeanInputFilterStreamFactory<FilterXMLInputProperties> xmlInputFilterStreamFactory;

    protected BeanOutputFilterStreamFactory<InstanceOutputProperties> outputFilterStreamFactory;

    @Before
    public void before() throws Exception
    {
        this.xmlInputFilterStreamFactory = this.oldcore.getMocker().getInstance(InputFilterStreamFactory.class,
            FilterStreamType.FILTER_XML.serialize());
        this.outputFilterStreamFactory = this.oldcore.getMocker().getInstance(OutputFilterStreamFactory.class,
            FilterStreamType.XWIKI_INSTANCE.serialize());

        this.oldcore.getXWikiContext().setWikiId("wiki");

        // XWiki

        doReturn(true).when(this.oldcore.getSpyXWiki()).hasAttachmentRecycleBin(anyXWikiContext());
    }

    protected void importFromXML(String resource) throws FilterException
    {
        importFromXML(resource, null);
    }

    protected void importFromXML(String resource, InstanceOutputProperties instanceProperties) throws FilterException
    {
        if (instanceProperties == null) {
            instanceProperties = new InstanceOutputProperties();
            instanceProperties.setVerbose(false);
        }

        OutputFilterStream outputFilterStream =
            this.outputFilterStreamFactory.createOutputFilterStream(instanceProperties);

        URL url = getClass().getResource("/" + resource + ".xml");

        FilterXMLInputProperties properties = new FilterXMLInputProperties();
        properties.setSource(new DefaultURLInputSource(url));

        InputFilterStream inputFilterStream = this.xmlInputFilterStreamFactory.createInputFilterStream(properties);

        inputFilterStream.read(outputFilterStream.getFilter());

        try {
            inputFilterStream.close();
        } catch (IOException e) {
            throw new FilterException("Failed to close input wiki stream", e);
        }
        try {
            outputFilterStream.close();
        } catch (IOException e) {
            throw new FilterException("Failed to close output wiki stream", e);
        }
    }

    protected Date toDate(String date) throws ParseException
    {
        return DATE_PARSER.parse(date);
    }
}
