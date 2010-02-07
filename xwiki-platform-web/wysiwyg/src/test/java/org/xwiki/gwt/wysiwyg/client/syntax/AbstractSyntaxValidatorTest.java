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
package org.xwiki.gwt.wysiwyg.client.syntax;

import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.wysiwyg.client.WysiwygTestCase;
import org.xwiki.gwt.wysiwyg.client.syntax.internal.DisablingRule;


/**
 * Unit test for any concrete implementation of {@link SyntaxValidator}.
 * 
 * @version $Id$
 */
public abstract class AbstractSyntaxValidatorTest extends WysiwygTestCase
{
    /**
     * @return A new instance of the concrete implementation of {@link SyntaxValidator} being tested.
     */
    protected abstract SyntaxValidator newSyntaxValidator();

    /**
     * Tests if adding a {@link DisablingRule} for a feature disables that feature and removing it re-enables the
     * feature.
     */
    public void testAddRemoveDisablingRule()
    {
        String feature = "feature";
        RichTextArea textArea = new RichTextArea();
        SyntaxValidator sv = newSyntaxValidator();
        DisablingRule dr = new DisablingRule(new String[] {feature});

        assertTrue(sv.isValid(feature, textArea));

        sv.addValidationRule(dr);
        assertFalse(sv.isValid(feature, textArea));

        sv.removeValidationRule(dr);
        assertTrue(sv.isValid(feature, textArea));
    }
}
