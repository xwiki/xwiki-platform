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
package org.xwiki.extension.xar.internal.handler.packager.xml;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.properties.ConverterManager;

import com.xpn.xwiki.XWikiContext;

/**
 * @version $Id$
 * @since 4.0M1
 */
public class AbstractHandler extends DefaultHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHandler.class);

    private ComponentManager componentManager;

    private ConverterManager converterManager;

    private Object currentBean;

    private ContentHandler currentHandler;

    private int currentHandlerLevel;

    private int depth = 0;

    protected StringBuffer value;

    protected Set<String> skippedElements = new HashSet<String>();

    protected Set<String> supportedElements;

    public AbstractHandler(ComponentManager componentManager)
    {
        this.componentManager = componentManager;

        try {
            this.converterManager = this.componentManager.getInstance(ConverterManager.class);
        } catch (ComponentLookupException e) {
            LOGGER.error("Failed to get default implementation of component role [{}]", ConverterManager.class);
        }
    }

    public AbstractHandler(ComponentManager componentManager, Object currentBean)
    {
        this(componentManager);

        this.currentBean = currentBean;
    }

    protected ComponentManager getComponentManager()
    {
        return componentManager;
    }

    protected Object getCurrentBean()
    {
        return currentBean;
    }

    protected void setCurrentBean(Object currentBean)
    {
        this.currentBean = currentBean;
    }

    protected void addsupportedElements(String supportedElement)
    {
        if (this.supportedElements == null) {
            this.supportedElements = new HashSet<String>();
        }

        this.supportedElements.add(supportedElement);
    }

    public boolean isSupported(String elementName)
    {
        return !this.skippedElements.contains(elementName)
            && (this.supportedElements == null || this.supportedElements.contains(elementName));
    }

    // ContentHandler

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        if (this.currentHandler == null) {
            if (this.depth == 0) {
                startHandlerElement(uri, localName, qName, attributes);
            } else if (this.depth == 1) {
                startElementInternal(uri, localName, qName, attributes);
            }
        }

        if (this.currentHandler != null) {
            this.currentHandler.startElement(uri, localName, qName, attributes);
        }

        ++this.depth;
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        if (this.currentHandler != null) {
            this.currentHandler.characters(ch, start, length);
        } else if (this.depth == 2) {
            charactersInternal(ch, start, length);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException
    {
        --this.depth;

        try {
            if (this.currentHandler != null) {
                this.currentHandler.endElement(uri, localName, qName);

                if (this.depth == this.currentHandlerLevel) {
                    endElementInternal(uri, localName, qName);
                    this.currentHandler = null;
                }
            } else {
                if (this.depth == 0) {
                    endHandlerElement(uri, localName, qName);
                } else if (this.depth == 1) {
                    endElementInternal(uri, localName, qName);
                }
            }
        } finally {
            this.value = null;
        }
    }

    // to override

    protected void startElementInternal(String uri, String localName, String qName, Attributes attributes)
        throws SAXException
    {
        if (isSupported(qName)) {
            if (this.value == null) {
                this.value = new StringBuffer();
            } else {
                this.value.setLength(0);
            }
        }
    }

    protected void charactersInternal(char[] ch, int start, int length) throws SAXException
    {
        if (this.currentBean != null && this.value != null) {
            this.value.append(ch, start, length);
        }
    }

    protected void endElementInternal(String uri, String localName, String qName) throws SAXException
    {
        if (this.currentBean != null && this.value != null) {
            try {
                callMethod(qName);
                currentBeanModified();
            } catch (Exception e) {
                LOGGER.warn("Failed to set property [{}]", qName, e);
            }
        }
    }

    protected void callMethod(String name) throws IllegalArgumentException, IllegalAccessException,
        InvocationTargetException
    {
        String methodName = "set" + StringUtils.capitalize(name);

        try {
            Method setter = this.currentBean.getClass().getMethod(methodName, String.class);
            setter.invoke(this.currentBean, this.value.toString());
        } catch (NoSuchMethodException e) {
            for (Method medthod : this.currentBean.getClass().getMethods()) {
                if (medthod.getGenericParameterTypes().length == 1 && medthod.getName().equals(methodName)) {
                    Type type = medthod.getGenericParameterTypes()[0];

                    if (type == String.class) {
                        medthod.invoke(this.currentBean, this.value.toString());
                    } else {
                        medthod.invoke(this.currentBean, converterManager.convert(type, this.value.toString()));
                    }
                }
            }
        }
    }

    protected void currentBeanModified()
    {
        // no op
    }

    protected void startHandlerElement(String uri, String localName, String qName, Attributes attributes)
        throws SAXException
    {
        // no op
    }

    protected void endHandlerElement(String uri, String localName, String qName) throws SAXException
    {
        // no op
    }

    // tools

    protected void setCurrentHandler(ContentHandler currentHandler)
    {
        this.currentHandler = currentHandler;
        this.currentHandlerLevel = this.depth;
    }

    public ContentHandler getCurrentHandler()
    {
        return this.currentHandler;
    }

    protected ExecutionContext getExecutionContext() throws ComponentLookupException
    {
        return getComponentManager().<Execution> getInstance(Execution.class).getContext();
    }

    protected XWikiContext getXWikiContext() throws ComponentLookupException
    {
        return (XWikiContext) getExecutionContext().getProperty("xwikicontext");
    }
}
