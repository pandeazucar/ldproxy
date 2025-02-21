/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.projections.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.features.core.domain.FeatureQueryTransformer;
import de.ii.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ogcapi.foundation.domain.ApiExtensionCache;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.HttpMethods;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.OgcApiQueryParameter;
import de.ii.ogcapi.foundation.domain.QueryParameterSet;
import de.ii.ogcapi.foundation.domain.SchemaValidator;
import de.ii.ogcapi.foundation.domain.TypedQueryParameter;
import de.ii.ogcapi.tiles.domain.TileGenerationUserParameter;
import de.ii.ogcapi.tiles.domain.provider.ImmutableTileGenerationParametersTransient;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationSchema;
import de.ii.xtraplatform.features.domain.ImmutableFeatureQuery;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @langEn The properties that should be included for each feature. The parameter value is a
 *     comma-separated list of property names. By default, all feature properties with a value are
 *     returned.
 * @langDe Todo
 * @name properties
 * @endpoints Tile
 */
@Singleton
@AutoBind
public class QueryParameterProperties extends ApiExtensionCache
    implements OgcApiQueryParameter,
        TypedQueryParameter<List<String>>,
        FeatureQueryTransformer,
        TileGenerationUserParameter {

  private final SchemaInfo schemaInfo;
  private final SchemaValidator schemaValidator;

  @Inject
  public QueryParameterProperties(SchemaInfo schemaInfo, SchemaValidator schemaValidator) {
    this.schemaInfo = schemaInfo;
    this.schemaValidator = schemaValidator;
  }

  @Override
  public String getId(String collectionId) {
    return "properties_" + collectionId;
  }

  @Override
  public String getName() {
    return "properties";
  }

  @Override
  public String getDescription() {
    return "The properties that should be included for each feature. The parameter value is a comma-separated list of property names. By default, all feature properties with a value are returned.";
  }

  @Override
  public boolean isApplicable(OgcApiDataV2 apiData, String definitionPath, HttpMethods method) {
    return computeIfAbsent(
        this.getClass().getCanonicalName() + apiData.hashCode() + definitionPath + method.name(),
        () ->
            isEnabledForApi(apiData)
                && method == HttpMethods.GET
                && (definitionPath.equals("/collections/{collectionId}/items")
                    || definitionPath.equals("/collections/{collectionId}/items/{featureId}")
                    || definitionPath.equals(
                        "/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
                    || definitionPath.equals(
                        "/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")));
  }

  private ConcurrentMap<Integer, ConcurrentMap<String, Schema<?>>> schemaMap =
      new ConcurrentHashMap<>();

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData) {
    int apiHashCode = apiData.hashCode();
    if (!schemaMap.containsKey(apiHashCode)) schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
    if (!schemaMap.get(apiHashCode).containsKey("*")) {
      schemaMap.get(apiHashCode).put("*", new ArraySchema().items(new StringSchema()));
    }
    return schemaMap.get(apiHashCode).get("*");
  }

  @Override
  public Schema<?> getSchema(OgcApiDataV2 apiData, String collectionId) {
    int apiHashCode = apiData.hashCode();
    if (!schemaMap.containsKey(apiHashCode)) schemaMap.put(apiHashCode, new ConcurrentHashMap<>());
    if (!schemaMap.get(apiHashCode).containsKey(collectionId)) {
      schemaMap
          .get(apiHashCode)
          .put(
              collectionId,
              new ArraySchema()
                  .items(
                      new StringSchema()
                          ._enum(schemaInfo.getPropertyNames(apiData, collectionId))));
    }
    return schemaMap.get(apiHashCode).get(collectionId);
  }

  @Override
  public SchemaValidator getSchemaValidator() {
    return schemaValidator;
  }

  @Override
  public Class<? extends ExtensionConfiguration> getBuildingBlockConfigurationType() {
    return ProjectionsConfiguration.class;
  }

  @Override
  public List<String> parse(String value, OgcApiDataV2 apiData) {
    try {
      return Splitter.on(',').omitEmptyStrings().trimResults().splitToList(value);
    } catch (Throwable e) {
      throw new IllegalArgumentException(
          String.format("Invalid value for query parameter '%s'.", getName()), e);
    }
  }

  @Override
  public ImmutableFeatureQuery.Builder transformQuery(
      ImmutableFeatureQuery.Builder queryBuilder,
      Map<String, String> parameters,
      OgcApiDataV2 datasetData,
      FeatureTypeConfigurationOgcApi featureTypeConfiguration) {
    List<String> propertiesList = getPropertiesList(parameters);

    return queryBuilder.fields(propertiesList);
  }

  @Override
  public void applyTo(
      ImmutableTileGenerationParametersTransient.Builder userParametersBuilder,
      QueryParameterSet parameters,
      Optional<TileGenerationSchema> generationSchema) {
    parameters.getValue(this).ifPresent(userParametersBuilder::fields);
  }

  private List<String> getPropertiesList(Map<String, String> parameters) {
    if (parameters.containsKey(getName())) {
      return Splitter.on(',')
          .omitEmptyStrings()
          .trimResults()
          .splitToList(parameters.get(getName()));
    } else {
      return ImmutableList.of("*");
    }
  }
}
