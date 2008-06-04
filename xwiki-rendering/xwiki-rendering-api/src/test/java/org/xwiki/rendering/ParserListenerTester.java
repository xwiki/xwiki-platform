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
package org.xwiki.rendering;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;

import org.xwiki.rendering.block.DOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.Parser;

import junit.framework.TestCase;

public class ParserListenerTester extends TestCase
{
    private Parser parser;
    private String testName;
    private Class<Listener> listenerClass;
    private String syntaxName;
    
    public ParserListenerTester(String testName, Parser parser, String syntaxName, Class<Listener> listenerClass)
    {
        super(testName + " " + parser.getClass().getName() + "/" + listenerClass.getName());
        this.testName = testName;
        this.parser = parser;
        this.listenerClass = listenerClass;
        this.syntaxName = syntaxName;
    }

    @Override
    protected void runTest() throws Throwable
    {
        InputStream input = getClass().getResourceAsStream("/" + this.testName + "-" + this.syntaxName + ".input");
        DOM dom = this.parser.parse(new InputStreamReader(input));
        
        StringWriter sw = new StringWriter();
        String actual;
        try {
            Listener listener = (Listener) this.listenerClass.getConstructor(new Class[] {Writer.class}).newInstance(new Object[]{sw});
            dom.traverse(listener);
            actual = sw.toString();
        } finally {
            sw.close();
        }
        
        String listenerShortName = listenerClass.getName().substring(listenerClass.getName().lastIndexOf(".") + 1);
        InputStream expectedStream = 
            getClass().getResourceAsStream("/" + this.testName + "-" + listenerShortName + ".expected");
        StringBuffer expected = new StringBuffer();
        try {
            char[] buffer = new char[1024]; // 10K should be enough
            BufferedReader br = new BufferedReader(new InputStreamReader(expectedStream));
            int r = 0;
            while ((r = br.read(buffer)) > 0) {
              expected.append(buffer, 0, r);
            }
        } finally {
            expectedStream.close();
        }

        assertEquals(expected.toString(), actual);
    }
}
