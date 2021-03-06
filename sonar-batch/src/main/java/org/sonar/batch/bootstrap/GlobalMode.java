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
package org.sonar.batch.bootstrap;

import org.apache.commons.lang.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;

import java.text.MessageFormat;

public class GlobalMode {
  private static final Logger LOG = LoggerFactory.getLogger(GlobalMode.class);
  private boolean preview;

  public boolean isPreview() {
    return preview;
  }

  public GlobalMode(BootstrapProperties props) {
    if (props.property(CoreProperties.DRY_RUN) != null) {
      LOG.warn(MessageFormat.format("Property {0} is deprecated. Please use {1} instead.", CoreProperties.DRY_RUN, CoreProperties.ANALYSIS_MODE));
      preview = "true".equals(props.property(CoreProperties.DRY_RUN));
    } else {
      String mode = props.property(CoreProperties.ANALYSIS_MODE);
      validate(mode);
      preview = CoreProperties.ANALYSIS_MODE_PREVIEW.equals(mode) || CoreProperties.ANALYSIS_MODE_QUICK.equals(mode);
    }

    if (preview) {
      LOG.info("Preview global mode");
    }
  }

  private void validate(String mode) {
    if (StringUtils.isEmpty(mode)) {
      return;
    }

    if (!CoreProperties.ANALYSIS_MODE_PREVIEW.equals(mode) && !CoreProperties.ANALYSIS_MODE_QUICK.equals(mode) &&
      !CoreProperties.ANALYSIS_MODE_ANALYSIS.equals(mode)) {
      throw new IllegalStateException("Invalid analysis mode: " + mode);
    }
  }
}
