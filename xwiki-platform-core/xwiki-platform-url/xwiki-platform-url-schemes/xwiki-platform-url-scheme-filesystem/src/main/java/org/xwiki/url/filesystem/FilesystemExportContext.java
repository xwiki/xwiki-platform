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
package org.xwiki.url.filesystem;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.xwiki.stability.Unstable;

/**
 * Stores states when generating Filesystem URLs. As we generate URLs for passed Resources we also export them to the
 * filesystem at the same time.
 *
 * @version $Id$
 * @since 7.2M1
 */
@Unstable
public class FilesystemExportContext
{
    /**
     * When there are relative links to resources inside CSS files they are resolved based on the location of the CSS
     * file itself. When we export we put all resources and attachments in the root of the exported directory and thus
     * in order to have valid relative links we need to make them match. We use this variable to do this.
     */
    private Stack<Integer> cssParentDepth = new Stack<>();

    /**
     * @see #getExportedPages()
     */
    private Set<String> exportedPages = new HashSet<>();

    /**
     * @see #getExportDir()
     */
    private File exportDir;

    /**
     * @see #getNeededSkins()
     */
    private Set<String> neededSkins = new HashSet<>();

    /**
     * @see #getExportedSkinFiles()
     */
    private Set<String> exportedSkinFiles = new HashSet<>();

    /**
     * @return the number of relative parent levels in the path to find the CSS file
     */
    public int getCSSParentLevel()
    {
        return this.cssParentDepth.isEmpty() ? 0 : this.cssParentDepth.peek();
    }

    /**
     * Pushes a new CSS parent's levels.
     *
     * @param depth the number of relative parent levels in the path to find the CSS file
     */
    public void pushCSSParentLevels(int depth)
    {
        this.cssParentDepth.push(depth);
    }

    /**
     * Pops the last CSS parent's levels.
     */
    public void popCSSParentLevels()
    {
        this.cssParentDepth.pop();
    }

    /**
     * @return the names of skins needed by rendered page(s)
     */
    public Set<String> getNeededSkins()
    {
        return this.neededSkins;
    }

    /**
     * @param skin see {@link #getNeededSkins()}
     */
    public void addNeededSkin(String skin)
    {
        this.neededSkins.add(skin);
    }

    /**
     * @return the base directory where the exported files are stored (attachments, resource files, etc)
     */
    public File getExportDir()
    {
        return this.exportDir;
    }

    /**
     * @return the pages for which to convert URLs to local
     */
    public Set<String> getExportedPages()
    {
        return this.exportedPages;
    }

    /**
     * @param page see {@link #getExportedPages()}
     */
    public void addExportedPage(String page)
    {
        this.exportedPages.add(page);
    }

    /**
     * @param page the page to check
     * @return true if the page URLs should be converted to local references
     */
    public boolean hasExportedPage(String page)
    {
        return this.exportedPages.contains(page);
    }

    /**
     * @param exportDir See {@link #getExportDir()}
     */
    public void setExportDir(File exportDir)
    {
        this.exportDir = exportDir;
    }

    /**
     * @return the list of custom skin files
     */
    public Collection<String> getExportedSkinFiles()
    {
        return this.exportedSkinFiles;
    }

    /**
     * @param filePath the skin file path to check
     * @return true if the skin file path is a custom skin file
     */
    public boolean hasExportedSkinFile(String filePath)
    {
        return this.exportedSkinFiles.contains(filePath);
    }

    /**
     * @param filePath see {@link #getExportedPages()}
     */
    public void addExportedSkinFile(String filePath)
    {
        this.exportedSkinFiles.add(filePath);
    }
}
