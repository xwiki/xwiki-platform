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

/**
 * Ensure a statement is unreachable
 *
 * @param value - the error value printed when this method is reached
 *
 * @since 18.0.0RC1
 * @beta
 */
function assertUnreachable(value: never): never {
  console.error({ unreachable: value });
  throw new Error("Reached a theoretically unreachable statement");
}

/**
 * Assert that a value is in an array, and fix its type
 *
 * @since 18.0.0RC1
 * @beta
 *
 * @param array - the array to check
 * @param value - the value expected in the array
 * @param message - a message displayed in case the value is not found in the array
 *
 * @returns Whether the value is in the provided array, with the correct type
 */
function assertInArray<T, U extends T>(
  value: T,
  array: U[],
  message: string,
): U {
  if (!array.includes(value as U)) {
    throw new Error(message + ": " + value);
  }

  return value as U;
}

/**
 * Get a function's output or `null` if it thrown an error
 *
 * @since 18.0.0RC1
 * @beta
 *
 * @param func - The function to try
 *
 * @returns The function's output, or `null` if it thrown an error
 */
function tryFallible<T>(func: () => T): T | null {
  try {
    return func();
  } catch {
    return null;
  }
}

/**
 * Get a function's output or the thrown error
 * Will construct a new Error object if the thrown value is not an instance of the Error class
 *
 * @since 18.0.0RC1
 * @beta
 *
 * @param func - The function to try
 *
 * @returns The function's output, the thrown error if it's an instance of the `Error` class, or a constructed `Error` instance
 */
// eslint-disable-next-line max-statements
function tryFallibleOrError<T>(func: () => T): T | Error {
  try {
    return func();
  } catch (e: unknown) {
    if (e instanceof Error) {
      return e;
    }

    if (typeof e === "string") {
      return new Error(e);
    }

    if (typeof e === "number" || typeof e === "boolean") {
      return new Error(e.toString());
    }

    if (e === null) {
      return new Error("null");
    }

    if (e === undefined) {
      return new Error("undefined");
    }

    console.error({ throw: e });
    return new Error("<thrown unknown value type>");
  }
}

/**
 * Get a funcion's promise's output or the thrown error
 * Will construct a new Error object if the thrown value is not an instance of the Error class
 *
 * @since 18.0.0RC1
 * @beta
 *
 * @param func - The function to try
 *
 * @returns The promise's output, the thrown/resolved error if it's an instance of the `Error` class, or a constructed `Error` instance
 */
// eslint-disable-next-line max-statements
async function tryFalliblePromiseOrError<T>(
  func: () => Promise<T>,
): Promise<T | Error> {
  try {
    return await func();
  } catch (e: unknown) {
    if (e instanceof Error) {
      return e;
    }

    if (typeof e === "string") {
      return new Error(e);
    }

    if (typeof e === "number" || typeof e === "boolean") {
      return new Error(e.toString());
    }

    if (e === null) {
      return new Error("null");
    }

    if (e === undefined) {
      return new Error("undefined");
    }

    console.error({ throw: e });
    return new Error("<thrown unknown value type>");
  }
}

/**
 * Provide a type for expressions type inference
 *
 * This is actually an identity function - the provided value is returned as is, with no other operation.
 *
 * @since 18.0.0RC1
 * @beta
 *
 * @param value - The value to return
 * @returns - The provided value
 *
 * @example `[1].concat("Hello")` // Type error
 * @example `provideTypeInference<number | string>([1].concat("Hello"))` // Works
 */
function provideTypeInference<T>(value: T): T {
  return value;
}

/**
 * Filter and map an array's values
 *
 * Combines both `.filter` and `.map` with the additional benefit of conditional type predicates
 *
 * @since 18.0.0RC1
 * @beta
 *
 * @example `filterMap([1, 2, 3], value => value >= 2 ? value.toString() : null) // ['2', '3']`
 *
 * @param array - The array to map
 * @param filterMap - The function performing the mapping and filtering. Returning `null` or `undefined` corresponds to filter values out.
 *
 * @returns The filtered and mapped array
 */
function filterMap<T, U>(
  array: T[],
  filterMap: (value: T, index: number) => U | null | undefined,
): U[] {
  return array
    .map(filterMap)
    .filter((value) => value !== null && value !== undefined);
}

/**
 * Get correctly-typed object entries, functionally equivalent to `Object.entries`
 *
 * Fixes builtin `Object.entries` which types the returned object values as `any`
 *
 * @param obj - The object to get the entries of
 *
 * @returns - The correctly-typed object's entries
 *
 * @since 18.0.0RC1
 * @beta
 */
function objectEntries<O extends Record<string, unknown>>(
  obj: O,
): Array<[keyof O & string, O[keyof O]]> {
  return Object.entries(obj) as Array<[keyof O & string, O[keyof O]]>;
}

/**
 * Produce an HTML string representing an element
 *
 * @param tagName - The element's tag name
 * @param attrs - The element's attributes
 * @param content - The element's inner HTML content
 *
 * @returns - The string representation of the HTML element
 *
 * @since 18.0.0RC1
 * @beta
 */
function produceHtmlEl(
  tagName: string,
  attrs: Record<string, string | undefined>,
  innerHTML: string | false,
): string {
  const el = document.createElement(tagName);

  for (const [name, value] of Object.entries(attrs)) {
    if (value !== undefined) {
      el.setAttribute(name, value);
    }
  }

  if (innerHTML !== false) {
    el.innerHTML = innerHTML;
  }

  return el.outerHTML;
}

/**
 * Escape HTML-specific characters in a string
 *
 * @param str - The string to escape
 *
 * @returns - The HTML-safe string
 *
 * @since 18.0.0RC1
 * @beta
 */
function escapeHtml(str: string): string {
  // NOTE: instantiating XMLSerializer is extremely cheap, so it's not a problem even in a hot loop
  return new XMLSerializer().serializeToString(document.createTextNode(str));
}

/**
 * Find an item inside an array and return its index along the value itself
 *
 * @param array - The array to search in
 * @param predicate - The predicate to find the value
 *
 * @returns - The found value and its index, or `null` if no value was found
 *
 * @since 18.0.0RC1
 * @beta
 */
function findWithIndex<T>(
  array: T[],
  predicate: (value: T) => boolean,
): [T, number] | null {
  for (let i = 0; i < array.length; i++) {
    if (predicate(array[i])) {
      return [array[i], i];
    }
  }

  return null;
}

/**
 * Find an item inside an array and return its index along the value itself, with a type predicate
 *
 * @param array - The array to search in
 * @param predicate - The predicate to find the value and assert the value's type
 *
 * @returns - The found value and its index, or `null` if no value was found
 *
 * @since 18.0.0RC1
 * @beta
 */
function findWithIndexTypePredicate<T, U extends T>(
  array: T[],
  predicate: (value: T) => value is U,
): [U, number] | null {
  for (let i = 0; i < array.length; i++) {
    const value = array[i];

    if (predicate(value)) {
      return [value, i];
    }
  }

  return null;
}

/**
 * Generic tree structure type.
 * @since 18.0.0RC1
 * @beta
 */
type TreeNode<T> = T & {
  children?: TreeNode<T>[];
};

export {
  assertInArray,
  assertUnreachable,
  escapeHtml,
  filterMap,
  findWithIndex,
  findWithIndexTypePredicate,
  objectEntries,
  produceHtmlEl,
  provideTypeInference,
  tryFallible,
  tryFallibleOrError,
  tryFalliblePromiseOrError,
};

export type { TreeNode };
