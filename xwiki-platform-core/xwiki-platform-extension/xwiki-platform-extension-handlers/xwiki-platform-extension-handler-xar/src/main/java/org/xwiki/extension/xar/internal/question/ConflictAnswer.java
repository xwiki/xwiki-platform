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
package org.xwiki.extension.xar.internal.question;

import java.io.Serializable;

import org.xwiki.extension.xar.question.ConflictQuestion.GlobalAction;
import org.xwiki.stability.Unstable;

/**
 * The answer to a {@link org.xwiki.extension.xar.question.ConflictQuestion}.
 * 
 * @version $Id$
 * @since 7.1RC1
 */
@Unstable
public class ConflictAnswer implements Serializable
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @see org.xwiki.extension.xar.question.ConflictQuestion#getGlobalAction()
     */
    private final GlobalAction globalAction;

    /**
     * @see org.xwiki.extension.xar.question.ConflictQuestion#isAlways()
     */
    private final boolean always;

    /**
     * Creates a new answer.
     * 
     * @param globalAction same as {@link org.xwiki.extension.xar.question.ConflictQuestion#getGlobalAction()}
     * @param always same as {@link org.xwiki.extension.xar.question.ConflictQuestion#isAlways()}
     */
    public ConflictAnswer(GlobalAction globalAction, boolean always)
    {
        this.globalAction = globalAction;
        this.always = always;
    }

    /**
     * @return same as {@link org.xwiki.extension.xar.question.ConflictQuestion#getGlobalAction()}
     */
    public GlobalAction getGlobalAction()
    {
        return globalAction;
    }

    /**
     * @return same as {@link org.xwiki.extension.xar.question.ConflictQuestion#isAlways()}
     */
    public boolean isAlways()
    {
        return always;
    }
}
