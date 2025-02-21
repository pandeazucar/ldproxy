/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.app;

import static de.ii.ogcapi.foundation.domain.ApiSecurity.SCOPE_READ;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import dagger.assisted.AssistedFactory;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ApiMetadata;
import de.ii.ogcapi.foundation.domain.ApiSecurity;
import de.ii.ogcapi.foundation.domain.ApiSecurity.ScopeElements;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.ImmutableApiMetadata;
import de.ii.ogcapi.foundation.domain.ImmutableApiSecurity;
import de.ii.ogcapi.foundation.domain.ImmutableCollectionExtent;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiDataHydratorExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.services.domain.ImmutableServiceDataCommon;
import de.ii.xtraplatform.services.domain.Service;
import de.ii.xtraplatform.store.domain.KeyPathAlias;
import de.ii.xtraplatform.store.domain.entities.AbstractEntityFactory;
import de.ii.xtraplatform.store.domain.entities.EntityData;
import de.ii.xtraplatform.store.domain.entities.EntityDataBuilder;
import de.ii.xtraplatform.store.domain.entities.EntityFactory;
import de.ii.xtraplatform.store.domain.entities.PersistentEntity;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class OgcApiFactory extends AbstractEntityFactory<OgcApiDataV2, OgcApiEntity>
    implements EntityFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(OgcApiFactory.class);

  private final ExtensionRegistry extensionRegistry;

  @SuppressWarnings(
      "PMD.UnusedFormalParameter") // crsTransformerFactory is needed here because dagger-auto does
  // not parse OgcApiEntity yet
  @Inject
  public OgcApiFactory(
      CrsTransformerFactory crsTransformerFactory,
      ExtensionRegistry extensionRegistry,
      OgcApiFactoryAssisted ogcApiFactoryAssisted) {
    super(ogcApiFactoryAssisted);
    this.extensionRegistry = extensionRegistry;
  }

  @Override
  public String type() {
    return Service.TYPE;
  }

  @Override
  public Optional<String> subType() {
    return Optional.of(OgcApiDataV2.SERVICE_TYPE);
  }

  @Override
  public Class<? extends PersistentEntity> entityClass() {
    return OgcApiEntity.class;
  }

  @Override
  public EntityDataBuilder<OgcApiDataV2> dataBuilder() {
    return new ImmutableOgcApiDataV2.Builder()
        .enabled(true)
        .secured(true)
        .metadata(getMetadata())
        .defaultExtent(
            new ImmutableCollectionExtent.Builder()
                .spatialComputed(true)
                .temporalComputed(true)
                .build())
        .accessControl(getSecurity())
        .extensions(getBuildingBlocks());
  }

  @Override
  public EntityDataBuilder<? extends EntityData> superDataBuilder() {
    return new ImmutableServiceDataCommon.Builder().enabled(true).secured(true);
  }

  @Override
  public Class<? extends EntityData> dataClass() {
    return OgcApiDataV2.class;
  }

  @Override
  public EntityData hydrateData(EntityData entityData) {
    try {
      OgcApiDataV2 hydrated = (OgcApiDataV2) entityData;

      if (hydrated.isAuto() && LOGGER.isInfoEnabled()) {
        LOGGER.info(
            "Service with id '{}' is in auto mode, generating configuration ...", hydrated.getId());
      }

      List<OgcApiDataHydratorExtension> extensions =
          extensionRegistry.getExtensionsForType(OgcApiDataHydratorExtension.class);
      extensions.sort(Comparator.comparing(OgcApiDataHydratorExtension::getSortPriority));
      for (OgcApiDataHydratorExtension hydrator : extensions) {
        if (hydrator.isEnabledForApi(hydrated)) {
          hydrated = hydrator.getHydratedData(hydrated);
        }
      }

      return hydrated;
    } catch (Throwable e) {
      if (LOGGER.isErrorEnabled()) {
        LogContext.error(
            LOGGER, e, "Service with id '{}' could not be started", entityData.getId());
      }
      throw e;
    }
  }

  @Override
  public Optional<KeyPathAlias> getKeyPathAlias(String keyPath) {
    return getAliases().entrySet().stream()
        .filter(entry -> Objects.equals(keyPath, entry.getKey()))
        .map(Map.Entry::getValue)
        .findFirst();
  }

  private ApiMetadata getMetadata() {
    return new ImmutableApiMetadata.Builder().build();
  }

  private ApiSecurity getSecurity() {
    return new ImmutableApiSecurity.Builder()
        .enabled(true)
        .scopeElements(Set.of(ScopeElements.READ_WRITE, ScopeElements.TAG))
        .publicScopes(List.of(SCOPE_READ))
        .build();
  }

  private List<ExtensionConfiguration> getBuildingBlocks() {
    return extensionRegistry.getExtensionsForType(ApiBuildingBlock.class).stream()
        .map(ApiBuildingBlock::getDefaultConfiguration)
        .collect(Collectors.toList());
  }

  private Map<String, KeyPathAlias> getAliases() {
    return extensionRegistry.getExtensionsForType(ApiBuildingBlock.class).stream()
        .map(
            ogcApiBuildingBlock -> ogcApiBuildingBlock.getDefaultConfiguration().getBuildingBlock())
        .map(
            buildingBlock ->
                new AbstractMap.SimpleImmutableEntry<String, KeyPathAlias>(
                    buildingBlock.toLowerCase(Locale.ROOT),
                    value ->
                        ImmutableMap.of(
                            "api",
                            ImmutableList.of(
                                ImmutableMap.builder()
                                    .put("buildingBlock", buildingBlock)
                                    .putAll(value)
                                    .build()))))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  @AssistedFactory
  public interface OgcApiFactoryAssisted extends FactoryAssisted<OgcApiDataV2, OgcApiEntity> {
    @Override
    OgcApiEntity create(OgcApiDataV2 data);
  }
}
