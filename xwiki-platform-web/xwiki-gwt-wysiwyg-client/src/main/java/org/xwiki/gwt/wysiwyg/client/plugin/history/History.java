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
package org.xwiki.gwt.wysiwyg.client.plugin.history;

/**
 * Interface for undoing and redoing user's edit actions.
 * 
 * @version $Id$
 */
public interface History
{
    /**
     * @return false if the current version of the edited content is the latest one.
     */
    boolean canRedo();

    /**
     * @return false if the current version is the first one or if it is the oldest one stored in the history.
     */
    boolean canUndo();

    /**
     * Loads the next newer version of the edited content.
     */
    void redo();

    /**
     * Loads the next older version of the edited content.
     */
    void undo();
}
