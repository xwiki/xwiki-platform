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
package org.xwiki.gwt.wysiwyg.client.syntax.internal;

import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.syntax.ValidationRule;


/**
 * Validation rule for text area's commands. Verifies if a specific command can be executed on the current state of the
 * given text area.
 * 
 * @version $Id$
 */
public class DefaultValidationRule implements ValidationRule
{
    /**
     * The constrained feature.
     */
    private final String feature;

    /**
     * The command associated with {@link #feature}, used for determining the disabling state of the {@link #feature}.
     */
    private final Command command;

    /**
     * Creates a constraint for the given feature based on a command with the same name.
     * 
     * @param feature the feature to be constrained
     */
    public DefaultValidationRule(String feature)
    {
        this(feature, new Command(feature));
    }

    /**
     * Creates a constraint for the given feature based on its associated command.
     * 
     * @param feature the feature to be constrained
     * @param command the command used for determining the disabling state of the given feature
     */
    public DefaultValidationRule(String feature, Command command)
    {
        this.feature = feature;
        this.command = command;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ValidationRule#areValid(RichTextArea)
     */
    public boolean areValid(RichTextArea textArea)
    {
        return textArea.getCommandManager().isEnabled(command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ValidationRule#getFeatures()
     */
    public String[] getFeatures()
    {
        return new String[] {feature};
    }
}
