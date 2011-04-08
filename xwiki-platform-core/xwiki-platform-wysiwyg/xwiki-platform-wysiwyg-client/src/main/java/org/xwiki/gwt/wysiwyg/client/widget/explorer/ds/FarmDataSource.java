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
package org.xwiki.gwt.wysiwyg.client.widget.explorer.ds;

import com.google.gwt.core.client.JavaScriptObject;

import com.smartgwt.client.core.BaseClass;
import com.smartgwt.client.data.DataSource;

/**
 * Wrapper for SmartClient-based FarmDataSource class.
 *
 * @version $Id$
 */
public class FarmDataSource extends DataSource
{

    /**
     * Constructor.
     */
    public FarmDataSource()
    {

    }

    /**
     * Constructor. Wraps the JavaScriptObject argument.
     *
     * @param jsObj JavaScript object to wrap.
     */
    public FarmDataSource(JavaScriptObject jsObj)
    {
        super(jsObj);
    }

    /**
     * Static method allowing to get FarmDataSource, creates the object from the JavaScriptObject argument
     * if it is not null.
     *
     * @param jsObj JavaScript object to wrap if not null.
     * @return XWikiExplorerDS object.
     */
    public static FarmDataSource getOrCreateRef(JavaScriptObject jsObj)
    {
        if (jsObj == null) {
            return null;
        }
        BaseClass obj = BaseClass.getRef(jsObj);
        if (obj != null) {
            return (FarmDataSource) obj;
        } else {
            return new FarmDataSource(jsObj);
        }
    }

    /**
     * Native JS call that creates the object.
     *
     * @return FarmDataSource JavaScript object.
     */
    public native JavaScriptObject create()
        /*-{
            var config = this.@com.smartgwt.client.core.BaseClass::getConfig()();
            return $wnd.isc.XWEDataSource.create(config);
        }-*/;
}
