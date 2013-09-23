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
package org.xwiki.rendering.macro.script;

import org.xwiki.properties.annotation.PropertyDescription;

/**
 * Parameters for the {@link AbstractScriptMacro} Macro.
 * 
 * @version $Id$
 * @since 1.7M3
 */
public class ScriptMacroParameters
{
    /**
     * @see #setOutput(boolean)
     */
    private boolean output = true;

    /**
     * @see #setWiki(boolean)
     */
    private boolean wiki = true;

    /**
     * @see #getJars()
     */
    private String jarURLsAsString;
    
    /**
     * @param output indicate the output result has to be inserted back in the document.
     */
    @PropertyDescription("Specifies whether or not the output result should be inserted back in the document.")
    public void setOutput(boolean output)
    {
        this.output = output;
    }

    /**
     * @return indicate the output result has to be inserted back in the document.
     */
    public boolean isOutput()
    {
        return this.output;
    }

    /**
     * @param wiki indicate if the result of the script execution has to be parsed by the current wiki parser. If not
     *            it's put in a verbatim block.
     * @since 2.0M1
     */
    @PropertyDescription("Specifies whether or not the script output contains wiki markup.")
    public void setWiki(boolean wiki)
    {
        this.wiki = wiki;
    }

    /**
     * @return indicate if the result of the script execution has to be parsed by the current wiki parser.
     * @since 2.0M1
     */
    public boolean isWiki()
    {
        return this.wiki;
    }
    
    /**
     * @param jarURLsAsString see {@link #getJars()}
     */
    @PropertyDescription("List of JARs to be added to the class loader used to execute this script. "
        + "Example: \"attach:wiki:space.page@somefile.jar\", \"attach:somefile.jar\", \"attach:wiki:space.page\" "
        + "(adds all JARs attached to the page) or URL to a JAR")
    public void setJars(String jarURLsAsString)
    {
        this.jarURLsAsString = jarURLsAsString;
    }
    
    /**
     * @return the list of JARs to be added to the script execution class loader, see 
     *         {@link org.xwiki.rendering.internal.macro.script.DefaultAttachmentClassLoaderFactory} for more details
     */
    public String getJars()
    {
        return this.jarURLsAsString;
    }
}
