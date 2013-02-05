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
package org.xwiki.wikistream.internal.input.mediawiki.xml;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.test.jmock.AbstractComponentTestCase;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.mediawiki.xml.MediaWikiXmlParameters;
import org.xwiki.wikistream.internal.output.xml.OutputWikiStreamWikiXML;
import org.xwiki.wikistream.internal.output.xml.WikiXMLListener;
import org.xwiki.wikistream.output.OutputWikiStream;

/**
 * @version $Id: 5c213c4c836ba7a506c7fae073a3c2eee28e20be $
 */
public class InputWikiStreamMediaWikiXmlTest extends AbstractComponentTestCase
{

    private InputWikiStreamMediaWikiXml mediaWikiXmlInputStream = null;

    private MediaWikiXmlParameters parametersBean = null;

    private OutputWikiStreamWikiXML outputWikiStream = null;
    
    private WikiXMLListener listener=null;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        mediaWikiXmlInputStream =
            (InputWikiStreamMediaWikiXml) getComponentManager().getInstance(InputWikiStream.class, "mediawiki-xml");
        parametersBean = new MediaWikiXmlParameters();
        parametersBean.setDefaultSpace("MediaWiki");
        parametersBean.setSrcPath(this.getClass().getResource("/MediaWikiXML.xml").getPath());

        outputWikiStream = (OutputWikiStreamWikiXML) getComponentManager().getInstance(OutputWikiStream.class, "wiki-xml");
        listener=(WikiXMLListener) outputWikiStream.createListener(null);
    }

    @Test
    public void testBasicConfiguration()
    {
        Assert.assertNotNull(mediaWikiXmlInputStream);
        Assert.assertNotNull(outputWikiStream);
        Assert.assertEquals(mediaWikiXmlInputStream.getName(), "MediaWiki XML InputWikiStream");
        Assert.assertEquals(mediaWikiXmlInputStream.getDescription(),
            "Generates wiki events from MediaWiki XML inputstream.");
        Assert.assertNotNull(mediaWikiXmlInputStream.getDescriptor());
    }

    @Test
    public void testXmlParsing() throws WikiStreamException{
        mediaWikiXmlInputStream.parse(parametersBean, listener);
        System.out.println(listener.getXMLString());

    }
    
    @After
    public void tearDown() throws Exception
    {
        super.tearDown();
        mediaWikiXmlInputStream = null;
    }

}
