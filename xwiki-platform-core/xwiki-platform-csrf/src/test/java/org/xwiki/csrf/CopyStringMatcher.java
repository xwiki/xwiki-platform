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
package org.xwiki.csrf;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jmock.api.Action;
import org.jmock.api.Invocation;

/**
 * A matcher that acts as an action and returns the matched argument in the mocked method, allowing small modifications
 * (prepend/append something to the value).
 * 
 * @version $Id$
 * @since 2.5M2
 */
public final class CopyStringMatcher extends BaseMatcher<String> implements Action
{
    /** The string to copy. */
    private String value = null;

    /** String to prepend to the copied value. */
    private String prefix;

    /** String to append to the copied value. */
    private String suffix;

    /**
     * Create new CopyStringMatcher with the given prefix and suffix.
     * 
     * @param prefix string to prepend to the value
     * @param suffix string to append to the value
     */
    public CopyStringMatcher(String prefix, String suffix)
    {
        this.prefix = prefix;
        this.suffix = suffix;
    }

    /**
     * @see org.hamcrest.Matcher#matches(java.lang.Object)
     */
    @Override
    public boolean matches(Object argument)
    {
        if (argument instanceof String) {
            this.value = (String) argument;
            return true;
        }
        return false;
    }

    /**
     * @see org.hamcrest.SelfDescribing#describeTo(org.hamcrest.Description)
     */
    @Override
    public void describeTo(Description d)
    {
        d.appendText("COPY VALUE: ");
        d.appendValue(this.value);
    }

    /**
     * @see org.jmock.api.Invokable#invoke(org.jmock.api.Invocation)
     */
    @Override
    public String invoke(Invocation invocation) throws Throwable
    {
        if (this.value == null) {
            return this.prefix + this.suffix;
        }
        return this.prefix + this.value + this.suffix;
    }
}
