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
package org.xwiki.wikistream.xml.parser;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import org.xml.sax.helpers.DefaultHandler;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

/**
 * 
 * @version $Id: 5c213c4c836ba7a506c7fae073a3c2eee28e20be $
 */
public abstract class AbstractXmlTagParser extends DefaultHandler implements XmlTagParser,Initializable
{
    

    protected Object listener;
    protected Stack<String> currentElement = new Stack<String>();
    protected Map<String,String> listenerClassMethod=null;
    
    private Stack<XmlTag> tagStack = new Stack<XmlTag>();

    public Method lookUpMethod(String methodName,Class clazz){
        
        return null;
    }

    /**
     * @return the listener
     */
    public Object getListener()
    {
        return listener;
    }

    /**
     * @param listener the listener to set
     */
    public void setListener(Object listener)
    {
        this.listenerClassMethod=Collections.unmodifiableMap(extractMethodsToMap(listener));
        this.listener = listener;
    }
    
    private Map<String,String> extractMethodsToMap(Object listener){
        Map<String,String> map=new LinkedHashMap<String, String>();
        Method[] methods=listener.getClass().getDeclaredMethods();
        
        for(Method method:methods){
            map.put(method.getName().toLowerCase(),method.getName());
        }
        
        return map;
    }
    

    @Override
    public void initialize() throws InitializationException
    {
        // TODO Auto-generated method stub
        
    }
    

}
