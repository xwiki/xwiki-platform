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
package org.xwiki.rendering.listener.chaining;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Stores information about the listeners in the chain and the order in which they need to be called. Also sports a
 * feature that allows pushing and popping listeners that are stackable. This feature is useful since listeners can hold
 * stateful information and sometimes you may need to push new versions of them to start with new state information. For
 * example this is used in the XWiki Syntax Renderer when an embedded document event is found to start the rendering for
 * that embedded document using reset state information.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
public class ListenerChain
{
    /**
     * The full list of chaining listeners. For each of them we have a stack since the ones that implement the
     * {@link StackableChainingListener} interface can be stacked.
     */
    private Map<Class< ? extends ChainingListener>, Stack<ChainingListener>> listeners =
        new HashMap<Class< ? extends ChainingListener>, Stack<ChainingListener>>();

    /**
     * The ordered list of listeners. We only allow one instance per listener class name so we just need to store the
     * class object and then the instance can be found in {@link #listeners}.
     */
    private List<Class< ? extends ChainingListener>> nextListeners =
        new ArrayList<Class< ? extends ChainingListener>>();

    /**
     * @param listener the chaining listener to add to the chain. If an instance of that listener is already present
     *            then we stack the new instance instead.
     */
    public void addListener(ChainingListener listener)
    {
        // If there's already an entry for that listener then push it on the existing stack
        // and don't add the listener as an additional listener in the list (since it's already
        // in there). We need to take these steps since the push() methods below will create
        // new instances of listeners which will add themselves in the chain automatically.
        Stack<ChainingListener> stack = this.listeners.get(listener.getClass());
        if (stack == null) {
            stack = new Stack<ChainingListener>();
            this.listeners.put(listener.getClass(), stack);
            this.nextListeners.add(listener.getClass());
        }
        stack.push(listener);
    }

    /**
     * @param listenerClass the listener to remove from the chain
     */
    public void removeListener(Class< ? extends ChainingListener> listenerClass)
    {
        this.listeners.remove(listenerClass);
        this.nextListeners.remove(listenerClass);
    }

    /**
     * @param listenerClass the listener for which we need to find the next listener in the chain
     * @return the next listener in the chain
     */
    public ChainingListener getNextListener(Class< ? extends ChainingListener> listenerClass)
    {
        ChainingListener next = null;
        int pos = this.nextListeners.indexOf(listenerClass);
        if (pos > -1 && this.nextListeners.size() > pos + 1) {
            next = this.listeners.get(this.nextListeners.get(pos + 1)).peek();
        }
        return next;
    }

    /**
     * @param listenerClass the listener class for which we want to find the listener instance
     * @return the listener instance corresponding to the passed class. Note that the last instance of the stack is
     *         returned
     */
    public ChainingListener getListener(Class< ? extends ChainingListener> listenerClass)
    {
        return this.listeners.get(listenerClass).peek();
    }

    public void pushListener(Class< ? extends ChainingListener> listenerClass)
    {
        if (StackableChainingListener.class.isAssignableFrom(listenerClass)) {
            Stack<ChainingListener> stack = this.listeners.get(listenerClass);
            ((StackableChainingListener) stack.peek()).createChainingListenerInstance();
        }
    }

    public void pushAllStackableListeners()
    {
        for (Class< ? extends ChainingListener> listenerClass : this.listeners.keySet()) {
            pushListener(listenerClass);
        }
    }

    public void popAllStackableListeners()
    {
        for (Class< ? extends ChainingListener> listenerClass : this.listeners.keySet()) {
            popListener(listenerClass);
        }
    }

    public void popListener(Class< ? extends ChainingListener> listenerClass)
    {
        if (StackableChainingListener.class.isAssignableFrom(listenerClass)) {
            this.listeners.get(listenerClass).pop();
        }
    }
}
