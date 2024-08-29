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

package org.xwiki.security.authorization.testwikis;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Public interface of the XML parser of test definition.
 *
 * @version $Id$
 * @since 5.0M2
 */
public interface TestDefinitionParser
{
    /**
     * Parse a test definition, creating a full representation in memory using test entities.
     * @param filename the name of the resource file to parse. Passed over to {@code ClassLoader.#getSystemResource();}.
     * @param resolver a reference resolver that will be used during parsing to convert strings to references.
     * @param serializer a reference serializer that will be used during parsing to convert references to strings.
     * @return a complete test definition.
     * @throws IOException on file access error.
     * @throws URISyntaxException on badly formed filename.
     * @throws ParserConfigurationException on badly setup XML parser.
     * @throws SAXException on badly formatted definition file.
     */
    TestDefinition parse(String filename,
                    EntityReferenceResolver<String> resolver,
                    EntityReferenceSerializer<String> serializer)
        throws IOException, URISyntaxException, ParserConfigurationException, SAXException;
}
