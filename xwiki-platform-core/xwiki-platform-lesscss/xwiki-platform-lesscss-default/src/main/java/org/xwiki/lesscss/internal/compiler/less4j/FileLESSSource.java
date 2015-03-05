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
package org.xwiki.lesscss.internal.compiler.less4j;

import java.io.File;
import java.nio.file.Path;

import com.github.sommeri.less4j.LessSource;

/**
 * Class that wraps the LESS code that could be located either on a file or given as a string.
 * Used to emulate the --include-path options which is not natively implemented in Less4j.
 * See: https://github.com/SomMeri/less4j/wiki/Customizing-Compiler.
 *
 * @since 7.0RC1
 * @version $Id$
 */
public class FileLESSSource extends LessSource
{
    private Path[] includePaths;

    private File file;

    private String content;

    /**
     * @param content a string containing the LESS code to compile.
     * @param includePaths an array of paths where the include files can be located.
     */
    public FileLESSSource(String content, Path[] includePaths)
    {
        this.content = content;
        this.includePaths = includePaths;
    }

    /**
     * @param file the file containing the LESS code to compile.
     * @param includePaths an array of paths where the include files can be located.
     */
    public FileLESSSource(File file, Path[] includePaths)
    {
        this.file = file;
        this.includePaths = includePaths;
    }

    @Override
    public LessSource relativeSource(String filename) throws FileNotFound
    {
        // First look at files located in the same directory than the current one, if there is a current one
        if (file != null) {
            File currentDir = file.getParentFile();
            File newFile = new File(currentDir, filename);
            if (newFile.exists()) {
                return new FileLESSSource(newFile, this.includePaths);
            }
        }

        // then look inside the include paths
        for (int i = 0; i < includePaths.length; ++i) {
            Path directory = includePaths[i];
            File newFile = new File(directory.toFile(), filename);
            if (newFile.exists()) {
                return new FileLESSSource(newFile, this.includePaths);
            }
        }

        // The file has not been found
        throw new FileNotFound();
    }

    @Override
    public String getContent() throws FileNotFound, CannotReadFile
    {
        if (file != null) {
            FileSource fileSource = new FileSource(file);
            return fileSource.getContent();
        }

        return content;
    }

    @Override
    public byte[] getBytes() throws FileNotFound, CannotReadFile
    {
        if (file != null) {
            FileSource fileSource = new FileSource(file);
            return fileSource.getBytes();
        }

        return content.getBytes();
    }

    @Override
    public String getName()
    {
        if (file != null) {
            return file.getName();
        }

        return null;
    }
}
