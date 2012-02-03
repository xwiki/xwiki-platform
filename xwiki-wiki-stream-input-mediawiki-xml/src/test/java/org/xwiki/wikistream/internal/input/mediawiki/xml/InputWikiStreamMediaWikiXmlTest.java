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

import org.junit.Before;
import org.junit.Test;
import org.xwiki.wikistream.input.InputWikiStream;
import org.xwiki.wikistream.input.mediawiki.xml.MediaWikiXmlParameters;
import org.xwiki.wikistream.test.AbstractWikiStreamTest;


/**
 * 
 * @version $Id: 5c213c4c836ba7a506c7fae073a3c2eee28e20be $
 */
public class InputWikiStreamMediaWikiXmlTest extends AbstractWikiStreamTest
{

    private InputWikiStreamMediaWikiXml mediaWikiXmlInput=null;
    private MediaWikiXmlParameters parametersBean=null;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        mediaWikiXmlInput=(InputWikiStreamMediaWikiXml) getComponentManager().lookup(InputWikiStream.class,"mediawiki-xml");        
        parametersBean=new MediaWikiXmlParameters();
        parametersBean.setDefaultSpace("MediaWiki");
        parametersBean.setSrcPath(this.getClass().getResource("/MediaWikiXML.xml").getPath());
    }
    
    @Test
    public void testBasicConfiguration(){
        Assert.assertEquals(mediaWikiXmlInput.getName(), "MediaWiki XML InputWikiStream");
        Assert.assertEquals(mediaWikiXmlInput.getDescription(), "Generates wiki events from MediaWiki XML inputstream.");
        Assert.assertNotNull(mediaWikiXmlInput.getDescriptor());
        
    }

}
