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
package org.xwiki.gwt.user.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Node;

/**
 * Interface to the browser's console. Useful for logging and profiling. This class provides an empty implementation.
 * Each browser has its own console API. You should extend this class and overwrite the needed methods by using that
 * API.
 * 
 * @version $Id$
 */
public class Console
{
    /**
     * The console instance in use.
     */
    private static Console instance;

    /**
     * @return The console instance in use.
     */
    public static synchronized Console getInstance()
    {
        if (instance == null) {
            instance = GWT.create(Console.class);
        }
        return instance;
    }

    /**
     * Creates a new timer under the given name. Call {@link #timeEnd(String)} with the same name to stop the timer and
     * print the time elapsed.
     * 
     * @param name The name of the timer.
     */
    public void time(String name)
    {
    }

    /**
     * Stops a timer created by a call to {@link #time(String)} and writes the time elapsed.
     * 
     * @param name The name of the timer.
     */
    public void timeEnd(String name)
    {
    }

    /**
     * Turns on the JavaScript profiler.
     * 
     * @param title The text to be printed in the header of the profile report.
     */
    public void profile(String title)
    {
    }

    /**
     * Turns off the JavaScript profiler and prints its report.
     */
    public void profileEnd()
    {
    }

    /**
     * Writes the number of times that the line of code where count was called was executed.
     * 
     * @param title The message printed in addition to the number of the count.
     */
    public void count(String title)
    {
    }

    /**
     * Writes a message to the console. You may pass as many arguments as you'd like, and they will be joined together
     * in a space-delimited line.
     * <p>
     * The first argument to log may be a string containing printf-like string substitution patterns. For example:<br>
     * <code>console.log("The %s jumped over %d tall buildings", animal, count);</code><br>
     * The example above can be re-written without string substitution to achieve the same result:<br>
     * <code>console.log("The", animal, "jumped over", count, "tall buildings");</code><br>
     * These two techniques can be combined. If you use string substitution but provide more arguments than there are
     * substitution patterns, the remaining arguments will be appended in a space-delimited line, like so:<br>
     * <code>console.log("I am %s and I have:", myName, thing1, thing2, thing3);</code><br>
     * If objects are logged, they will be written not as static text, but as interactive hyperlinks that can be clicked
     * to inspect the object in Firebug's HTML, CSS, Script, or DOM tabs. You may also use the %o pattern to substitute
     * a hyperlink in a string.<br>
     * Here is the complete set of patterns that you may use for string substitution:<br>
     * <table summary="Substitution Patterns">
     * <thead>
     * <tr>
     * <td>String</td>
     * <td>Substitution Patterns</td>
     * </tr>
     * </thead> <tbody>
     * <tr>
     * <td>%s</td>
     * <td>String</td>
     * </tr>
     * <tr>
     * <td>%d, %i</td>
     * <td>Integer (numeric formatting is not yet supported)</td>
     * </tr>
     * <tr>
     * <td>%f</td>
     * <td>Floating point number (numeric formatting is not yet supported)</td>
     * </tr>
     * <tr>
     * <td>%o</td>
     * <td>Object hyperlink</td>
     * </tr>
     * </tbody>
     * </table>
     * 
     * @param object First parameter is required. It can be either a formatting string or any ordinary object.
     * @param objects Optional parameters.
     */
    public void log(Object object, Object... objects)
    {
    }

    /**
     * Writes a message to the console, including a hyperlink to the line where it was called.
     * 
     * @param object First parameter is required. It can be either a formatting string or any ordinary object.
     * @param objects Optional parameters.
     * @see #log(Object, Object...)
     */
    public void debug(Object object, Object... objects)
    {
    }

    /**
     * Writes a message to the console with the visual "info" icon and color coding and a hyperlink to the line where it
     * was called.
     * 
     * @param object First parameter is required. It can be either a formatting string or any ordinary object.
     * @param objects Optional parameters.
     * @see #log(Object, Object...)
     */
    public void info(Object object, Object... objects)
    {
    }

    /**
     * Writes a message to the console with the visual "warning" icon and color coding and a hyperlink to the line where
     * it was called.
     * 
     * @param object First parameter is required. It can be either a formatting string or any ordinary object.
     * @param objects Optional parameters.
     * @see #log(Object, Object...)
     */
    public void warn(Object object, Object... objects)
    {
    }

    /**
     * Writes a message to the console with the visual "error" icon and color coding and a hyperlink to the line where
     * it was called.
     * 
     * @param object First parameter is required. It can be either a formatting string or any ordinary object.
     * @param objects Optional parameters.
     * @see #log(Object, Object...)
     */
    public void error(Object object, Object... objects)
    {
    }

    /**
     * Tests that an expression is true. If not, it will write a message to the console and throw an exception.
     * 
     * @param expression The expression to be evaluated.
     * @param objects Optional parameters.
     * @see #log(Object, Object...)
     */
    public void assertTrue(boolean expression, Object... objects)
    {
    }

    /**
     * Prints an interactive listing of all properties of the given object.
     * 
     * @param object The object whose properties will be displayed.
     */
    public void dir(Object object)
    {
    }

    /**
     * Prints the XML source tree of an HTML or XML element. You can click on any node to inspect it.
     * 
     * @param node Any DOM node.
     */
    public void dirxml(Node node)
    {
    }

    /**
     * Prints an interactive stack trace of JavaScript execution at the point where it is called.
     * <p>
     * The stack trace details the functions on the stack, as well as the values that were passed as arguments to each
     * function. You can click each function to take you to its source, and click each argument value to inspect it.
     */
    public void trace()
    {
    }

    /**
     * Writes a message to the console and opens a nested block to indent all future messages sent to the console. Call
     * {@link #groupEnd()} to close the block.
     * 
     * @param object First parameter is required. It can be either a formatting string or any ordinary object.
     * @param objects Optional parameters.
     * @see #log(Object, Object...)
     */
    public void group(Object object, Object... objects)
    {
    }

    /**
     * Closes the most recently opened block created by a call to {@link #group(Object, Object...)}.
     */
    public void groupEnd()
    {
    }

    /**
     * Adds a break point for the debugger. If a debugger is present then the execution should stop at this point and
     * continue from here in debug mode.
     */
    public native void addBreakPoint()
    /*-{
        debugger;
    }-*/;
}
