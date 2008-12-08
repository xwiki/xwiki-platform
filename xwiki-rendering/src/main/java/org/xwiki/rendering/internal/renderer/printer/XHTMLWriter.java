package org.xwiki.rendering.internal.renderer.printer;

import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

/**
 * XHTMLWriter is an helper to configure XMLWriter to format a DOM4J tree as XHTML.
 * 
 * @version $Id$
 */
public class XHTMLWriter extends XMLWriter
{
    protected static final OutputFormat DEFAULT_XHTML_FORMAT;

    static {
        // Enable indentation based on two spaces characters
        // DEFAULT_XHTML_FORMAT = new OutputFormat(/*" ", true*/);

        DEFAULT_XHTML_FORMAT = new OutputFormat();

        DEFAULT_XHTML_FORMAT.setXHTML(true);
    }

    public XHTMLWriter(Writer writer) throws UnsupportedEncodingException
    {
        super(writer, DEFAULT_XHTML_FORMAT);

        // escape all non US-ASCII to have as less encoding problems as possible
        setMaximumAllowedCharacter(127);
    }
}
