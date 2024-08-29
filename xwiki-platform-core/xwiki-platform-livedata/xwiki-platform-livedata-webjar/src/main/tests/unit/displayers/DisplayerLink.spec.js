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

import DisplayerLink from "../../../displayers/DisplayerLink";
import {initWrapper} from "./displayerTestsHelper";

describe('DisplayerLink.vue', () => {

  it('Renders an entry in view mode', () => {
    const wrapper = initWrapper(DisplayerLink, {
      props: {
        entry: {
          color: 'yellow',
          colorHref: 'entryLink'
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
    expect(wrapper.text()).toMatch('yellow')
    expect(wrapper.find('a').element.href).toBe('http://localhost/entryLink');
  })

  it('Renders an entry in view mode with untrusted content', () => {
    const logic = {
      getDisplayerDescriptor() {
        return {
          propertyHref: 'colorHref'
        };
      },
      isContentTrusted: () => false
    };
    const wrapperHttpLink = initWrapper(DisplayerLink, {
      props: {
        entry: {
          color: 'yellow<script>console.log("hello")</script>',
          colorHref: 'http://test.com'
        }
      },
      logic
    });
    expect(wrapperHttpLink.text()).toMatch('yellow')
    expect(wrapperHttpLink.find('a').element.href).toBe('http://test.com/');

    const wrapperJavascriptLink = initWrapper(DisplayerLink, {
      props: {
        entry: {
          color: 'yellow<script>console.log("hello")</script>',
          colorHref: 'javascript:console.log("world")'
        }
      },
      logic
    });

    expect(wrapperJavascriptLink.text()).toMatch('yellow')
    expect(wrapperJavascriptLink.find('a').element.href).toBe('http://localhost/#');
  })

  it('Renders an entry in view mode with an empty content', () => {
    const wrapper = initWrapper(DisplayerLink, {
      props: {
        entry: {
          colorHref: 'entryLink'
        }
      },
      logic: {
        getDisplayerDescriptor() {
          return {
            propertyHref: 'colorHref'
          };
        }
      },
      mocks: {
        $t: (key) => key
      }
    });
    expect(wrapper.text()).toMatch('livedata.displayer.link.noValue')
    expect(wrapper.find('a').element.href).toBe('http://localhost/entryLink');
  })

  it('Renders an entry in view mode when view action is not allowed', () => {
    const wrapper = initWrapper(DisplayerLink, {
      props: {
        entry: {
          colorHref: 'entryLink'
        }
      },
      logic: {
        getDisplayerDescriptor() {
          return {
            propertyHref: 'colorHref'
          };
        },
        isActionAllowed()
        {
          return false;
        },
      },
      mocks: {
        $t: (key) => key
      }
    });
    expect(wrapper.html()).toMatch('livedata.displayer.emptyValue')
  })


  it('Renders an entry in edit mode', async () => {
    const wrapper = initWrapper(DisplayerLink, {});

    await wrapper.setData({isView: false})

    expect(wrapper.find('input').element.value).toBe("red")
    expect(wrapper.find('input').element).toHaveFocus()
  })

  it('Send events after the end of the edit', async () => {
    const values = [];
    const wrapper = initWrapper(DisplayerLink, {
      logic: {
        getDisplayerDescriptor() {
          return {
            html: false
          };
        }
      },
      editBus: {
        start() {
        },
        save(entry, propertyId, v) {
          values.push({[propertyId]: v})
        }
      }
    });

    await wrapper.setData({isView: false})

    let inputField = wrapper.find('input');

    await inputField.setValue('blue');
    await wrapper.find('.edit > div').trigger('keypress.enter');

    expect(values).toMatchObject([{
      color: {
        color: "blue",
      }
    }]);
  })
})
