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
package com.xpn.xwiki.notify;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Manages general and named notifications rules.
 * 
 * @version $Id$
 */
@Deprecated
public class XWikiNotificationManager
{
    private Vector<XWikiNotificationRule> generalrules = new Vector<XWikiNotificationRule>();

    private Map<String, Vector<XWikiNotificationRule>> namedrules =
        new HashMap<String, Vector<XWikiNotificationRule>>();

    public XWikiNotificationManager()
    {
    }

    /**
     * Add a "general" notification rule to be evaluated for any document (as opposed to a named rule)
     * 
     * @param rule the rule to be added
     */
    public void addGeneralRule(XWikiNotificationRule rule)
    {
        synchronized (generalrules) {
            generalrules.add(rule);
        }
    }

    /**
     * Remove the given general rule
     */
    public void removeGeneralRule(XWikiNotificationRule rule)
    {
        // TODO: This should be improved since it's impossible to remove a rule if you loose its instance
        // (since it's an equals on the pointer in memory that is done).
        // The solution is to implement equals/hashCode in XWikiNotificationRule implementations and introduce
        // a name for general rules.
        // That said, this is being phased with the new Observation manager...
        synchronized (generalrules) {
            generalrules.remove(rule);
        }
    }

    /**
     * Add a "named" notification rule, to be evaluated only when the notification concerns the document with the given
     * name
     * 
     * @param name the document's name for which the notification rule should apply. For example, for the "Main.WebHome"
     *            document, the rule name should either be "Main.WebHome", or "database:Main.WebHome"
     * @param rule the rule to add for this name
     */
    public void addNamedRule(String name, XWikiNotificationRule rule)
    {
        synchronized (namedrules) {
            Vector<XWikiNotificationRule> vnamedrules = namedrules.get(name);
            if (vnamedrules == null) {
                vnamedrules = new Vector<XWikiNotificationRule>();
                namedrules.put(name, vnamedrules);
            }
            vnamedrules.add(rule);
        }
    }

    /**
     * Remove all rules with the given name
     */
    public void removeNamedRule(String name)
    {
        synchronized (namedrules) {
            Vector<XWikiNotificationRule> vnamedrules = namedrules.get(name);
            if (vnamedrules != null) {
                vnamedrules.removeAllElements();
            }
            namedrules.remove(name);
        }
    }

    /**
     * Remove the given rule (if it exists) from the rules with the given name
     */
    public void removeNamedRule(String name, XWikiNotificationRule rule)
    {
        synchronized (namedrules) {
            Vector<XWikiNotificationRule> vnamedrules = namedrules.get(name);
            if (vnamedrules != null) {
                vnamedrules.remove(rule);
                if (vnamedrules.size() == 0)
                    namedrules.remove(name);
            }
        }
    }

    /**
     * @return all named rules with the given name
     */
    public Vector<XWikiNotificationRule> getNamedRules(String name)
    {
        synchronized (namedrules) {
            return namedrules.get(name);
        }
    }

    public void preverify(XWikiDocument newdoc, XWikiDocument olddoc, int event, XWikiContext context)
    {
        // Call rules explicitly for any actions of this document
        Vector<XWikiNotificationRule> vnamedrules;
        String name = newdoc.getFullName();
        synchronized (namedrules) {
            vnamedrules = getNamedRules(name);
            if (vnamedrules != null) {
                vnamedrules = (Vector<XWikiNotificationRule>) vnamedrules.clone();
            }

        }
        if (vnamedrules != null) {
            for (XWikiNotificationRule rule : vnamedrules) {
                rule.preverify(newdoc, olddoc, context);
            }
        }

        name = context.getWikiId() + ":" + newdoc.getFullName();

        synchronized (namedrules) {
            vnamedrules = getNamedRules(name);
            if (vnamedrules != null)
                vnamedrules = (Vector<XWikiNotificationRule>) vnamedrules.clone();
        }
        if (vnamedrules != null) {
            for (XWikiNotificationRule rule : vnamedrules) {
                rule.preverify(newdoc, olddoc, context);
            }
        }

        Vector<XWikiNotificationRule> grules;
        synchronized (generalrules) {
            grules = (Vector<XWikiNotificationRule>) generalrules.clone();
        }

        for (XWikiNotificationRule rule : grules) {
            rule.preverify(newdoc, olddoc, context);
        }
    }

    public void verify(XWikiDocument newdoc, XWikiDocument olddoc, int event, XWikiContext context)
    {
        // Call rules explicitly for any actions of this document
        Vector<XWikiNotificationRule> vnamedrules;
        String name = newdoc.getFullName();
        synchronized (namedrules) {
            vnamedrules = getNamedRules(name);
            if (vnamedrules != null)
                vnamedrules = (Vector<XWikiNotificationRule>) vnamedrules.clone();
        }
        if (vnamedrules != null) {
            for (XWikiNotificationRule rule : vnamedrules) {
                rule.verify(newdoc, olddoc, context);
            }
        }

        name = context.getWikiId() + ":" + newdoc.getFullName();

        synchronized (namedrules) {
            vnamedrules = getNamedRules(name);
            if (vnamedrules != null)
                vnamedrules = (Vector<XWikiNotificationRule>) vnamedrules.clone();
        }
        if (vnamedrules != null) {
            for (XWikiNotificationRule rule : vnamedrules) {
                rule.verify(newdoc, olddoc, context);
            }
        }

        Vector<XWikiNotificationRule> grules;
        synchronized (generalrules) {
            grules = (Vector<XWikiNotificationRule>) generalrules.clone();
        }
        for (XWikiNotificationRule rule : grules) {
            rule.verify(newdoc, olddoc, context);
        }
    }

    public void verify(XWikiDocument doc, String action, XWikiContext context)
    {
        // Call rules explicitly for any actions of this document
        Vector<XWikiNotificationRule> vnamedrules;
        String name = doc.getFullName();
        synchronized (namedrules) {
            vnamedrules = getNamedRules(name);
            if (vnamedrules != null)
                vnamedrules = (Vector<XWikiNotificationRule>) vnamedrules.clone();
        }
        if (vnamedrules != null) {
            for (XWikiNotificationRule rule : vnamedrules) {
                rule.verify(doc, action, context);
            }
        }
        name = context.getWikiId() + ":" + doc.getFullName();
        synchronized (namedrules) {
            vnamedrules = getNamedRules(name);
            if (vnamedrules != null)
                vnamedrules = (Vector<XWikiNotificationRule>) vnamedrules.clone();
        }
        if (vnamedrules != null) {
            for (XWikiNotificationRule rule : vnamedrules) {
                rule.verify(doc, action, context);
            }
        }
        Vector<XWikiNotificationRule> grules;
        synchronized (generalrules) {
            grules = (Vector<XWikiNotificationRule>) generalrules.clone();
        }
        for (XWikiNotificationRule rule : grules) {
            rule.verify(doc, action, context);
        }
    }

    public void preverify(XWikiDocument doc, String action, XWikiContext context)
    {
        // Call rules explicitly for any actions of this document
        Vector<XWikiNotificationRule> vnamedrules;
        String name = doc.getFullName();
        synchronized (namedrules) {
            vnamedrules = getNamedRules(name);
            if (vnamedrules != null)
                vnamedrules = (Vector<XWikiNotificationRule>) vnamedrules.clone();
        }
        if (vnamedrules != null) {
            for (XWikiNotificationRule rule : vnamedrules) {
                rule.preverify(doc, action, context);
            }
        }
        Vector<XWikiNotificationRule> grules;
        synchronized (generalrules) {
            grules = (Vector<XWikiNotificationRule>) generalrules.clone();
        }
        for (XWikiNotificationRule rule : grules) {
            rule.preverify(doc, action, context);
        }
    }
}
