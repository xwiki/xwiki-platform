/*
 * See the LICENSE file distributed with this work for additional
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
import { type Storage } from "@xwiki/cristal-api";
import { StorageProvider } from "@xwiki/cristal-backend-api";
import { Container, inject, injectable, named } from "inversify";

@injectable("Singleton")
export class DefaultStorageProvider implements StorageProvider {
  public static bind(container: Container): void {
    container.bind("StorageProvider").to(DefaultStorageProvider).inSingletonScope();
  }

  constructor(@inject("Storage") @named("XWiki") private readonly storage: Storage) {}

  get(): Storage {
    return this.storage;
  }
}
