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
package org.xwiki.uiextension;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.rendering.block.Block;

public class UIExtensions
{
    public static class TestUix1valueZ implements UIExtension
    {
        @Override
        public String getId()
        {
            return "platform.testuix1";
        }

        @Override
        public String getExtensionPointId()
        {
            return "platform.test";
        }

        @Override
        public Map<String, String> getParameters()
        {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("key", "valueZ");
            return parameters;
        }

        @Override
        public Block execute()
        {
            return null;
        }
    }

    public static class TestUix2valueY implements UIExtension
    {
        @Override
        public String getId()
        {
            return "platform.testuix2";
        }

        @Override
        public String getExtensionPointId()
        {
            return "platform.test";
        }

        @Override
        public Map<String, String> getParameters()
        {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("key", "valueY");
            return parameters;
        }

        @Override
        public Block execute()
        {
            return null;
        }
    }

    public static class TestUix3valueX implements UIExtension
    {
        @Override
        public String getId()
        {
            return "platform.testuix3";
        }

        @Override
        public String getExtensionPointId()
        {
            return "platform.test";
        }

        @Override
        public Map<String, String> getParameters()
        {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("key", "valueX");
            return parameters;
        }

        @Override
        public Block execute()
        {
            return null;
        }
    }

    public static class TestUix4valueW implements UIExtension
    {
        @Override
        public String getId()
        {
            return "platform.testuix4";
        }

        @Override
        public String getExtensionPointId()
        {
            return "platform.test";
        }

        @Override
        public Map<String, String> getParameters()
        {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("key", "valueW");
            return parameters;
        }

        @Override
        public Block execute()
        {
            return null;
        }
    }


    public static class TestUix5value1 implements UIExtension
    {
        @Override
        public String getId()
        {
            return "platform.testuix5";
        }

        @Override
        public String getExtensionPointId()
        {
            return "platform.test";
        }

        @Override
        public Map<String, String> getParameters()
        {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("key", "1");
            return parameters;
        }

        @Override
        public Block execute()
        {
            return null;
        }
    }

    public static class TestUix6value11 implements UIExtension
    {
        @Override
        public String getId()
        {
            return "platform.testuix6";
        }

        @Override
        public String getExtensionPointId()
        {
            return "platform.test";
        }

        @Override
        public Map<String, String> getParameters()
        {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("key", "11");
            return parameters;
        }

        @Override
        public Block execute()
        {
            return null;
        }
    }


    public static class TestUix7value2 implements UIExtension
    {
        @Override
        public String getId()
        {
            return "platform.testuix7";
        }

        @Override
        public String getExtensionPointId()
        {
            return "platform.test";
        }

        @Override
        public Map<String, String> getParameters()
        {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("key", "2");
            return parameters;
        }

        @Override
        public Block execute()
        {
            return null;
        }
    }
}
