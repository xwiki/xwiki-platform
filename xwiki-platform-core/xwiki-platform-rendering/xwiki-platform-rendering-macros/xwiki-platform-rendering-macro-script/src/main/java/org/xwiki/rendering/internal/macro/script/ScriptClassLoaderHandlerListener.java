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
package org.xwiki.rendering.internal.macro.script;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.classloader.ExtendedURLClassLoader;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.script.event.ScriptEvaluatedEvent;
import org.xwiki.script.event.ScriptEvaluatingEvent;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.script.ScriptMacroParameters;

/**
 * Replaces the context class loader by a custom one that takes into account the "jars" Script parameter that allows
 * to add jars that will be visible to the executing script.
 *
 * Listens to script evaluation events ({@link org.xwiki.script.event.ScriptEvaluatingEvent} and
 * {@link org.xwiki.script.event.ScriptEvaluatedEvent}) to set the context class loader and to restore the original
 * one.
 * 
 * @version $Id$
 * @since 2.5M1
 */
@Component
@Named("scriptmacroclassloader")
@Singleton
public class ScriptClassLoaderHandlerListener implements EventListener
{
    /** Key used to store the original class loader in the Execution Context. */
    private static final String EXECUTION_CONTEXT_ORIG_CLASSLOADER_KEY = "originalClassLoader";

    /** Key used to store the class loader used by scripts in the Execution Context, see {@link #execution}. */
    private static final String EXECUTION_CONTEXT_CLASSLOADER_KEY = "scriptClassLoader";

    /** Key under which the jar params used for the last macro execution are cached in the Execution Context. */
    private static final String EXECUTION_CONTEXT_JARPARAMS_KEY = "scriptJarParams";

    /** Used to find if the current document's author has programming rights. */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to set the classLoader to be used by scripts across invocations. We save it in the Execution Context to be
     * sure it's the same classLoader used.
     */
    @Inject
    private Execution execution;

    /**
     * Used to create a custom class loader that knows how to support JARs attached to wiki page.
     */
    @Inject
    private AttachmentClassLoaderFactory attachmentClassLoaderFactory;

    @Override
    public String getName()
    {
        return "scriptmacroclassloader";
    }

    @Override
    public List<Event> getEvents()
    {
        List<Event> events = new LinkedList<Event>();
        events.add(new ScriptEvaluatingEvent());
        events.add(new ScriptEvaluatedEvent());
        return events;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (!(data instanceof ScriptMacroParameters)) {
            return;
        }
        if (event instanceof ScriptEvaluatingEvent) {
            // Set the context class loader to the script CL to ensure that any script engine using the context
            // classloader will work just fine.
            // Note: We must absolutely ensure that we always use the same context CL during the whole execution
            // request since JSR223 script engines (for example) that create internal class loaders need to
            // continue using these class loaders (where classes defined in scripts have been loaded for example).
            ScriptMacroParameters parameters = (ScriptMacroParameters) data;
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            this.execution.getContext().setProperty(EXECUTION_CONTEXT_ORIG_CLASSLOADER_KEY, originalClassLoader);
            try {
                ClassLoader newClassLoader = getClassLoader(parameters.getJars(), originalClassLoader);
                Thread.currentThread().setContextClassLoader(newClassLoader);
            } catch (Exception exception) {
                // abort execution
                ((CancelableEvent) event).cancel(exception.getMessage());
            }
        } else if (event instanceof ScriptEvaluatedEvent) {
            // Restore original class loader.
            ClassLoader originalClassLoader =
                (ClassLoader) this.execution.getContext().getProperty(EXECUTION_CONTEXT_ORIG_CLASSLOADER_KEY);
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    /**
     * @param jarsParameterValue the value of the macro parameters used to pass extra URLs that should be in the
     *            execution class loader
     * @param parent the parent classloader for the classloader to create (if it doesn't already exist)
     * @return the class loader to use for executing the script
     * @throws MacroExecutionException in case of an error in building the class loader
     */
    private ClassLoader getClassLoader(String jarsParameterValue, ClassLoader parent) throws MacroExecutionException
    {
        try {
            return findClassLoader(jarsParameterValue, parent);
        } catch (MacroExecutionException mee) {
            throw mee;
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to add JAR URLs to the current class loader for ["
                + jarsParameterValue + "]", e);
        }
    }

    /**
     * @param jarsParameterValue the value of the macro parameters used to pass extra URLs that should be in the
     *            execution class loader
     * @param parent the parent classloader for the classloader to create (if it doesn't already exist)
     * @return the class loader to use for executing the script
     * @throws Exception in case of an error in building the class loader
     */
    private ClassLoader findClassLoader(String jarsParameterValue, ClassLoader parent) throws Exception
    {
        // We cache the Class Loader for improved performances and we check if the saved class loader had the same
        // jar parameters value as the current execution. If not, we compute a new class loader.
        ExtendedURLClassLoader cl =
            (ExtendedURLClassLoader) this.execution.getContext().getProperty(EXECUTION_CONTEXT_CLASSLOADER_KEY);

        if (cl == null) {
            if (StringUtils.isNotEmpty(jarsParameterValue)) {
                cl = createOrExtendClassLoader(true, jarsParameterValue, parent);
            } else {
                cl = this.attachmentClassLoaderFactory.createAttachmentClassLoader("", parent);
            }
        } else {
            String cachedJarsParameterValue =
                (String) this.execution.getContext().getProperty(EXECUTION_CONTEXT_JARPARAMS_KEY);
            if (cachedJarsParameterValue != jarsParameterValue) {
                cl = createOrExtendClassLoader(false, jarsParameterValue, cl);
            }
        }
        this.execution.getContext().setProperty(EXECUTION_CONTEXT_CLASSLOADER_KEY, cl);

        return cl;
    }

    /**
     * @param createNewClassLoader if true create a new classloader and if false extend an existing one with the passed
     *            additional jars
     * @param jarsParameterValue the value of the macro parameters used to pass extra URLs that should be in the
     *            execution class loader
     * @param classLoader the parent classloader for the classloader to create or the classloader to extend, depending
     *            on the value of the createNewClassLoader parameter
     * @return the new classloader or the extended one
     * @throws Exception in case of an error in building or extending the class loader
     */
    private ExtendedURLClassLoader createOrExtendClassLoader(boolean createNewClassLoader, String jarsParameterValue,
        ClassLoader classLoader) throws Exception
    {
        ExtendedURLClassLoader cl;
        if (canHaveJarsParameters()) {
            if (createNewClassLoader) {
                cl = this.attachmentClassLoaderFactory.createAttachmentClassLoader(jarsParameterValue, classLoader);
            } else {
                cl = (ExtendedURLClassLoader) classLoader;
                this.attachmentClassLoaderFactory.extendAttachmentClassLoader(jarsParameterValue, cl);
            }
            this.execution.getContext().setProperty(EXECUTION_CONTEXT_JARPARAMS_KEY, jarsParameterValue);
        } else {
            throw new MacroExecutionException(
                "You cannot pass additional jars since you don't have programming rights");
        }
        return cl;
    }

    /**
     * Note that this method allows extending classes to override it to allow jars parameters to be used without
     * programming rights for example or to use some other conditions.
     * 
     * @return true if the user can use the macro parameter used to pass additional JARs to the class loader used to
     *         evaluate a script
     */
    private boolean canHaveJarsParameters()
    {
        return this.documentAccessBridge.hasProgrammingRights();
    }
}

