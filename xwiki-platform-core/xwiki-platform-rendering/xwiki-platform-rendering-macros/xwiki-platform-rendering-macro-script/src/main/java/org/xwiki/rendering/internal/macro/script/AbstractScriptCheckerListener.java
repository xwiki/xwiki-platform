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

import java.util.Collections;
import java.util.List;

import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.macro.script.ScriptMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.script.event.ScriptEvaluatingEvent;

/**
 * Abstract base class for listeners that need to perform some checks just before a script macro is executed. Subclasses
 * must implement {@link #check(CancelableEvent, MacroTransformationContext, ScriptMacroParameters)} that allows
 * them to avoid casting and checking for the right event type.
 * 
 * @version $Id$
 * @since 2.5M1
 */
public abstract class AbstractScriptCheckerListener implements EventListener
{
    /**
     * {@inheritDoc}
     * <p>
     * This implementation returns a singleton list with a {@link org.xwiki.script.event.ScriptEvaluatingEvent}.
     * </p>
     * 
     * @see org.xwiki.observation.EventListener#getEvents()
     */
    @Override
    public List<Event> getEvents()
    {
        return Collections.singletonList((Event) new ScriptEvaluatingEvent());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation casts the arguments to the correct type and calls
     * {@link #check(CancelableEvent, MacroTransformationContext, ScriptMacroParameters)}.
     * </p>
     * 
     * @see org.xwiki.observation.EventListener#onEvent(Event, java.lang.Object, java.lang.Object)
     */
    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (!(event instanceof ScriptEvaluatingEvent)) {
            return;
        }
        if ((source == null || source instanceof MacroTransformationContext)
            && (data == null || data instanceof ScriptMacroParameters)) {
            check((CancelableEvent) event, (MacroTransformationContext) source, (ScriptMacroParameters) data);
        }
    }

    /**
     * This method is called when an {@link org.xwiki.script.event.ScriptEvaluatingEvent} is received.
     * 
     * @param event the received event
     * @param context current transformation context
     * @param parameters parameters of the script macro about to be executed
     */
    protected abstract void check(CancelableEvent event, MacroTransformationContext context,
        ScriptMacroParameters parameters);
}
