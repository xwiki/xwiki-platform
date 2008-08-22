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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.wysiwyg.client.syntax.SyntaxValidator;
import com.xpn.xwiki.wysiwyg.client.syntax.ValidationRule;
import com.xpn.xwiki.wysiwyg.client.syntax.rule.BackColorRule;
import com.xpn.xwiki.wysiwyg.client.ui.XRichTextArea;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;

/**
 * Base class for syntax-specific validators. It includes by default syntax-independent rules.
 */
public abstract class DefaultSyntaxValidator implements SyntaxValidator
{
    private String syntax;

    private Map<String, List<ValidationRule>> rules = new HashMap<String, List<ValidationRule>>();

    public DefaultSyntaxValidator(String syntax)
    {
        this.syntax = syntax;

        // add default validation rules
        addValidationRule(new DefaultValidationRule("bold", Command.BOLD));
        addValidationRule(new DefaultValidationRule("italic", Command.ITALIC));
        addValidationRule(new DefaultValidationRule("underline", Command.UNDERLINE));
        addValidationRule(new DefaultValidationRule("strikethrough", Command.STRIKE_THROUGH));
        addValidationRule(new DefaultValidationRule("subscript", Command.SUB_SCRIPT));
        addValidationRule(new DefaultValidationRule("superscript", Command.SUPER_SCRIPT));
        addValidationRule(new DefaultValidationRule("justifyleft", Command.JUSTIFY_LEFT));
        addValidationRule(new DefaultValidationRule("justifycenter", Command.JUSTIFY_CENTER));
        addValidationRule(new DefaultValidationRule("justifyright", Command.JUSTIFY_RIGHT));
        addValidationRule(new DefaultValidationRule("justifyfull", Command.JUSTIFY_FULL));
        addValidationRule(new DefaultValidationRule("orderedlist", Command.INSERT_ORDERED_LIST));
        addValidationRule(new DefaultValidationRule("unorderedlist", Command.INSERT_UNORDERED_LIST));
        addValidationRule(new DefaultValidationRule("indent", Command.INDENT));
        addValidationRule(new DefaultValidationRule("outdent", Command.OUTDENT));
        addValidationRule(new DefaultValidationRule("undo", Command.UNDO));
        addValidationRule(new DefaultValidationRule("redo", Command.REDO));
        addValidationRule(new DefaultValidationRule("format", Command.FORMAT_BLOCK));
        addValidationRule(new DefaultValidationRule("fontname", Command.FONT_NAME));
        addValidationRule(new DefaultValidationRule("fontsize", Command.FONT_SIZE));
        addValidationRule(new DefaultValidationRule("forecolor", Command.FORE_COLOR));
        addValidationRule(new BackColorRule());
    }

    private List<ValidationRule> getValidationRules(String feature)
    {
        List<ValidationRule> featureRules = rules.get(feature);
        if (featureRules == null) {
            featureRules = new ArrayList<ValidationRule>();
            rules.put(feature, featureRules);
        }
        return featureRules;
    }

    private boolean hasValidationRules(String feature)
    {
        List<ValidationRule> featureRules = rules.get(feature);
        return featureRules != null && featureRules.size() > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SyntaxValidator#addValidationRule(ValidationRule)
     */
    public void addValidationRule(ValidationRule rule)
    {
        String[] features = rule.getFeatures();
        for (int i = 0; i < features.length; i++) {
            List<ValidationRule> featureRules = getValidationRules(features[i]);
            featureRules.add(rule);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see SyntaxValidator#getSyntax()
     */
    public String getSyntax()
    {
        return syntax;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SyntaxValidator#isValid(String, XRichTextArea)
     */
    public boolean isValid(String feature, XRichTextArea textArea)
    {
        if (hasValidationRules(feature)) {
            for (ValidationRule rule : getValidationRules(feature)) {
                if (!rule.areValid(textArea)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see SyntaxValidator#removeValidationRule(ValidationRule)
     */
    public void removeValidationRule(ValidationRule rule)
    {
        String[] features = rule.getFeatures();
        for (int i = 0; i < features.length; i++) {
            List<ValidationRule> featureRules = getValidationRules(features[i]);
            featureRules.remove(rule);
        }
    }
}
