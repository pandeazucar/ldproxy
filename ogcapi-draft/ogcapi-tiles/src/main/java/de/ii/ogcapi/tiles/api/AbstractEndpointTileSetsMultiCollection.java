/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.api;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.foundation.domain.ApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ApiOperation;
import de.ii.ogcapi.foundation.domain.ApiRequestContext;
import de.ii.ogcapi.foundation.domain.Endpoint;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.ExtensionRegistry;
import de.ii.ogcapi.foundation.domain.FormatExtension;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.ImmutableApiEndpointDefinition;
import de.ii.ogcapi.foundation.domain.ImmutableOgcApiResourceSet;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.tiles.domain.ImmutableQueryInputTileSets.Builder;
import de.ii.ogcapi.tiles.domain.TileSetsFormatExtension;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.TilesQueriesHandler;
import de.ii.xtraplatform.features.domain.FeatureProvider2;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

public abstract class AbstractEndpointTileSetsMultiCollection extends Endpoint {

  private final TilesQueriesHandler queryHandler;
  private final FeaturesCoreProviders providers;

  public AbstractEndpointTileSetsMultiCollection(
      ExtensionRegistry extensionRegistry,
      TilesQueriesHandler queryHandler,
      FeaturesCoreProviders providers) {
    super(extensionRegistry);
    this.queryHandler = queryHandler;
    this.providers = providers;
  }

  @Override
  public boolean isEnabledForApi(OgcApiDataV2 apiData) {
    Optional<TilesConfiguration> config = apiData.getExtension(TilesConfiguration.class);
    if (config.map(cfg -> !cfg.getTileProvider().requiresQuerySupport()).orElse(false)) {
      // Tiles are pre-generated as a static tile set
      return config.filter(ExtensionConfiguration::isEnabled).isPresent();
    } else {
      // Tiles are generated on-demand from a data source
      if (config
          .filter(TilesConfiguration::isEnabled)
          .filter(TilesConfiguration::isMultiCollectionEnabled)
          .isEmpty()) return false;
      // currently no vector tiles support for WFS backends
      return providers
          .getFeatureProvider(apiData)
          .map(FeatureProvider2::supportsHighLoad)
          .orElse(false);
    }
  }

  @Override
  public List<? extends FormatExtension> getFormats() {
    if (formats == null)
      formats = extensionRegistry.getExtensionsForType(TileSetsFormatExtension.class);
    return formats;
  }

  protected ApiEndpointDefinition computeDefinition(
      OgcApiDataV2 apiData,
      String apiEntrypoint,
      int sortPriority,
      String path,
      String dataType,
      List<String> tags) {
    ImmutableApiEndpointDefinition.Builder definitionBuilder =
        new ImmutableApiEndpointDefinition.Builder()
            .apiEntrypoint(apiEntrypoint)
            .sortPriority(sortPriority);
    HttpMethods method = HttpMethods.GET;
    List<OgcApiQueryParameter> queryParameters =
        getQueryParameters(extensionRegistry, apiData, path);
    String operationSummary = "retrieve a list of the available tile sets";
    Optional<String> operationDescription =
        Optional.of(
            "This operation fetches the list of multi-layer tile sets supported by this API.");
    ImmutableOgcApiResourceSet.Builder resourceBuilderSet =
        new ImmutableOgcApiResourceSet.Builder().path(path).subResourceType("Tile Set");
    ApiOperation.getResource(
            apiData,
            path,
            false,
            queryParameters,
            ImmutableList.of(),
            getContent(apiData, path),
            operationSummary,
            operationDescription,
            Optional.empty(),
            getOperationId("getTileSetsList", "dataset", dataType),
            tags)
        .ifPresent(operation -> resourceBuilderSet.putOperations(method.name(), operation));
    definitionBuilder.putResources(path, resourceBuilderSet.build());

    return definitionBuilder.build();
  }

  protected Response getTileSets(
      OgcApiDataV2 apiData,
      ApiRequestContext requestContext,
      String definitionPath,
      boolean onlyWebMercatorQuad,
      List<String> tileEncodings) {

    if (!isEnabledForApi(apiData))
      throw new NotFoundException("Multi-collection tiles are not available in this API.");

    TilesConfiguration tilesConfiguration = apiData.getExtension(TilesConfiguration.class).get();

    TilesQueriesHandler.QueryInputTileSets queryInput =
        new Builder()
            .from(getGenericQueryInput(apiData))
            .center(tilesConfiguration.getCenterDerived())
            .tileMatrixSetZoomLevels(tilesConfiguration.getZoomLevelsDerived())
            .path(definitionPath)
            .onlyWebMercatorQuad(onlyWebMercatorQuad)
            .tileEncodings(tileEncodings)
            .build();

    return queryHandler.handle(TilesQueriesHandler.Query.TILE_SETS, queryInput, requestContext);
  }
}
