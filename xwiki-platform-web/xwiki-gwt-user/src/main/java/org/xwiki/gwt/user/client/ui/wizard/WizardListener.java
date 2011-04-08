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
package org.xwiki.gwt.user.client.ui.wizard;

/**
 * Interface to specify wizard event, such as the wizard completion or cancel, to let obsevers know when a wizard is
 * done.
 * 
 * @version $Id$
 */
public interface WizardListener
{
    /**
     * Fired when the wizard is finished, passing the result of the wizard in its parameter.
     * 
     * @param sender the source of this wizard event
     * @param result the result of the wizard.
     */
    void onFinish(Wizard sender, Object result);

    /**
     * Fired when the wizard is canceled.
     * 
     * @param sender the source of this wizard event
     */
    void onCancel(Wizard sender);
}
