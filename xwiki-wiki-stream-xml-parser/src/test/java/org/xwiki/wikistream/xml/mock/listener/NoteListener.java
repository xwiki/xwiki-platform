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
package org.xwiki.wikistream.xml.mock.listener;

/**
 * 
 * @version $Id: 5c213c4c836ba7a506c7fae073a3c2eee28e20be $
 */
public class NoteListener
{

    private StringBuilder stringBuilder=new StringBuilder();
    
    public void startNote(){
        stringBuilder.append("<startNote>");
    }

    public void startTo(){
        stringBuilder.append("<startTo>");    
    }

    public void endTo(){
        
        stringBuilder.append("<startTo>");    
    }

    public void startFrom(){
        stringBuilder.append("<startFrom>");            
    }

    public void endFrom(){
        stringBuilder.append("</endFrom>");    
        
    }
    public void startHeading(){
        stringBuilder.append("<startHeading>");    
        
    }

    public void endHeading(){
        stringBuilder.append("</endHeading>");    
        
    }
    public void startBody(){
        stringBuilder.append("<startBody>");    
        
    }

    public void endBody(){
        stringBuilder.append("</endBody>");    
        
    }
    public void endNote(){
        stringBuilder.append("</endNote>");    
        
    }
    
    public StringBuilder getString(){
        return stringBuilder;
    }
    

}
