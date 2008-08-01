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
package org.xwiki.rendering.macro;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class XHTMLMacro extends AbstractMacro implements Initializable
{
    private static final String DESCRIPTION = "Inserts XHTML code into the page.";

    private static final String PARAMETER_ESCAPE_WIKI_SYNTAX = "escapeWikiSyntax";
    
    /**
     * Injected by the Component Manager.
     */
    private Parser parser;

    /**
     * In order to speed up DTD loading/validation we use an entity resolver that can resolve DTDs locally.
     * Injected by the Component Manager.
     */
    private EntityResolver entityResolver;
    
    private Map<String, String> allowedParameters;

    /**
     * {@inheritDoc}
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
		// TODO: Use an I8N service to translate the descriptions in several languages
		this.allowedParameters = new HashMap<String, String>();
        this.allowedParameters.put("escapeWikiSyntax", "If true then the XHTML element contents won't be rendered "
            + "even if they contain valid wiki syntax.");
	}

	/**
     * {@inheritDoc}
     * @see Macro#getDescription()
     */
	public String getDescription()
	{
		// TODO: Use an I8N service to translate the description in several languages
		return DESCRIPTION;
	}

    /**
     * {@inheritDoc}
     * @see Macro#getAllowedParameters()
     */
	public Map<String, String> getAllowedParameters()
	{
		// We send a copy of the map and not our map since we don't want it to be modified.
		return new HashMap<String, String>(this.allowedParameters);
	}
	
    /**
     * {@inheritDoc}
     * @see Macro#execute(Map, String, org.xwiki.rendering.block.XDOM)
     */
    public List<Block> execute(Map<String, String> parameters, String content, XDOM dom)
        throws MacroExecutionException
    {
        // Parse the XHTML using an XML Parser and Wrap the XML elements in XMLBlock(s).
        // For each XML element's text, run it through the main Parser.

        // Check if the user has asked to escape wiki syntax or not
        boolean escapeWikiSyntax = false;
        if (parameters.containsKey(PARAMETER_ESCAPE_WIKI_SYNTAX)) {
            escapeWikiSyntax = Boolean.parseBoolean(parameters.get(PARAMETER_ESCAPE_WIKI_SYNTAX));
        }

        XMLBlockConverterHandler handler;
        try {
            XMLReader xr = XMLReaderFactory.createXMLReader();
            handler = new XMLBlockConverterHandler(this.parser, escapeWikiSyntax);
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            xr.setEntityResolver(this.entityResolver);

            // Since XML can only have a single root node and since we want to allow users to put
            // content such as the following, we need to wrap the content in a root node:
            //   <tag1>
            //     ..
            //   </tag1>
            //   <tag2>
            //   </tag2>
            String normalizedContent = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n"
                + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
                + "<root>" + content + "</root>";

            xr.parse(new InputSource(new StringReader(normalizedContent)));
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to parse content as XML ["
                + content + "]", e);
        }

        return handler.getRootBlock().getChildren();
    }

}
