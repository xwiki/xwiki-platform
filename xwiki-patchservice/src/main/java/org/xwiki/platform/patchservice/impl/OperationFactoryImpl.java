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
 *
 */
package org.xwiki.platform.patchservice.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.radeox.util.Service;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.OperationFactory;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiException;

public final class OperationFactoryImpl implements OperationFactory
{
    private static final Log LOG = LogFactory.getLog(OperationFactoryImpl.class);

    private static Map<String, Class< ? extends RWOperation>> typeMap =
        new HashMap<String, Class< ? extends RWOperation>>();

    private static class SingletonHolder
    {
        private static OperationFactoryImpl theInstance = new OperationFactoryImpl();
    }

    @SuppressWarnings("unchecked")
    private OperationFactoryImpl()
    {
        for (Iterator<RWOperation> it = Service.providers(RWOperation.class); it.hasNext();) {
            Operation op = it.next();
            LOG.info("Registering " + op.getClass().getCanonicalName() + " for type " + op.getType());
        }
    }

    public static OperationFactoryImpl getInstance()
    {
        return SingletonHolder.theInstance;
    }

    public RWOperation newOperation(String type) throws XWikiException
    {
        RWOperation op;
        try {
            Class< ? extends RWOperation> opClass = typeMap.get(type);
            op = opClass.newInstance();
        } catch (NullPointerException ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
                "No implementation registered for the operation type [" + type + "]");
        } catch (InstantiationException ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
                "Error instantiating the implementation for the operation type [" + type + "]");
        } catch (IllegalAccessException ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
                "The implementation for the operation type [" + type + "] does not have a public default constructor");
        }
        return op;
    }

    public Operation loadOperation(Element e) throws XWikiException
    {
        String type = e.getAttribute(AbstractOperationImpl.OPERATION_TYPE_ATTRIBUTE_NAME);
        Operation op = newOperation(type);
        op.fromXml(e);
        return op;
    }

    public static void registerTypeProvider(String type, Class< ? extends RWOperation> provider)
    {
        typeMap.put(type, provider);
    }

    public static void registerTypeProvider(String[] types, Class< ? extends RWOperation> provider)
    {
        for (int i = 0; i < types.length; ++i) {
            registerTypeProvider(types[i], provider);
        }
    }
}
