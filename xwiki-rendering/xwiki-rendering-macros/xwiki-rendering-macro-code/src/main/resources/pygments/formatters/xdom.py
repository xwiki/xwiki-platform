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

__all__ = ['XDOMFormatter']


class XDOMFormatter(Formatter):
    """
    Call the provided XDOM listener.
    """
    name = 'XWiki listener'
    aliases = ['text', 'null']
    filenames = []

    def __init__(self, listener, **options):
        Formatter.__init__(self, **options)
        self.listener = listener
        self.styles = dict(self.style)

    def format(self, tokensource, outfile):
        lastval = ''
        lasttype = None
        
        for ttype, value in tokensource:
            while ttype not in self.styles:
                ttype = ttype.parent
            if ttype == lasttype:
                lastval += value
            else:
                if lastval:
                    self.listener.format(lasttype.__str__(), lastval, self.style.style_for_token(lasttype))
                lasttype = ttype
                lastval = value

        if lastval:
           self.listener.format(lasttype.__str__(), lastval, self.style.style_for_token(lasttype))
