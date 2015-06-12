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

package org.sonar.server.computation.debt;

public interface MutableDebtModelHolder extends DebtModelHolder {

  /**
   * Sets the characteristics in the {@link DebtModelHolder}. Settings characteristics more than once is not allowed
   *
   * @param rootCharacteristic a root {@link Characteristic}, can not be {@code null}
   * @param subCharacteristics list of sub characteristics of the root characteristic, can not be {@code null}
   *
   * @throws NullPointerException if {@code rootCharacteristic} is {@code null}
   * @throws NullPointerException if {@code subCharacteristics} is {@code null}
   */
  void addCharacteristics(Characteristic rootCharacteristic, Iterable<? extends Characteristic> subCharacteristics);
}