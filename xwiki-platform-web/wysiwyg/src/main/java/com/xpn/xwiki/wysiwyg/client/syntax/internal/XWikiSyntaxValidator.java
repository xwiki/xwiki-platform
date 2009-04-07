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
package com.xpn.xwiki.wysiwyg.client.syntax.internal;

import com.xpn.xwiki.wysiwyg.client.syntax.rule.DisableBlockElementsInTable;
import com.xpn.xwiki.wysiwyg.client.syntax.rule.DisableIndentOutsideList;
import com.xpn.xwiki.wysiwyg.client.syntax.rule.DisableListInHeader;

/**
 * Validator for the <em>xwiki/2.0</em> syntax.
 * 
 * @version $Id$
 */
public class XWikiSyntaxValidator extends DefaultSyntaxValidator
{
    /**
     * Default constructor.
     */
    public XWikiSyntaxValidator()
    {
        super("xwiki/2.0");

        // add XWiki specific validation rules
        addValidationRule(new DisableListInHeader());
        addValidationRule(new DisableIndentOutsideList());
        // FIXME : find a generic way of disabling inline/block/both elements on some identified elements
        addValidationRule(new DisableBlockElementsInTable());
    }
}
