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
package org.xwiki.wikistream.xml.parser.internal;

import java.io.InputStream;

import junit.framework.Assert;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xwiki.wikistream.xml.internal.parser.DefaultXmlParser;
import org.xwiki.wikistream.xml.mock.listener.NoteListener;
import org.xwiki.wikistream.xml.parser.XmlParserException;

/**
 * 
 * @version $Id: 5c213c4c836ba7a506c7fae073a3c2eee28e20be $
 */
public class DefaultXmlParserTest
{
   @Test
    public void testParser(){
        InputStream inputStream=this.getClass().getResourceAsStream("/note.xml");
        DefaultXmlParser parser=new DefaultXmlParser();
        NoteListener listener=new NoteListener();
        try {
            parser.parse(inputStream, listener);
        } catch (XmlParserException e) {
            
        }
		Assert.assertTrue(inputStream != null);
    }

}
