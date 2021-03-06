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

import org.sonar.api.utils.HttpDownloader;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.apache.commons.io.IOUtils;
import org.mockito.Mockito;
import org.mockito.InOrder;

import java.io.IOException;
import java.net.URI;

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyInt;
import org.sonar.batch.bootstrap.WSLoader.LoadStrategy;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.junit.Before;
import org.sonar.home.cache.PersistentCache;
import org.mockito.Mock;

public class WSLoaderTest {
  private final static String ID = "/dummy";
  private final static String cacheValue = "cache";
  private final static String serverValue = "server";

  @Mock
  private ServerClient client;
  @Mock
  private PersistentCache cache;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    when(client.load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt())).thenReturn(IOUtils.toInputStream(serverValue));
    when(cache.get(ID, null)).thenReturn(cacheValue.getBytes());
    when(client.getURI(anyString())).thenAnswer(new Answer<URI>() {
      @Override
      public URI answer(InvocationOnMock invocation) throws Throwable {
        return new URI((String) invocation.getArguments()[0]);
      }
    });
  }

  @Test
  public void dont_retry_server() throws IOException {
    when(client.load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt())).thenThrow(new IllegalStateException());
    WSLoader loader = new WSLoader(true, cache, client);
    loader.setStrategy(LoadStrategy.SERVER_FIRST);

    assertResult(loader.loadString(ID), cacheValue, true);
    assertResult(loader.loadString(ID), cacheValue, true);

    // only try once the server
    verify(client, times(1)).load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt());
    verify(cache, times(2)).get(ID, null);
  }

  @Test
  public void test_cache_strategy_fallback() throws IOException {
    when(cache.get(ID, null)).thenReturn(null);
    WSLoader loader = new WSLoader(true, cache, client);
    loader.setStrategy(LoadStrategy.CACHE_FIRST);

    assertResult(loader.load(ID), serverValue.getBytes(), false);

    InOrder inOrder = Mockito.inOrder(client, cache);
    inOrder.verify(cache).get(ID, null);
    inOrder.verify(client).load(eq(ID), anyString(), anyBoolean(), anyInt(), anyInt());
  }

  @Test
  public void test_server_strategy_fallback() throws IOException {
    when(client.load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt())).thenThrow(new IllegalStateException());
    WSLoader loader = new WSLoader(true, cache, client);
    loader.setStrategy(LoadStrategy.SERVER_FIRST);

    assertResult(loader.loadString(ID), cacheValue, true);

    InOrder inOrder = Mockito.inOrder(client, cache);
    inOrder.verify(client).load(eq(ID), anyString(), anyBoolean(), anyInt(), anyInt());
    inOrder.verify(cache).get(ID, null);
  }

  @Test
  public void test_put_cache() throws IOException {
    WSLoader loader = new WSLoader(true, cache, client);
    loader.setStrategy(LoadStrategy.SERVER_FIRST);
    loader.load(ID);
    verify(cache).put(ID, serverValue.getBytes());
  }

  @Test(expected = NullPointerException.class)
  public void test_throw_cache_exception_fallback() throws IOException {
    when(client.load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt())).thenThrow(new IllegalStateException());
    when(cache.get(ID, null)).thenThrow(new NullPointerException());

    WSLoader loader = new WSLoader(true, cache, client);
    loader.setStrategy(LoadStrategy.SERVER_FIRST);

    loader.load(ID);
  }

  @Test(expected = IllegalStateException.class)
  public void test_throw_cache_exception() throws IOException {
    when(cache.get(ID, null)).thenThrow(new IllegalStateException());

    WSLoader loader = new WSLoader(true, cache, client);
    loader.setStrategy(LoadStrategy.CACHE_FIRST);

    loader.load(ID);
  }

  @Test(expected = IllegalStateException.class)
  public void test_throw_http_exceptions() {
    HttpDownloader.HttpException httpException = mock(HttpDownloader.HttpException.class);
    IllegalStateException wrapperException = new IllegalStateException(httpException);

    when(client.load(anyString(), anyString(), anyBoolean(), anyInt(), anyInt())).thenThrow(wrapperException);

    WSLoader loader = new WSLoader(true, cache, client);
    loader.setStrategy(LoadStrategy.SERVER_FIRST);

    try {
      loader.load(ID);
    } catch (IllegalStateException e) {
      // cache should not be used
      verifyNoMoreInteractions(cache);
      throw e;
    }
  }

  @Test
  public void test_change_strategy() throws IOException {
    WSLoader loader = new WSLoader(true, cache, client);
    loader.setStrategy(LoadStrategy.CACHE_FIRST);
    test_cache_strategy_fallback();
  }

  @Test
  public void test_enable_cache() throws IOException {
    WSLoader loader = new WSLoader(true, cache, client);
    loader.setCacheEnabled(false);
    test_cache_disabled();
  }

  @Test
  public void test_server_strategy() throws IOException {
    WSLoader loader = new WSLoader(true, cache, client);
    loader.setStrategy(LoadStrategy.SERVER_FIRST);
    assertResult(loader.load(ID), serverValue.getBytes(), false);

    // should not fetch from cache
    verify(cache).put(ID, serverValue.getBytes());
    verifyNoMoreInteractions(cache);
  }

  @Test
  public void test_cache_disabled() throws IOException {
    WSLoader loader = new WSLoader(cache, client);
    loader.load(ID);

    // should not even put
    verifyNoMoreInteractions(cache);
  }

  @Test
  public void test_string() {
    WSLoader loader = new WSLoader(cache, client);
    assertResult(loader.loadString(ID), serverValue, false);
  }

  private <T> void assertResult(WSLoaderResult<T> result, T expected, boolean fromCache) {
    assertThat(result).isNotNull();
    assertThat(result.get()).isEqualTo(expected);
    assertThat(result.isFromCache()).isEqualTo(fromCache);
  }
}
