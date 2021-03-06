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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.bootstrap.AnalysisProperties;
import org.sonar.batch.bootstrap.GlobalMode;
import org.sonar.batch.bootstrap.WSLoader;
import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.batch.rule.ModuleQProfiles;
import org.sonar.batch.util.BatchUtils;

public class DefaultProjectRepositoriesLoader extends AbstractServerLoader implements ProjectRepositoriesLoader {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultProjectRepositoriesLoader.class);
  private static final String BATCH_PROJECT_URL = "/batch/project";

  private final WSLoader wsLoader;
  private final GlobalMode globalMode;

  public DefaultProjectRepositoriesLoader(WSLoader wsLoader, GlobalMode globalMode) {
    this.wsLoader = wsLoader;
    this.globalMode = globalMode;
  }

  @Override
  public ProjectRepositories load(ProjectReactor reactor, AnalysisProperties taskProperties) {
    String projectKey = reactor.getRoot().getKeyWithBranch();
    String url = BATCH_PROJECT_URL + "?key=" + BatchUtils.encodeForUrl(projectKey);
    if (taskProperties.properties().containsKey(ModuleQProfiles.SONAR_PROFILE_PROP)) {
      LOG.warn("Ability to set quality profile from command line using '" + ModuleQProfiles.SONAR_PROFILE_PROP
        + "' is deprecated and will be dropped in a future SonarQube version. Please configure quality profile used by your project on SonarQube server.");
      url += "&profile=" + BatchUtils.encodeForUrl(taskProperties.properties().get(ModuleQProfiles.SONAR_PROFILE_PROP));
    }
    url += "&preview=" + globalMode.isPreview();
    ProjectRepositories projectRepositories = ProjectRepositories.fromJson(load(url));
    validateProjectRepositories(projectRepositories, reactor.getRoot().getKey());
    return projectRepositories;
  }

  private String load(String resource) {
    WSLoaderResult<String> result = wsLoader.loadString(resource);
    super.loadedFromCache = result.isFromCache();
    return result.get();
  }

  private static void validateProjectRepositories(ProjectRepositories projectRepositories, String projectKey) {
    if (projectRepositories.qProfiles().isEmpty()) {
      throw MessageException.of("No quality profiles has been found this project, you probably don't have any language plugin suitable for this analysis.");
    }
  }
}
