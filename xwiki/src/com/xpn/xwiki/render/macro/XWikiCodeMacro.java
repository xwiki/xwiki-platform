/*
 * This file is part of "SnipSnap Radeox Rendering Engine".
 *
 * Copyright (c) 2002 Stephan J. Schmidt, Matthias L. Jugel
 * All Rights Reserved.
 *
 * Please visit http://radeox.org/ for updates and contact.
 *
 * --LICENSE NOTICE--
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * --LICENSE NOTICE--
 */

package com.xpn.xwiki.render.macro;

import org.radeox.macro.CodeMacro;

/*
 * MacroFilter for displaying programming language source code. XWikiCodeMacro knows about
 * different source code formatters which can be plugged into radeox to
 * display more languages. XWikiCodeMacro displays Java, Ruby or SQL code.
 *
 * @author stephan
 * @team sonicteam
 * @version $Id$
 */

public class XWikiCodeMacro extends CodeMacro {

  public XWikiCodeMacro() {
    super();
    addSpecial('>');
    addSpecial('<');
  }

  public String getLocaleKey() {
    return "macro.code";
  }
}
