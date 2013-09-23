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
package com.xpn.xwiki.render.groovy;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import groovy.lang.Writable;
import groovy.text.Template;
import groovy.text.TemplateEngine;

import java.beans.Introspector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.xwiki.cache.DisposableCacheValue;

/**
 * This simple template engine uses JSP <% %> script and <%= %> expression syntax. It also lets you use normal groovy
 * expressions in the template text much like the new JSP EL functionality. The variable 'out' is bound to the writer
 * that the template is being written to.
 */
public class XWikiSimpleTemplateEngine extends TemplateEngine
{

    @Override
    public Template createTemplate(Reader reader) throws CompilationFailedException, IOException
    {
        XWikiSimpleTemplate template = new XWikiSimpleTemplate();
        GroovyShell shell = new GroovyShell();
        String script = template.parse(reader);
        template.script = shell.parse(script);

        return template;
    }

    private static class XWikiSimpleTemplate implements Template, DisposableCacheValue
    {
        private Script script;

        private Binding binding;

        @Override
        public void dispose() throws Exception
        {
            if (this.script != null) {
                InvokerHelper.removeClass(this.script.getClass());
            }
        }

        /**
         * Write the template document with the set binding applied to the writer.
         * 
         * @see groovy.lang.Writable#writeTo(java.io.Writer)
         */
        public Writer writeTo(Writer writer) throws IOException
        {
            if (this.binding == null) {
                this.binding = new Binding();
            }
            Script scriptObject = InvokerHelper.createScript(this.script.getClass(), this.binding);
            PrintWriter pw = new PrintWriter(writer);
            scriptObject.setProperty("out", pw);
            scriptObject.run();
            pw.flush();

            return writer;
        }

        /**
         * {@inheritDoc}
         * <p>
         * Convert the template and binding into a result String.
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            try {
                StringWriter sw = new StringWriter();
                writeTo(sw);

                return sw.toString();
            } catch (Exception e) {
                return e.toString();
            }
        }

        /**
         * Parse the text document looking for <% or <%= and then call out to the appropriate handler, otherwise copy
         * the text directly into the script while escaping quotes.
         */
        private String parse(Reader reader) throws IOException
        {
            if (!reader.markSupported()) {
                reader = new BufferedReader(reader);
            }
            StringWriter sw = new StringWriter();
            startScript(sw);

            for (int c; (c = reader.read()) != -1;) {
                if (c == '<') {
                    c = reader.read();
                    if (c != '%') {
                        sw.write('<');
                    } else {
                        reader.mark(1);
                        c = reader.read();
                        if (c == '=') {
                            groovyExpression(reader, sw);
                        } else {
                            reader.reset();
                            groovySection(reader, sw);
                        }
                        continue;
                    }
                }
                if (c == '\"') {
                    sw.write('\\');
                }
                if ((c != '\r') && (c != '\n')) {
                    sw.write(c);
                } else if (c == '\n') {
                    sw.write("\");out.println();out.print(\"");
                }
            }
            endScript(sw);
            String result = sw.toString();

            return result;
        }

        private void startScript(StringWriter sw)
        {
            sw.write("/* Generated by GroovyTemplateEngine */ ");
            sw.write("out.print(\"");
        }

        private void endScript(StringWriter sw)
        {
            sw.write("\");");
        }

        /**
         * Closes the currently open write and writes out the following text as a GString expression until it reaches an
         * end %>.
         */
        private void groovyExpression(Reader reader, StringWriter sw) throws IOException
        {
            sw.write("\");out.print(\"${");
            for (int c; (c = reader.read()) != -1;) {
                if (c == '%') {
                    c = reader.read();
                    if (c != '>') {
                        sw.write('%');
                    } else {
                        break;
                    }
                }
                sw.write(c);
            }
            sw.write("}\");out.print(\"");
        }

        /**
         * Closes the currently open write and writes the following text as normal Groovy script code until it reaches
         * an end %>.
         */
        private void groovySection(Reader reader, StringWriter sw) throws IOException
        {
            sw.write("\");");
            for (int c; (c = reader.read()) != -1;) {
                if (c == '%') {
                    c = reader.read();
                    if (c != '>') {
                        sw.write('%');
                    } else {
                        break;
                    }
                }
                sw.write(c);
            }
            sw.write(";out.print(\"");
        }

        @Override
        public Writable make()
        {
            return make(null);
        }

        @Override
        public Writable make(final Map map)
        {
            return new Writable()
            {
                /**
                 * {@inheritDoc}
                 * <p>
                 * Write the template document with the set binding applied to the writer.
                 * 
                 * @see groovy.lang.Writable#writeTo(java.io.Writer)
                 */
                @Override
                public Writer writeTo(Writer writer) throws IOException
                {
                    Binding binding;
                    if (map == null) {
                        binding = new Binding();
                    } else {
                        binding = new Binding(map);
                    }
                    Script scriptObject = InvokerHelper.createScript(script.getClass(), binding);
                    PrintWriter pw = new PrintWriter(writer);
                    scriptObject.setProperty("out", pw);
                    scriptObject.run();
                    pw.flush();

                    return writer;
                }

                /**
                 * {@inheritDoc}
                 * <p>
                 * Convert the template and binding into a result String.
                 * 
                 * @see java.lang.Object#toString()
                 */
                @Override
                public String toString()
                {
                    try {
                        StringWriter sw = new StringWriter();
                        writeTo(sw);

                        return sw.toString();
                    } catch (Exception e) {
                        return e.toString();
                    }
                }
            };
        }
    }

    public static void flushCache()
    {
        Introspector.flushCaches();
    }
}
