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
import org.xwiki.gwt.wysiwyg.client.syntax.ValidationRule;


/**
 * Utility rule for disabling some of the editor's features.
 * 
 * @version $Id$
 */
public class DisablingRule implements ValidationRule
{
    /**
     * The features that are disabled.
     */
    private String[] features;

    /**
     * Creates a new validation rule that always disables the specified features.
     * 
     * @param features the features to be disabled
     */
    public DisablingRule(String[] features)
    {
        this.features = copy(features);
    }

    /**
     * Utility method for making a copy of the features array.
     * 
     * @param source the array to be copied
     * @return the copy
     */
    private String[] copy(String[] source)
    {
        // In the future, move this to an utility class or use a library method.
        String[] copy = new String[source.length];
        for (int i = 0; i < copy.length; i++) {
            copy[i] = source[i];
        }
        return copy;
    }

    @Override
    public boolean areValid(RichTextArea textArea)
    {
        return false;
    }

    @Override
    public String[] getFeatures()
    {
        return copy(features);
    }
}
