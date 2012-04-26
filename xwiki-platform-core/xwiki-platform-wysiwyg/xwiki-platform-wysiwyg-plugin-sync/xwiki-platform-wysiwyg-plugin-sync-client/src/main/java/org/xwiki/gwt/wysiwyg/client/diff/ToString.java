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
package org.xwiki.gwt.wysiwyg.client.diff;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * This class delegates handling of the to a StringBuffer based version.
 * 
 * @version $Revision: 1.1 $ $Date: 2006/03/12 00:24:21 $
 * @author <a href="mailto:juanco@suigeneris.org">Juanco Anez</a>
 */
public class ToString implements IsSerializable
{
    public ToString()
    {
    }

    /**
     * Default implementation of the
     * {@link java.lang.Object#toString toString() } method that delegates work
     * to a {@link java.lang.StringBuffer StringBuffer} base version.
     */
    public String toString()
    {
        StringBuffer s = new StringBuffer();
        toString(s);
        return s.toString();
    }

    /**
     * Place a string image of the object in a StringBuffer.
     * 
     * @param s
     *            the string buffer.
     */
    public void toString(StringBuffer s)
    {
        s.append(super.toString());
    }

    /**
     * Breaks a string into an array of strings. Use the value of the
     * <code>line.separator</code> system property as the linebreak character.
     * 
     * @param value
     *            the string to convert.
     */
    /*
    public static String[] stringToArray(String value)
    {
        ArrayList list = new ArrayList();
        String[] lines = value.replaceAll("\r", "").split("\n");
        for (int i=0;i<lines.length;i++) {
           splitLine(list, lines[i]);
        }
        String[] result = new String[list.size() + 2];
        for (int i=0;i<list.size();i++) {
            result[i+1] = (String) list.get(i);
        }
        // Add placeholder strings at the beginning and end
        result[0] = "BEGIN";
        result[list.size()+1] = "END";
        return result;

    }

    public static void splitLine(ArrayList list, String line) {
        boolean isTag = false;
        StringBuffer currentToken = new StringBuffer();
        for (int i=0;i<line.length();i++) {
            char c = line.charAt(i);
            if (!isTag)  {
                if (c=='<') {
                    list.add(currentToken.toString());
                    currentToken = new StringBuffer();
                    currentToken.append(c);
                    isTag = true;
                } else if (c==' ') {
                    list.add(currentToken.toString());
                    list.add(" ");
                    currentToken = new StringBuffer();
                } else {
                    currentToken.append(c);
                }
            } else if (isTag && (c == '>')) {
               currentToken.append(c);
               list.add(currentToken.toString());
               currentToken = new StringBuffer();
               isTag = false;
            } else {
                currentToken.append(c);
            }
        }
        if (currentToken.length()>0) {
            list.add(currentToken.toString());
        }
        list.add("\n");
    }

    /**
     * Converts an array of {@link Object Object} to a string using the given
     * line separator.
     * 
     * @param o                   
     *            the array of objects.
     */
    /*
    public static String arrayToString(Object[] o)
    {
        StringBuffer buf = new StringBuffer();
        // ignore first and last element
        for (int i = 1; i < o.length - 1; i++)
        {
            buf.append(o[i]);
        }
        return buf.toString().replaceAll("\r", "");
    } */

    public static String[] stringToArray(String value)
    {
      value = value.replaceAll("\r", "");
      String[] result = new String[value.length() + 2];
      result[0] = "BEGIN";
      for (int i=0;i<value.length();i++) {
          result[i+1] = "" + value.charAt(i);
      }
      result[result.length-1] = "END";
      return result;
    }

    public static String arrayToString(Object[] o)
    {
        StringBuffer buf = new StringBuffer();
        // ignore first and last element
        for (int i = 1; i < o.length - 1; i++)
        {
            buf.append(o[i]);
        }
        return buf.toString().replaceAll("\r", "");
    }
}
