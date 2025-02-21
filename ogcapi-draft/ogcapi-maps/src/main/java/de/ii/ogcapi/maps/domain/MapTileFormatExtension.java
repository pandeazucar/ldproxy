/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.maps.domain;

import static de.ii.ogcapi.collections.domain.AbstractPathParameterCollectionId.COLLECTION_ID_PATTERN;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileSet;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import java.util.List;

public abstract class MapTileFormatExtension extends TileFormatExtension {

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    return apiData
            .getExtension(MapTilesConfiguration.class)
            .filter(MapTilesConfiguration::isEnabled)
            .filter(MapTilesConfiguration::isMultiCollectionEnabled)
            .filter(
                config -> config.getTileEncodingsDerived().contains(this.getMediaType().label()))
            .isPresent()
        && apiData
            .getExtension(TilesConfiguration.class)
            .filter(TilesConfiguration::isEnabled)
            .filter(TilesConfiguration::isMultiCollectionEnabled)
            .isPresent();
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData, String collectionId) {
    return apiData
            .getExtension(MapTilesConfiguration.class, collectionId)
            .filter(MapTilesConfiguration::getEnabled)
            .filter(MapTilesConfiguration::isSingleCollectionEnabled)
            .filter(
                config -> config.getTileEncodingsDerived().contains(this.getMediaType().label()))
            .isPresent()
        && apiData
            .getExtension(TilesConfiguration.class, collectionId)
            .filter(TilesConfiguration::getEnabled)
            .filter(TilesConfiguration::isSingleCollectionEnabled)
            .isPresent();
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath) {
    List<String> formats =
        apiData
            .getExtension(MapTilesConfiguration.class)
            .map(MapTilesConfiguration::getTileEncodingsDerived)
            .orElse(ImmutableList.of());
    return isEnabledForApi(apiData)
        && definitionPath.startsWith("/map/tiles")
        && ((formats.isEmpty() && isEnabledByDefault())
            || formats.contains(getMediaType().label()));
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String collectionId, String definitionPath) {
    List<String> formats =
        apiData
            .getExtension(MapTilesConfiguration.class, collectionId)
            .map(MapTilesConfiguration::getTileEncodingsDerived)
            .orElse(ImmutableList.of());
    return isEnabledForApi(apiData, collectionId)
        && definitionPath.startsWith("/collections/{collectionId}/map/tiles")
        && ((formats.isEmpty() && isEnabledByDefault())
            || formats.contains(getMediaType().label()));
  }

  @Override
  public String getPathPattern() {
    return "^(?:/collections/" + COLLECTION_ID_PATTERN + ")?/map/tiles/\\w+/\\w+/\\w+/\\w+/?$";
  }

  @Override
  public TileSet.DataType getDataType() {
    return TileSet.DataType.map;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return MapTilesConfiguration.class;
  }
}
