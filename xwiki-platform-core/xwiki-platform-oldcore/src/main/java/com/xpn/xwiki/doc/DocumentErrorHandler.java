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
package com.xpn.xwiki.doc;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class DocumentErrorHandler implements ErrorHandler
{
    /**
     * Receive notification of a recoverable error.
     * <p>
     * This corresponds to the definition of "error" in section 1.2 of the W3C XML 1.0 Recommendation. For example, a
     * validating parser would use this callback to report the violation of a validity constraint. The default behaviour
     * is to take no action.
     * <p>
     * The SAX parser must continue to provide normal parsing events after invoking this method: it should still be
     * possible for the application to process the document through to the end. If the application cannot do so, then
     * the parser should report a fatal error even if the XML 1.0 recommendation does not require it to do so.
     * <p>
     * Filters may use this method to report other, non-XML errors as well.
     *
     * @param exception The error information encapsulated in a SAX parse exception.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly wrapping another exception.
     * @see org.xml.sax.SAXParseException
     */
    @Override
    public void error(SAXParseException exception) throws SAXException
    {
        System.out.println("Error: ");
        exception.printStackTrace();
        // To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Receive notification of a non-recoverable error.
     * <p>
     * This corresponds to the definition of "fatal error" in section 1.2 of the W3C XML 1.0 Recommendation. For
     * example, a parser would use this callback to report the violation of a well-formedness constraint.
     * <p>
     * The application must assume that the document is unusable after the parser has invoked this method, and should
     * continue (if at all) only for the sake of collecting addition error messages: in fact, SAX parsers are free to
     * stop reporting any other events once this method has been invoked.
     *
     * @param exception The error information encapsulated in a SAX parse exception.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly wrapping another exception.
     * @see org.xml.sax.SAXParseException
     */
    @Override
    public void fatalError(SAXParseException exception) throws SAXException
    {
        System.out.println("Fatal Error: ");
        exception.printStackTrace();
        // To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Receive notification of a warning.
     * <p>
     * SAX parsers will use this method to report conditions that are not errors or fatal errors as defined by the XML
     * 1.0 recommendation. The default behaviour is to take no action.
     * <p>
     * The SAX parser must continue to provide normal parsing events after invoking this method: it should still be
     * possible for the application to process the document through to the end.
     * <p>
     * Filters may use this method to report other, non-XML warnings as well.
     *
     * @param exception The warning information encapsulated in a SAX parse exception.
     * @throws org.xml.sax.SAXException Any SAX exception, possibly wrapping another exception.
     * @see org.xml.sax.SAXParseException
     */
    @Override
    public void warning(SAXParseException exception) throws SAXException
    {
        System.out.println("Warning: ");
        exception.printStackTrace();
        // To change body of implemented methods use File | Settings | File Templates.
    }
}
