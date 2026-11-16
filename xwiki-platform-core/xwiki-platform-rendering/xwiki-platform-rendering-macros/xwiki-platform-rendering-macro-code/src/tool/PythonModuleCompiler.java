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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.python.core.imp;

/**
 * Compiles Python modules to the Java classes Jython would otherwise compile when importing them, so that Jython loads
 * them instead of compiling the sources, which is much slower.
 * <p>
 * Run at build time with the Java source launcher: {@code java -classpath <Jython> PythonModuleCompiler.java <output
 * directory> <source directory>...}. The compiled class of the module {@code a/b.py} is written to
 * {@code a/b$py.class} in the output directory, which Jython looks for next to the source of a module on the class
 * path. The files whose name starts with {@code -} in the arguments are excluded, relative to their source directory.
 *
 * @version $Id$
 */
public final class PythonModuleCompiler
{
    private static final String SOURCE_EXTENSION = ".py";

    private static final String PACKAGE_MODULE = ".__init__";

    private PythonModuleCompiler()
    {
    }

    /**
     * @param args the output directory, the source directories, and the excluded files
     * @throws IOException when failing to read a source or to write a class
     */
    public static void main(String[] args) throws IOException
    {
        Path output = Path.of(args[0]);
        List<String> excluded = Stream.of(args).filter(arg -> arg.startsWith("-")).map(arg -> arg.substring(1))
            .toList();
        List<Path> sources = Stream.of(args).skip(1).filter(arg -> !arg.startsWith("-")).map(Path::of).toList();

        int count = 0;
        for (Path source : sources) {
            List<Path> modules;
            try (Stream<Path> files = Files.walk(source)) {
                modules = files.filter(file -> file.toString().endsWith(SOURCE_EXTENSION))
                    .filter(file -> !excluded.contains(toPath(source.relativize(file)))).sorted().toList();
            }
            for (Path module : modules) {
                compile(source, module, output);
                count++;
            }
        }

        System.out.println("Compiled " + count + " Python modules to " + output);
    }

    private static void compile(Path source, Path module, Path output) throws IOException
    {
        String path = toPath(source.relativize(module));
        String pathWithoutExtension = path.substring(0, path.length() - SOURCE_EXTENSION.length());
        String name = pathWithoutExtension.replace('/', '.');
        if (name.endsWith(PACKAGE_MODULE)) {
            name = name.substring(0, name.length() - PACKAGE_MODULE.length());
        }

        byte[] bytes;
        try (InputStream input = Files.newInputStream(module)) {
            // Use the path Jython gives to the sources it finds on the class path, which appears in the stack traces.
            bytes = imp.compileSource(name, input, "__pyclasspath__/" + path);
        }

        Path target = output.resolve(pathWithoutExtension + "$py.class");
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
    }

    private static String toPath(Path path)
    {
        return path.toString().replace(path.getFileSystem().getSeparator(), "/");
    }
}
