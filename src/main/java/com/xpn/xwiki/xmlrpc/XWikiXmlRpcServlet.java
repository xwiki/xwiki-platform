package com.xpn.xwiki.xmlrpc;

import com.xpn.xwiki.XWikiException;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;
import org.apache.xmlrpc.server.PropertyHandlerMapping;
import org.apache.xmlrpc.server.RequestProcessorFactoryFactory;
import org.apache.xmlrpc.webserver.XmlRpcServlet;
import org.apache.xmlrpc.webserver.XmlRpcServletServer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;

public class XWikiXmlRpcServlet extends XmlRpcServlet
{

    protected XmlRpcServletServer newXmlRpcServer(ServletConfig pConfig) throws XmlRpcException
    {
        return new XmlRpcServletServer()
        {
            protected XmlRpcHttpRequestConfigImpl newConfig(HttpServletRequest pRequest)
            {
                RequestInitializableHandler.Config config =
                    new RequestInitializableHandler.Config();
                config.setRequest(pRequest);
                return config;
            }
        };
    }

    protected PropertyHandlerMapping newPropertyHandlerMapping(URL url) throws IOException,
        XmlRpcException
    {
        PropertyHandlerMapping mapping = new PropertyHandlerMapping();
        mapping.setTypeConverterFactory(getXmlRpcServletServer().getTypeConverterFactory());

        RequestProcessorFactoryFactory factory =
            new RequestProcessorFactoryFactory.RequestSpecificProcessorFactoryFactory()
            {
                protected Object getRequestProcessor(Class pClass, XmlRpcRequest pRequest)
                    throws XmlRpcException
                {
                    Object proc = super.getRequestProcessor(pClass, pRequest);

                    if (proc instanceof RequestInitializableHandler) {
                        RequestInitializableHandler initProc = (RequestInitializableHandler) proc;
                        try {
                            ServletRequest request =
                                ((RequestInitializableHandler.Config) pRequest.getConfig())
                                    .getRequest();
                            initProc.init(XWikiXmlRpcServlet.this, request);
                        } catch (XWikiException ex) {
                            throw new XmlRpcException("Initialization Error", ex);
                        }
                    } else if (proc instanceof InitializableHandler) {
                        InitializableHandler initProc = (InitializableHandler) proc;
                        try {
                            initProc.init(XWikiXmlRpcServlet.this);
                        } catch (XWikiException ex) {
                            throw new XmlRpcException("Initialization Error", ex);
                        }
                    }
                    return proc;
                }
            };
        mapping.setRequestProcessorFactoryFactory(factory);
        mapping.load(Thread.currentThread().getContextClassLoader(), url);
        return mapping;
    }
}
