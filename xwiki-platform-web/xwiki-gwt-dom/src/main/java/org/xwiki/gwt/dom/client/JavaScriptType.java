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
package org.xwiki.gwt.dom.client;

/**
 * The type a JavaScript variable can have.
 * 
 * @version $Id$
 */
public enum JavaScriptType
{
    /**
     * The boolean variable is used to record a value of either {@code true} or {@code false}.
     */
    BOOLEAN,
    /**
     * The number variable holds any type of number, either an integer or a real number.
     */
    NUMBER,
    /**
     * The string variable type stores a string of characters, typically making up either a word or sentence. String
     * variables can also be assigned empty strings.
     */
    STRING,
    /**
     * Any built-in or user defined function. A function is one of the basic building blocks of most programming
     * languages including JavaScript. Functions provide a way to organize functionality into clean, reusable modules
     * that can be accessed from any other JavaScript to perform specific tasks. In addition to user created functions,
     * JavaScript contains a number of built-in functions that provide pre-defined functionality.
     */
    FUNCTION,
    /**
     * JavaScript provides a set of predefined objects to which the JavaScript programmer has access. For example the
     * document object refers to the current web page and can be accessed to make changes to the page content.
     */
    OBJECT,

    /**
     * A JavaScript variable whose value is not set.
     */
    UNDEFINED
}
