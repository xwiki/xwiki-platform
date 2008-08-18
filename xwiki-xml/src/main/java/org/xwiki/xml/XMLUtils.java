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
package org.xwiki.xml;

import org.w3c.dom.Document;
import org.jdom.input.DOMBuilder;
import org.jdom.output.XMLOutputter;
import org.jdom.output.Format;

/**
 * XML Utility methods.
 * 
 * @version $Id: $
 * @since 1.6M1
 */
public class XMLUtils
{
    /**
     * @param document the W3C Document to transform into a String
     * @return the XML as a String
     */
    public static String toString(Document document)
    {
        // Note: We don't use javax.xml.transform.Transformer since it prints our valid XHTML as HTML which is not
        // XHTML compliant. For example it transforms our "<hr/>" into "<hr>.
        DOMBuilder builder = new DOMBuilder();
        org.jdom.Document jdomDoc = builder.build(document);

        Format format = Format.getRawFormat();
        // Force newlines to use \n since otherwise the default is \n\r.
        // See http://www.jdom.org/docs/apidocs/org/jdom/output/Format.html#setLineSeparator(java.lang.String)
        format.setLineSeparator("\n");

        XMLOutputter outputter = new XMLOutputter(format);
        return outputter.outputString(jdomDoc);
    }
}
