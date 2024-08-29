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

import DisplayerDocTitle from "../../../displayers/DisplayerDocTitle";
import {initWrapper} from "./displayerTestsHelper";

describe('DisplayerDocTitle.vue', () => {
  it('Renders an entry in view mode with doc.title_raw defined', () => {
    const wrapper = initWrapper(DisplayerDocTitle, {
      props: {
        propertyId: 'doc.title',
        entry: {
          'doc.title': 'Test',
          colorHref: 'entryLink',
          'doc.title_raw': 'entryLink'
        }
      },
      logic: {
        getDisplayerDescriptor() {
          return {
            propertyHref: 'colorHref'
          };
        },
        isContentTrusted: () => true
      }
    });
    expect(wrapper.find('a').html()).toBe('<a href="entryLink" class="">Test <sup>1</sup></a>');
  })

  it('Renders an entry in view mode with doc.title_raw undefined', () => {
    const wrapper = initWrapper(DisplayerDocTitle, {
      props: {
        propertyId: 'doc.title',
        entry: {
          'doc.title': 'Test',
          colorHref: 'entryLink',
        }
      },
      logic: {
        getDisplayerDescriptor() {
          return {
            propertyHref: 'colorHref'
          };
        }
      }
    });
    expect(wrapper.find('a').html()).toBe('<a href="entryLink" class="">Test</a>');
  })
})
