/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.repository;

import org.sonar.batch.bootstrap.AbstractServerLoader;

import org.sonar.batch.bootstrap.WSLoaderResult;
import org.sonar.batch.bootstrap.WSLoader;
import org.sonar.batch.protocol.input.GlobalRepositories;

public class DefaultGlobalRepositoriesLoader extends AbstractServerLoader implements GlobalRepositoriesLoader {

  private static final String BATCH_GLOBAL_URL = "/batch/global";

  private final WSLoader wsLoader;

  public DefaultGlobalRepositoriesLoader(WSLoader wsLoader) {
    this.wsLoader = wsLoader;
  }

  @Override
  public GlobalRepositories load() {
    WSLoaderResult<String> result = wsLoader.loadString(BATCH_GLOBAL_URL);
    super.loadedFromCache = result.isFromCache();
    return GlobalRepositories.fromJson(result.get());
  }

}
