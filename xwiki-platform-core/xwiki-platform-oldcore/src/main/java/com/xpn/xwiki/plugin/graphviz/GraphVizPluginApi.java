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
package com.xpn.xwiki.plugin.graphviz;

import java.io.IOException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * Plugin which wraps the <a href="http://graphviz.org/">GraphViz</a> <tt>dot</tt> executable; transforming dot source
 * files (representing graphs) into images, image maps, or other output formats supported by GraphViz.
 * <p>
 * See http://www.graphviz.org/doc/info/lang.html for the dot language specification. See
 * http://www.graphviz.org/doc/info/output.html for the possible output formats
 * </p>
 *
 * @deprecated the plugin technology is deprecated
 * @version $Id$
 */
@Deprecated
public class GraphVizPluginApi extends PluginApi<GraphVizPlugin>
{
    /**
     * Default plugin API constructor.
     *
     * @param plugin the wrapped plugin instance
     * @param context the current request context
     */
    public GraphVizPluginApi(GraphVizPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * Return the inner plugin object, if the user has the required programming rights.
     *
     * @return The wrapped plugin object.
     */
    public GraphVizPlugin getPlugin()
    {
        return getInternalPlugin();
    }

    /**
     * Executes GraphViz and return the content of the resulting image (PNG format).
     *
     * @param content the dot source code
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @return the content of the generated image
     * @throws IOException if writing the input or output files to the disk fails
     */
    public byte[] getDotImage(String content, boolean dot) throws IOException
    {
        return getProtectedPlugin().getDotImage(content, dot);
    }

    /**
     * Executes GraphViz and return the content of the resulting image (PNG format).
     *
     * @param content the dot source code
     * @param extension the output file extension
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @return the content of the generated file
     * @throws IOException if writing the input or output files to the disk fails
     */
    public byte[] getDotImage(String content, String extension, boolean dot) throws IOException
    {
        return getProtectedPlugin().getDotImage(content, extension, dot);
    }

    /**
     * Executes GraphViz and returns the URL for the produced file, a PNG image.
     *
     * @param content the dot source
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @return the URL which can be used to access the generated image
     * @throws IOException if writing the input or output files to the disk fails
     */
    public String getDotImageURL(String content, boolean dot) throws IOException
    {
        return getProtectedPlugin().getDotImageURL(content, dot, getXWikiContext());
    }

    /**
     * Executes GraphViz, writes the resulting image (PNG format) in a temporary file on disk, and returns the filename.
     *
     * @param content the dot source code
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @return the name of the file where the generated output is stored
     * @throws IOException if writing the input or output files to the disk fails
     */
    public String writeDotImage(String content, boolean dot) throws IOException
    {
        return getProtectedPlugin().writeDotImage(content, dot);
    }

    /**
     * Executes GraphViz, writes the resulting image (in the requested format) in a temporary file on disk, and returns
     * the filename.
     *
     * @param content the dot source code
     * @param extension the output file extension
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @return the name of the file where the generated output is stored
     * @throws IOException if writing the input or output files to the disk fails
     */
    public String writeDotImage(String content, String extension, boolean dot) throws IOException
    {
        return getProtectedPlugin().writeDotImage(content, extension, dot);
    }

    /**
     * Executes GraphViz and writes the resulting image (PNG format) into the response.
     *
     * @param content the dot source code
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @throws IOException if writing the input or output files to the disk fails, or if writing the response body fails
     */
    public void outputDotImage(String content, boolean dot) throws IOException
    {
        getProtectedPlugin().outputDotImage(content, "png", dot, getXWikiContext());
    }

    /**
     * Executes GraphViz and writes the resulting image (in the requested format) into the response.
     *
     * @param content the dot source code
     * @param extension the output file extension
     * @param dot which engine to execute: {@code dot} if {@code true}, {@code neato} if {@code false}
     * @throws IOException if writing the input or output files to the disk fails, or if writing the response body fails
     */
    public void outputDotImage(String content, String extension, boolean dot) throws IOException
    {
        getProtectedPlugin().outputDotImage(content, extension, dot, getXWikiContext());
    }

    /**
     * Discard all generated output from the temporary file storage.
     */
    public void flushCache()
    {
        getProtectedPlugin().flushCache();
    }
}
