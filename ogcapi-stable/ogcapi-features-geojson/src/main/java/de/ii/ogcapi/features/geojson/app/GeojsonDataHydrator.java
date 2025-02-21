/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.geojson.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ogcapi.features.geojson.domain.ImmutableGeoJsonConfiguration.Builder;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableFeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiDataHydratorExtension;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.xtraplatform.store.domain.entities.ValidationResult.MODE;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@AutoBind
public class GeojsonDataHydrator implements OgcApiDataHydratorExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(GeojsonDataHydrator.class);

  private final FeaturesCoreProviders providers;

  @Inject
  public GeojsonDataHydrator(FeaturesCoreProviders providers) {
    this.providers = providers;
  }

  @Override
  public int getSortPriority() {
    // this must be processed after the FeaturesCoreDataHydrator
    return 110;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return GeoJsonConfiguration.class;
  }

  @Override
  public OgcApiDataV2 getHydratedData(OgcApiDataV2 apiData) {

    // any Features GeoJSON hydration actions are not taken in STRICT validation mode;
    // STRICT: an invalid service definition will not start
    if (apiData.getApiValidation() != MODE.STRICT) return apiData;

    OgcApiDataV2 data = apiData;

    // get Features Core configurations to process, normalize property names to exclude all square
    // brackets
    Map<String, GeoJsonConfiguration> configs =
        data.getCollections().entrySet().stream()
            .map(
                entry -> {
                  // normalize the property references in transformations by removing all
                  // parts in square brackets

                  final FeatureTypeConfigurationOgcApi collectionData = entry.getValue();
                  GeoJsonConfiguration config =
                      collectionData.getExtension(GeoJsonConfiguration.class).orElse(null);
                  if (Objects.isNull(config)) return null;

                  final String collectionId = entry.getKey();
                  final String buildingBlock = config.getBuildingBlock();

                  if (config.hasDeprecatedTransformationKeys())
                    config =
                        new Builder()
                            .from(config)
                            .transformations(
                                config.normalizeTransformationKeys(buildingBlock, collectionId))
                            .build();

                  return new AbstractMap.SimpleImmutableEntry<>(collectionId, config);
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    // update data with changes
    data =
        new ImmutableOgcApiDataV2.Builder()
            .from(data)
            .collections(
                data.getCollections().entrySet().stream()
                    .map(
                        entry -> {
                          final String collectionId = entry.getKey();
                          if (!configs.containsKey(collectionId)) return entry;

                          final GeoJsonConfiguration config = configs.get(collectionId);
                          final String buildingBlock = config.getBuildingBlock();

                          return new AbstractMap.SimpleImmutableEntry<
                              String, FeatureTypeConfigurationOgcApi>(
                              collectionId,
                              new ImmutableFeatureTypeConfigurationOgcApi.Builder()
                                  .from(entry.getValue())
                                  .extensions(
                                      new ImmutableList.Builder<ExtensionConfiguration>()
                                          // do not touch any other extension
                                          .addAll(
                                              entry.getValue().getExtensions().stream()
                                                  .filter(
                                                      ext ->
                                                          !ext.getBuildingBlock()
                                                              .equals(buildingBlock))
                                                  .collect(Collectors.toUnmodifiableList()))
                                          // add the GeoJSON configuration
                                          .add(config)
                                          .build())
                                  .build());
                        })
                    .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue)))
            .build();

    return data;
  }
}
