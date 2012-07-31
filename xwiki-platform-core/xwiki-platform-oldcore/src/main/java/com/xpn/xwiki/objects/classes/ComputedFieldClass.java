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
package com.xpn.xwiki.objects.classes;

import org.apache.velocity.VelocityContext;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.web.Utils;

/**
 * Computed Field Class allows to create a field without storage 
 * that will display computed values based on other data
 * in the object or wiki.
 * @version $Id$
 * @since 4.2M2
 */
public class ComputedFieldClass extends PropertyClass
{
    /**
     * Constant defining the field name.
     **/
    protected static final String XCLASSNAME = "computedfield";

    /**
     * Constant defining the name of the script field.
     **/
    protected static final String FIELD_SCRIPT = "script";

    /**
     * Constructor for ComputedFieldClass.
     * @param wclass Meta Class
     */
    public ComputedFieldClass(PropertyMetaClass wclass)
    {
        super(XCLASSNAME, "Computed Field", wclass);
    }

    /**
     * Constructor for ComputedFieldClass.
     */
    public ComputedFieldClass()
    {
        this(null);
    }

    /**
     * Setter for the script value.
     * @param sValue script to be used for the computed field
     */    
    public void setScript(String sValue)
    {
        setLargeStringValue(FIELD_SCRIPT, sValue);
    }

    /**
     * Getter for the script value.
     * @return script to be used for the computed field
     */    
    public String getScript()
    {
        String sValue = getLargeStringValue(FIELD_SCRIPT);
        return sValue;
    }

    @Override
    public BaseProperty fromString(String value)
    {
        return null;
    }

    @Override
    public BaseProperty newProperty()
    {
        BaseProperty property = new StringProperty();
        property.setName(getName());
        return property;
    }

    @Override
    public void displayView(StringBuffer buffer, String name, String prefix, 
                            BaseCollection object, XWikiContext context)
    {
        String script = getScript();
        String content = "";
        try {
            VelocityContext vcontext = Utils.getComponent(VelocityManager.class).getVelocityContext();
            vcontext.put("name", name);
            vcontext.put("prefix", prefix);
            vcontext.put("object", new com.xpn.xwiki.api.Object((BaseObject) object, context));
            vcontext.put("context", new com.xpn.xwiki.api.Context(context));
            String classSyntax =
                context.getWiki().getDocument(getObject().getDocumentReference(), context).getSyntax().toIdString();
            content = context.getDoc().getRenderedContent(script, classSyntax, context);
        } catch (Exception e) {
            buffer.append(e.getMessage());
        }
        buffer.append(content);
    }
}
