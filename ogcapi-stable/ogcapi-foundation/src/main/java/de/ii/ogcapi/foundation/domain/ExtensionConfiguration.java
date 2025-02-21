/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.google.common.base.CaseFormat;
import de.ii.xtraplatform.base.domain.JacksonProvider;
import de.ii.xtraplatform.store.domain.entities.Mergeable;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.Buildable;
import de.ii.xtraplatform.store.domain.entities.maptobuilder.BuildableBuilder;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.CUSTOM,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "buildingBlock")
@JsonTypeIdResolver(JacksonProvider.DynamicTypeIdResolver.class)
public interface ExtensionConfiguration
    extends Buildable<ExtensionConfiguration>, Mergeable<ExtensionConfiguration> {

  abstract class Builder implements BuildableBuilder<ExtensionConfiguration> {

    public abstract Builder defaultValues(ExtensionConfiguration defaultValues);
  }

  static String getBuildingBlockIdentifier(Class<? extends ExtensionConfiguration> clazz) {
    return CaseFormat.UPPER_CAMEL.to(
        CaseFormat.UPPER_UNDERSCORE,
        clazz.getSimpleName().replace("Immutable", "").replace("Configuration", ""));
  }

  @JsonAlias("extensionType")
  @Value.Derived
  default String getBuildingBlock() {
    return getBuildingBlockIdentifier(this.getClass());
  }

  /**
   * @langEn Enable the building block?
   * @langDe Das Modul aktivieren?
   * @default false
   */
  @Nullable
  Boolean getEnabled();

  @JsonIgnore
  @Value.Derived
  @Value.Auxiliary
  default boolean isEnabled() {
    return Objects.equals(getEnabled(), true);
  }

  @JsonIgnore
  @Value.Auxiliary
  Optional<ExtensionConfiguration> getDefaultValues();

  @Override
  default ExtensionConfiguration mergeInto(ExtensionConfiguration source) {
    return source.getBuilder().from(source).from(this).build();
  }
}
