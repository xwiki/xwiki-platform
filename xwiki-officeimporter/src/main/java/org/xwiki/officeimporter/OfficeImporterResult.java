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
package org.xwiki.officeimporter;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.rendering.parser.Syntax;

/**
 * Holds the result of an office import operation.
 * 
 * @version $Id$
 * @since 1.8M2
 */
public class OfficeImporterResult
{
    /**
     * Resulting content from the import operation.
     */
    private String content;

    /**
     * {@link Syntax} of the resulting content.
     */
    private Syntax syntax;

    /**
     * Collection of all artifacts extracted during import operation.
     */
    private Map<String, byte[]> artifacts = new HashMap<String, byte[]>();

    /**
     * Constructs an {@link OfficeImporterResult} object.
     * 
     * @param content content resulting from import operation.
     * @param syntax {@link Syntax} of the content.
     * @param artifacts artifacts collected during import operation.
     */
    public OfficeImporterResult(String content, Syntax syntax, Map<String, byte[]> artifacts)
    {
        this.content = content;
        this.syntax = syntax;
        this.artifacts = artifacts;
    }

    /**
     * @return the content.
     */
    public String getContent()
    {
        return content;
    }

    /**
     * @return syntax of the content.
     */
    public Syntax getSyntax()
    {
        return syntax;
    }

    /**
     * @return artifacts resulting from the import operation.
     */
    public Map<String, byte[]> getArtifacts()
    {
        return artifacts;
    }
}
