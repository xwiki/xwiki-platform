"""
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
"""

import pygments
from pygments.lexers import guess_lexer
from pygments.lexers import get_lexer_by_name
from pygments.styles import get_style_by_name
from pygments.util import ClassNotFound
from pygments.formatters.xdom import XDOMFormatter
from pygments.styles.xwiki_default import XWikiStyle

if language:
  try:
    pygmentLexer = get_lexer_by_name(language, stripnl=False)
  except ClassNotFound:
    pygmentLexer = None
else:
  try:
    pygmentLexer = guess_lexer(code, stripnl=False)
  except ClassNotFound:
    pygmentLexer = None

if pygmentLexer:
  pygmentStyle = None
  if style:
    if style == "xwiki":
      pygmentStyle = XWikiStyle
    else:
      try:
        pygmentStyle = get_style_by_name(style)
      except ClassNotFound:
        ""

  if pygmentStyle:
    pygmentFormatter = XDOMFormatter(listener, style=pygmentStyle)
  else:
    pygmentFormatter = XDOMFormatter(listener)

  pygments.highlight(code, pygmentLexer, pygmentFormatter)