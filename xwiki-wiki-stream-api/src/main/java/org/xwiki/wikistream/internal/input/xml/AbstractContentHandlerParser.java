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
package org.xwiki.wikistream.internal.input.xml;

import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xwiki.wikistream.input.ContentHandlerParser;
import org.xwiki.wikistream.listener.Listener;
import org.xwiki.wikistream.type.WikiStreamType;

/**
 * @version $Id$
 */
public class AbstractContentHandlerParser extends DefaultHandler implements ContentHandlerParser
{

    private Listener listener;

    protected Map<String, String> xmlTagParameters;

    protected int level = 0;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.input.ContentHandlerParser#getType()
     */
    @Override
    public WikiStreamType getType()
    {
        return WikiStreamType.MEDIAWIKI_XML;
    }

    
    /**
     * @return the listener
     */
    public Listener getListener()
    {
        return listener;
    }


    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.input.ContentHandlerParser#setListener(org.xwiki.wikistream.listener.Listener)
     */
    @Override
    public void setListener(Listener listener)
    {
        this.listener = listener;
    }

    
    /**
     * @return the xmlTagParameters
     */
    public Map<String, String> getXmlTagParameters()
    {
        return xmlTagParameters;
    }


    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.input.ContentHandlerParser#setXmlTagParameters(java.util.Map)
     */
    @Override
    public void setXmlTagParameters(Map<String, String> xmlTagParameters)
    {
        this.xmlTagParameters = xmlTagParameters;
    }


    
}
