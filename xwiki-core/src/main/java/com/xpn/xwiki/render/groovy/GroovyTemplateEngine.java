/*
 * Copyright 2003 (C) Sam Pullara. All Rights Reserved.
 *
 * Redistribution and use of this software and associated documentation
 * ("Software"), with or without modification, are permitted provided that the
 * following conditions are met: 1. Redistributions of source code must retain
 * copyright statements and notices. Redistributions must also contain a copy
 * of this document. 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in
 * the documentation and/or other materials provided with the distribution. 3.
 * The name "groovy" must not be used to endorse or promote products derived
 * from this Software without prior written permission of The Codehaus. For
 * written permission, please contact info@codehaus.org. 4. Products derived
 * from this Software may not be called "groovy" nor may "groovy" appear in
 * their names without prior written permission of The Codehaus. "groovy" is a
 * registered trademark of The Codehaus. 5. Due credit should be given to The
 * Codehaus - http://groovy.codehaus.org/
 *
 * THIS SOFTWARE IS PROVIDED BY THE CODEHAUS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE CODEHAUS OR ITS CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package com.xpn.xwiki.render.groovy;

import com.xpn.xwiki.cache.impl.XWikiCachedObject;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.MetaClassRegistry;
import groovy.lang.Script;
import groovy.lang.Writable;
import groovy.text.Template;
import groovy.text.TemplateEngine;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.DefaultGroovyStaticMethods;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.beans.Introspector;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

/**
 * This simple template engine uses JSP <% %> script and <%= %> expression syntax.  It also lets you
 * use normal groovy expressions in the template text much like the new JSP EL functionality.  The
 * variable 'out' is bound to the writer that the template is being written to.
 */
public class GroovyTemplateEngine extends TemplateEngine
{
    /* (non-Javadoc)
    * @see groovy.util.TemplateEngine#createTemplate(java.io.Reader)
    */
    public Template createTemplate(Reader reader)
        throws CompilationFailedException, ClassNotFoundException, IOException
    {
        com.xpn.xwiki.render.groovy.GroovyTemplateEngine.SimpleTemplate template =
            new com.xpn.xwiki.render.groovy.GroovyTemplateEngine.SimpleTemplate();
        GroovyShell shell = new GroovyShell();
        String script = template.parse(reader);
        template.script = shell.parse(script);
        return template;
    }

    private static class SimpleTemplate implements Template, XWikiCachedObject
    {
        private Script script;

        private Binding binding;

        private Map map;

        public void finalize() throws Throwable
        {
            try {
                if (script != null) {
                    InvokerHelper.removeClass(script.getClass());
                    removeClass(script.getClass());
                }
            } finally {
                super.finalize();
            }
        }

        /**
         * Set the binding for the template.  Keys will be converted to Strings.
         *
         * @see groovy.text.Template#setBinding(java.util.Map)
         */
        public void setBinding(final Map map)
        {
            this.map = map;
            binding = new Binding(map);
        }

        /**
         * Write the template document with the set binding applied to the writer.
         *
         * @see groovy.lang.Writable#writeTo(java.io.Writer)
         */
        public Writer writeTo(Writer writer) throws IOException
        {
            if (binding == null) {
                binding = new Binding();
            }
            Script scriptObject = InvokerHelper.createScript(script.getClass(), binding);
            PrintWriter pw = new PrintWriter(writer);
            scriptObject.setProperty("out", pw);
            scriptObject.run();
            pw.flush();
            return writer;
        }

        /**
         * Convert the template and binding into a result String.
         *
         * @see java.lang.Object#toString()
         */
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
         * Parse the text document looking for <% or <%= and then call out to the appropriate handler,
         * otherwise copy the text directly into the script while escaping quotes.
         */
        private String parse(Reader reader) throws IOException
        {
            if (!reader.markSupported()) {
                reader = new BufferedReader(reader);
            }
            StringWriter sw = new StringWriter();
            startScript(sw);
            boolean start = false;
            int c;
            while ((c = reader.read()) != -1) {
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
            //System.out.println( "source text:\n" + result );
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
         * Closes the currently open write and writes out the following text as a GString expression
         * until it reaches an end %>.
         */
        private void groovyExpression(Reader reader, StringWriter sw) throws IOException
        {
            sw.write("\");out.print(\"${");
            int c;
            while ((c = reader.read()) != -1) {
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
         * Closes the currently open write and writes the following text as normal Groovy script code
         * until it reaches an end %>.
         */
        private void groovySection(Reader reader, StringWriter sw) throws IOException
        {
            sw.write("\");");
            int c;
            while ((c = reader.read()) != -1) {
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

        public Writable make()
        {
            return make(null);
        }

        public Writable make(final Map map)
        {
            return new Writable()
            {
                /**
                 * Write the template document with the set binding applied to the writer.
                 *
                 * @see groovy.lang.Writable#writeTo(java.io.Writer)
                 */
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
                 * Convert the template and binding into a result String.
                 *
                 * @see java.lang.Object#toString()
                 */
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

    protected static void clearMetaClassRegistry(MetaClassRegistry mcr)
    {
        Map map = (Map) com.xpn.xwiki.XWiki.getPrivateField(mcr, "metaClasses");
        map.clear();
        Map map2 = (Map) com.xpn.xwiki.XWiki.getPrivateField(mcr, "loaderMap");
        map2.clear();

        Class[] classes;
        Object[] objects;

        classes = new Class[1];
        classes[0] = DefaultGroovyMethods.class.getClass();
        objects = new Object[1];
        objects[0] = DefaultGroovyMethods.class;
        com.xpn.xwiki.XWiki.callPrivateMethod(mcr, "lookup", classes, objects);

        classes = new Class[2];
        classes[0] = DefaultGroovyMethods.class.getClass();
        classes[1] = boolean.class;
        objects = new Object[2];
        objects[0] = DefaultGroovyMethods.class;
        objects[1] = Boolean.TRUE;
        com.xpn.xwiki.XWiki.callPrivateMethod(mcr, "registerMethods", classes, objects);

        classes = new Class[1];
        classes[0] = DefaultGroovyStaticMethods.class.getClass();
        objects = new Object[1];
        objects[0] = DefaultGroovyStaticMethods.class;
        com.xpn.xwiki.XWiki.callPrivateMethod(mcr, "lookup", classes, objects);

        classes = new Class[2];
        classes[0] = DefaultGroovyStaticMethods.class.getClass();
        classes[1] = boolean.class;
        objects = new Object[2];
        objects[0] = DefaultGroovyStaticMethods.class;
        objects[1] = Boolean.FALSE;
        com.xpn.xwiki.XWiki.callPrivateMethod(mcr, "registerMethods", classes, objects);

        com.xpn.xwiki.XWiki.callPrivateMethod(mcr, "checkInitialised");
    }

    public static void flushCache()
    {
        // Clear up groovy registry
        MetaClassRegistry mcr = MetaClassRegistry.getIntance(0);
        clearMetaClassRegistry(mcr);
        mcr = MetaClassRegistry.getIntance(1);
        clearMetaClassRegistry(mcr);
        mcr = InvokerHelper.getInstance().getMetaRegistry();
        clearMetaClassRegistry(mcr);
        Introspector.flushCaches();
    }

    public static void removeClass(Class clazz)
    {
        // Clear up groovy registry
        MetaClassRegistry mcr = MetaClassRegistry.getIntance(0);
        mcr.removeMetaClass(clazz);
        mcr = MetaClassRegistry.getIntance(1);
        mcr.removeMetaClass(clazz);
        mcr = InvokerHelper.getInstance().getMetaRegistry();
        mcr.removeMetaClass(clazz);
    }
}
