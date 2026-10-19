# -*- coding: utf-8 -*-
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

from pygments.formatter import Formatter

"""
Make the processed token definitions of the regular expression lexers immutable.

The lexers keep their token definitions in lists shared by all the threads using a lexer of the same class, and
iterate over them at each position of the highlighted code. Jython synchronizes each step of the iteration over a
list, so the threads highlighting code at the same time block each other. Iterating over a tuple doesn't synchronize.
The lists are only modified while the token definitions are processed, so they are converted once processed.

Importing this module applies the change once.
"""

from pygments.lexer import RegexLexerMeta

_process_tokendef = RegexLexerMeta.process_tokendef


def _process_immutable_tokendef(cls, name, tokendefs=None):
    processed = _process_tokendef(cls, name, tokendefs)
    for state, rules in list(processed.items()):
        processed[state] = tuple(rules)
    return processed


RegexLexerMeta.process_tokendef = _process_immutable_tokendef
