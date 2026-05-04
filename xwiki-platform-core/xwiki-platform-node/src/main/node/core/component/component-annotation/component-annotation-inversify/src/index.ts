/**
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
import {
  inject as inversifyInject,
  injectable as inversifyInjectable,
  multiInject,
  named as inversifyNamed,
  tagged,
} from "inversify";

/**
 * Injects a dependency by identifier.
 * @param id - The symbol identifier for the dependency
 * @beta
 * @since 18.4.0RC1
 */
function inject(id: symbol): ParameterDecorator {
  return inversifyInject(id);
}

/**
 * Injects all dependencies matching the given identifier.
 * @param id - The symbol identifier for the dependencies
 * @beta
 * @since 18.4.0RC1
 */
function injectAll(id: symbol): ParameterDecorator {
  return (target, propertyKey, parameterIndex) => {
    multiInject(id)(target, propertyKey, parameterIndex);
    tagged("all", true)(target, propertyKey, parameterIndex);
  };
}

/**
 * Names a dependency for disambiguation when multiple implementations exist.
 * @param name - The name identifier for the dependency
 * @beta
 * @since 18.4.0RC1
 */
function named(name: string): ParameterDecorator {
  return inversifyNamed(name);
}

/**
 * Marks a class as injectable in the component container.
 * @beta
 * @since 18.4.0RC1
 */
function injectable(): ClassDecorator {
  return inversifyInjectable();
}

export { inject, injectAll, injectable, named };
