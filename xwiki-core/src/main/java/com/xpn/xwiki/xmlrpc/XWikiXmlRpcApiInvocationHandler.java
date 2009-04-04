package com.xpn.xwiki.xmlrpc;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Formatter;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.xmlrpc.XmlRpcException;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletContainerException;
import org.xwiki.container.servlet.ServletContainerInitializer;
import org.xwiki.context.Execution;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * This is the invocation handler used by the proxy object that will provide the implementation of the XML RPC API. This
 * handler performs component initialization and cleanup at each method call.
 * 
 * @version $Id$
 */
public class XWikiXmlRpcApiInvocationHandler implements InvocationHandler
{
    private ServletContext servletContext;

    private HttpServletRequest request;

    private HttpServletResponse response;

    public XWikiXmlRpcApiInvocationHandler(ServletContext servletContext, HttpServletRequest request)
    {
        this.servletContext = servletContext;
        this.request = request;

        /* Mock the response */
        response =
            (HttpServletResponse) Proxy.newProxyInstance(HttpServletResponse.class.getClassLoader(),
                new Class[] {HttpServletResponse.class}, new InvocationHandler()
                {
                    public Object invoke(Object object, Method method, Object[] args) throws Throwable
                    {
                        /* Just return null for everything */
                        return null;
                    }
                });
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
        XWikiContext context = null;

        try {
            /*
             * Here we prepare the XWikiXmlRpcContext object and we put it in the config object so that XMLRPC methods
             * can get it. The config object is the same that is passed to the XWikiXmlRpcHandler when it is
             * initialized. So modifications made here are reflected in the config object that is available to XMLRPC
             * methods through the field XWikiXmlRpcHandler.xwikiXmlRpcHttpRequestConfig.
             */
            XWikiRequest xwikiRequest = new XWikiXmlRpcRequest(request);
            XWikiResponse xwikiResponse = new XWikiXmlRpcResponse(response);
            XWikiServletContext xwikiServletContext = new XWikiServletContext(servletContext);

            context = Utils.prepareContext("", xwikiRequest, xwikiResponse, xwikiServletContext);

            /* Initialize new Container subsystem */
            initializeContainerComponent(context);

            XWiki xwiki = XWiki.getXWiki(context);
            XWikiURLFactory urlf = xwiki.getURLFactoryService().createURLFactory(context.getMode(), context);
            context.setURLFactory(urlf);

            /* Create the implementation object and initialize it with relevant XWiki objects */
            XWikiXmlRpcApiImpl xwikiXmlRpcApiImpl =
                new XWikiXmlRpcApiImpl(context, xwiki, new com.xpn.xwiki.api.XWiki(xwiki, context));
            Method m = xwikiXmlRpcApiImpl.getClass().getMethod(method.getName(), method.getParameterTypes());

            return m.invoke(xwikiXmlRpcApiImpl, args);
        } catch (Exception e) {
            /*
             * Here we cannot pass exceptions through XMLRPC. So, in order to have a dump on the client side of what
             * happened we use the stacktrace dump as the exception message.
             */
            throw new Exception(getExceptionMessage(e.getCause()));
        } finally {
            // Cleanup code
            if (context != null) {
                cleanupComponents();
            }
        }
    }

    /**
     * <p>
     * Format a message containing the server side stack trace to be used as the message of the exception sent to the
     * client.
     * </p>
     * <p>
     * The message is in the following format:<br/>
     * 
     * <pre>
     * Exception message (1 line)
     * [START] Server stacktrace (1 line)
     * Actual server stacktrace (n lines)
     * [END] Server stacktrace (1 line)
     * </pre>
     * 
     * </p>
     * 
     * @param throwable
     * @return A string containing the stack trace of the exception passed as parameter.
     */
    private String getExceptionMessage(Throwable throwable)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        Formatter f = new Formatter(pw);
        f.format("%s\n", throwable.getMessage());
        f.format("[START] Server stacktrace\n");
        throwable.printStackTrace(pw);
        f.format("[END] Server stacktrace\n");
        pw.flush();
        pw.close();

        return sw.toString();
    }

    private void initializeContainerComponent(XWikiContext context) throws XmlRpcException
    {
        /*
         * Initialize the Container fields (request, response, session). Note that this is a bridge between the old core
         * and the component architecture. In the new component architecture we use ThreadLocal to transport the
         * request, response and session to components which require them.
         */
        ServletContainerInitializer containerInitializer =
            (ServletContainerInitializer) Utils.getComponent(ServletContainerInitializer.class);

        try {
            containerInitializer.initializeRequest(context.getRequest().getHttpServletRequest(), context);
            containerInitializer.initializeResponse(context.getResponse().getHttpServletResponse());
            containerInitializer.initializeSession(context.getRequest().getHttpServletRequest());
        } catch (ServletContainerException e) {
            throw new XmlRpcException("Failed to initialize request/response or session", e);
        }
    }

    private void cleanupComponents()
    {
        Container container = (Container) Utils.getComponent(Container.class);
        Execution execution = (Execution) Utils.getComponent(Execution.class);

        /*
         * We must ensure we clean the ThreadLocal variables located in the Container and Execution components as
         * otherwise we will have a potential memory leak.
         */
        container.removeRequest();
        container.removeResponse();
        container.removeSession();
        execution.removeContext();
    }

}
