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

import org.sonar.api.batch.AnalysisMode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.batch.bootstrap.AnalysisProperties;
import org.sonar.batch.bootstrap.ServerClient;
import org.sonar.batch.bootstrap.WSLoader;
import org.sonar.batch.bootstrap.WSLoader.LoadStrategy;
import org.sonar.home.cache.PersistentCache;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

public class WSLoaderProjectProviderTest {
  @Mock
  private PersistentCache cache;

  @Mock
  private ServerClient client;

  @Mock
  private AnalysisMode mode;

  private ProjectWSLoaderProvider loaderProvider;
  private Map<String, String> propMap;
  private AnalysisProperties props;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    loaderProvider = new ProjectWSLoaderProvider();
    propMap = new HashMap<>();
  }

  @Test
  public void testDefault() {
    props = new AnalysisProperties(propMap, null);

    WSLoader loader = loaderProvider.provide(props, mode, cache, client);
    assertThat(loader.getStrategy()).isEqualTo(LoadStrategy.SERVER_FIRST);
    assertThat(loader.isCacheEnabled()).isEqualTo(false);
  }

  @Test
  public void testSingleMode() {
    when(mode.isQuick()).thenReturn(true);
    when(mode.isPreview()).thenReturn(true);
    propMap.put("sonar.enableOffline", "true");
    props = new AnalysisProperties(propMap, null);

    WSLoader loader = loaderProvider.provide(props, mode, cache, client);
    assertThat(loader.getStrategy()).isEqualTo(LoadStrategy.CACHE_FIRST);
    assertThat(loader.isCacheEnabled()).isEqualTo(true);
  }
}
