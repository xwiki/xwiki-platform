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
package org.xwiki.rendering.macro.script;

import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.context.ExecutionContext;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.script.ScriptContextManager;

/**
 * Base Class for script evaluation macros based on JSR223.
 * 
 * @param <P> the type of macro parameters bean.
 * @version $Id$urn engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
g

        return engine.compile(content);
    }
}
