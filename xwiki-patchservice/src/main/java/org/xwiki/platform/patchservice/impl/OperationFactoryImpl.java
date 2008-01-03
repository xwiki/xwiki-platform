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

    private static Map typeMap = new HashMap();

    private static class SingletonHolder
    {
        private static OperationFactoryImpl theInstance = new OperationFactoryImpl();
    }

    private OperationFactoryImpl()
    {
        for (Iterator it = Service.providers(RWOperation.class); it.hasNext();) {
            Operation op = (Operation) it.next();
            LOG.info("Registering " + op.getClass().getCanonicalName() + " for type "
                + op.getType());
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
            Class opClass = (Class) typeMap.get(type);
            op = (RWOperation) opClass.newInstance();
        } catch (NullPointerException ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
                "No implementation registered for the operation type [" + type + "]");
        } catch (InstantiationException ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
                "Error instantiating the implementation for the operation type [" + type + "]");
        } catch (IllegalAccessException ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_NOT_IMPLEMENTED,
                "The implementation for the operation type [" + type
                    + "] does not have a public default constructor");
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

    public static void registerTypeProvider(String type, Class provider)
    {
        typeMap.put(type, provider);
    }

    public static void registerTypeProvider(String[] types, Class provider)
    {
        for (int i = 0; i < types.length; ++i) {
            registerTypeProvider(types[i], provider);
        }
    }
}
