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
package org.sonar.batch.scan;

import com.google.common.collect.Lists;

import java.util.List;

import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.bootstrap.GlobalSettings;
import org.sonar.batch.protocol.input.ProjectRepositories;

/**
 * @since 2.12
 */
public class ModuleSettings extends Settings {

  private final ProjectRepositories projectReferentials;
  private ProjectAnalysisMode analysisMode;

  public ModuleSettings(GlobalSettings batchSettings, ProjectDefinition moduleDefinition, ProjectRepositories projectReferentials,
    ProjectAnalysisMode analysisMode) {
    super(batchSettings.getDefinitions());
    this.projectReferentials = projectReferentials;
    this.analysisMode = analysisMode;
    getEncryption().setPathToSecretKey(batchSettings.getString(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));

    init(moduleDefinition, batchSettings);
  }

  private ModuleSettings init(ProjectDefinition moduleDefinition, GlobalSettings batchSettings) {
    addProjectProperties(moduleDefinition, batchSettings);
    addBuildProperties(moduleDefinition);
    return this;
  }

  private void addProjectProperties(ProjectDefinition moduleDefinition, GlobalSettings batchSettings) {
    addProperties(batchSettings.getProperties());
    addProperties(projectReferentials.settings(moduleDefinition.getKeyWithBranch()));
  }

  private void addBuildProperties(ProjectDefinition project) {
    List<ProjectDefinition> orderedProjects = getTopDownParentProjects(project);
    for (ProjectDefinition p : orderedProjects) {
      addProperties(p.getProperties());
    }
  }

  /**
   * From root to given project
   */
  static List<ProjectDefinition> getTopDownParentProjects(ProjectDefinition project) {
    List<ProjectDefinition> result = Lists.newArrayList();
    ProjectDefinition p = project;
    while (p != null) {
      result.add(0, p);
      p = p.getParent();
    }
    return result;
  }

  @Override
  protected void doOnGetProperties(String key) {
    if (analysisMode.isPreview() && key.endsWith(".secured") && !key.contains(".license")) {
      throw MessageException.of("Access to the secured property '" + key
        + "' is not possible in preview mode. The SonarQube plugin which requires this property must be deactivated in preview mode.");
    }
  }
}
