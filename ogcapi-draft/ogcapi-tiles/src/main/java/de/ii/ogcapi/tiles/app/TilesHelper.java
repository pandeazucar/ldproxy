/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.features.core.domain.FeaturesCoreConfiguration;
import de.ii.ogcapi.features.core.domain.FeaturesCoreProviders;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaBoolean;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaNumber;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaObject;
import de.ii.ogcapi.features.core.domain.ImmutableJsonSchemaString;
import de.ii.ogcapi.features.core.domain.JsonSchema;
import de.ii.ogcapi.features.core.domain.JsonSchemaCache;
import de.ii.ogcapi.features.core.domain.JsonSchemaDocument;
import de.ii.ogcapi.features.core.domain.JsonSchemaObject;
import de.ii.ogcapi.features.core.domain.SchemaInfo;
import de.ii.ogcapi.features.geojson.domain.GeoJsonConfiguration;
import de.ii.ogcapi.foundation.domain.FeatureTypeConfigurationOgcApi;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.tilematrixsets.domain.ImmutableTilesBoundingBox;
import de.ii.ogcapi.tilematrixsets.domain.MinMax;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSet;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimits;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetLimitsGenerator;
import de.ii.ogcapi.tilematrixsets.domain.TilesBoundingBox;
import de.ii.ogcapi.tiles.domain.ImmutableTileLayer;
import de.ii.ogcapi.tiles.domain.ImmutableTilePoint;
import de.ii.ogcapi.tiles.domain.ImmutableTileSet;
import de.ii.ogcapi.tiles.domain.ImmutableTileSet.Builder;
import de.ii.ogcapi.tiles.domain.ImmutableVectorLayer;
import de.ii.ogcapi.tiles.domain.TileLayer;
import de.ii.ogcapi.tiles.domain.TilePoint;
import de.ii.ogcapi.tiles.domain.TileProviderFeatures;
import de.ii.ogcapi.tiles.domain.TileProviderMbtiles;
import de.ii.ogcapi.tiles.domain.TileSet;
import de.ii.ogcapi.tiles.domain.TilesConfiguration;
import de.ii.ogcapi.tiles.domain.VectorLayer;
import de.ii.xtraplatform.codelists.domain.Codelist;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.CrsTransformationException;
import de.ii.xtraplatform.crs.domain.CrsTransformerFactory;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.features.domain.FeatureSchema;
import de.ii.xtraplatform.features.domain.SchemaBase;
import de.ii.xtraplatform.geometries.domain.SimpleFeatureGeometry;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TilesHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(TilesHelper.class);

  // TODO: move to TileSet as static of()
  /**
   * generate the tile set metadata according to the OGC Tile Matrix Set standard (version 2.0.0,
   * draft from June 2021)
   *
   * @param api the API
   * @param tileMatrixSet the tile matrix set
   * @param zoomLevels the range of zoom levels
   * @param center the center point
   * @param collectionId the collection, empty = all collections in the dataset
   * @param dataType vector, map or coverage
   * @param links links to include in the object
   * @param uriCustomizer optional URI of the resource
   * @param limitsGenerator helper to generate the limits for each zoom level based on the bbox of
   *     the data
   * @param providers helper to access feature providers
   * @return the tile set metadata
   */
  public static TileSet buildTileSet(
      OgcApi api,
      TileMatrixSet tileMatrixSet,
      MinMax zoomLevels,
      List<Double> center,
      Optional<String> collectionId,
      TileSet.DataType dataType,
      List<Link> links,
      Optional<URICustomizer> uriCustomizer,
      CrsTransformerFactory crsTransformerFactory,
      TileMatrixSetLimitsGenerator limitsGenerator,
      FeaturesCoreProviders providers,
      EntityRegistry entityRegistry) {
    OgcApiDataV2 apiData = api.getData();
    Builder builder = ImmutableTileSet.builder().dataType(dataType);

    builder.tileMatrixSetId(tileMatrixSet.getId());

    if (tileMatrixSet.getURI().isPresent())
      builder.tileMatrixSetURI(tileMatrixSet.getURI().get().toString());
    else builder.tileMatrixSet(tileMatrixSet.getTileMatrixSetData());

    if (Objects.isNull(zoomLevels)) builder.tileMatrixSetLimits(ImmutableList.of());
    else
      builder.tileMatrixSetLimits(
          limitsGenerator.getTileMatrixSetLimits(api, tileMatrixSet, zoomLevels, collectionId));

    try {
      BoundingBox boundingBox =
          // TODO get information from TileProviderData, instead of TilesConfiguration
          (collectionId.isPresent()
                  ? apiData.getExtension(TilesConfiguration.class, collectionId.get())
                  : apiData.getExtension(TilesConfiguration.class))
              .map(TilesConfiguration::getTileProvider)
              .filter(c -> c instanceof TileProviderMbtiles)
              // if present, the bbox is in CRS84
              .flatMap(c -> ((TileProviderMbtiles) c).getBounds())
              .orElse(
                  api.getSpatialExtent(collectionId)
                      .orElse(tileMatrixSet.getBoundingBoxCrs84(crsTransformerFactory)));
      builder.boundingBox(
          ImmutableTilesBoundingBox.builder()
              .lowerLeft(
                  BigDecimal.valueOf(boundingBox.getXmin()).setScale(7, RoundingMode.HALF_UP),
                  BigDecimal.valueOf(boundingBox.getYmin()).setScale(7, RoundingMode.HALF_UP))
              .upperRight(
                  BigDecimal.valueOf(boundingBox.getXmax()).setScale(7, RoundingMode.HALF_UP),
                  BigDecimal.valueOf(boundingBox.getYmax()).setScale(7, RoundingMode.HALF_UP))
              .crs(OgcCrs.CRS84.toUriString())
              .build());
    } catch (CrsTransformationException e) {
      builder.boundingBox(
          ImmutableTilesBoundingBox.builder()
              .lowerLeft(BigDecimal.valueOf(-180), BigDecimal.valueOf(-90))
              .upperRight(BigDecimal.valueOf(180), BigDecimal.valueOf(90))
              .crs(OgcCrs.CRS84.toUriString())
              .build());
    }

    if ((Objects.nonNull(zoomLevels) && zoomLevels.getDefault().isPresent()) || !center.isEmpty()) {
      ImmutableTilePoint.Builder builder2 = new ImmutableTilePoint.Builder();
      if (Objects.nonNull(zoomLevels))
        zoomLevels.getDefault().ifPresent(def -> builder2.tileMatrix(String.valueOf(def)));
      if (!center.isEmpty()) builder2.coordinates(center);
      builder.centerPoint(builder2.build());
    }

    // TODO get information from TileProviderData, instead of TilesConfiguration
    (collectionId.isPresent()
            ? apiData.getExtension(TilesConfiguration.class, collectionId.get())
            : apiData.getExtension(TilesConfiguration.class))
        .map(TilesConfiguration::getTileProvider)
        .ifPresent(
            tileProvider -> {
              if (tileProvider instanceof TileProviderFeatures) {
                // prepare a map with the JSON schemas of the feature collections used in the style
                JsonSchemaCache schemas =
                    new SchemaCacheTileSet(() -> entityRegistry.getEntitiesForType(Codelist.class));

                Map<String, JsonSchemaDocument> schemaMap =
                    collectionId.isPresent()
                        ? apiData
                            .getCollectionData(collectionId.get())
                            .filter(
                                collectionData -> {
                                  Optional<TilesConfiguration> config =
                                      collectionData.getExtension(TilesConfiguration.class);
                                  return collectionData.getEnabled()
                                      && config.isPresent()
                                      && config.get().isEnabled();
                                })
                            .map(
                                collectionData -> {
                                  Optional<FeatureSchema> schema =
                                      providers.getFeatureSchema(apiData, collectionData);
                                  if (schema.isPresent())
                                    return ImmutableMap.of(
                                        collectionId.get(),
                                        schemas.getSchema(
                                            schema.get(),
                                            apiData,
                                            collectionData,
                                            Optional.empty()));
                                  return null;
                                })
                            .filter(Objects::nonNull)
                            .orElse(ImmutableMap.of())
                        : apiData.getCollections().entrySet().stream()
                            .filter(
                                entry -> {
                                  Optional<TilesConfiguration> config =
                                      entry.getValue().getExtension(TilesConfiguration.class);
                                  return entry.getValue().getEnabled()
                                      && config.isPresent()
                                      && config.get().isMultiCollectionEnabled();
                                })
                            .map(
                                entry -> {
                                  Optional<FeatureSchema> schema =
                                      providers.getFeatureSchema(apiData, entry.getValue());
                                  if (schema.isPresent())
                                    return new AbstractMap.SimpleImmutableEntry<>(
                                        entry.getKey(),
                                        schemas.getSchema(
                                            schema.get(),
                                            apiData,
                                            entry.getValue(),
                                            Optional.empty()));
                                  return null;
                                })
                            .filter(Objects::nonNull)
                            .collect(
                                ImmutableMap.toImmutableMap(
                                    Map.Entry::getKey, Map.Entry::getValue));

                // TODO: replace with SchemaDeriverTileLayers
                schemaMap.entrySet().stream()
                    .forEach(
                        entry -> {
                          String collectionId2 = entry.getKey();
                          FeatureTypeConfigurationOgcApi collectionData =
                              apiData.getCollections().get(collectionId2);

                          JsonSchemaDocument schema = entry.getValue();
                          ImmutableTileLayer.Builder builder2 =
                              ImmutableTileLayer.builder()
                                  .id(collectionId2)
                                  .title(collectionData.getLabel())
                                  .description(collectionData.getDescription())
                                  .dataType(dataType);

                          collectionData
                              .getExtension(TilesConfiguration.class)
                              .map(
                                  config ->
                                      config.getZoomLevelsDerived().get(tileMatrixSet.getId()))
                              .ifPresent(
                                  minmax ->
                                      builder2
                                          .minTileMatrix(String.valueOf(minmax.getMin()))
                                          .maxTileMatrix(String.valueOf(minmax.getMax())));

                          final JsonSchema geometry = schema.getProperties().get("geometry");
                          if (Objects.nonNull(geometry)) {
                            String geomAsString = geometry.toString();
                            boolean point =
                                geomAsString.contains("GeoJSON Point")
                                    || geomAsString.contains("GeoJSON MultiPoint");
                            boolean line =
                                geomAsString.contains("GeoJSON LineString")
                                    || geomAsString.contains("GeoJSON MultiLineString");
                            boolean polygon =
                                geomAsString.contains("GeoJSON Polygon")
                                    || geomAsString.contains("GeoJSON MultiPolygon");
                            if (point && !line && !polygon)
                              builder2.geometryType(TileLayer.GeometryType.points);
                            else if (!point && line && !polygon)
                              builder2.geometryType(TileLayer.GeometryType.lines);
                            else if (!point && !line && polygon)
                              builder2.geometryType(TileLayer.GeometryType.polygons);
                          }

                          final JsonSchemaObject properties =
                              (JsonSchemaObject) schema.getProperties().get("properties");
                          builder2.propertiesSchema(
                              ImmutableJsonSchemaObject.builder()
                                  .required(properties.getRequired())
                                  .properties(properties.getProperties())
                                  .patternProperties(properties.getPatternProperties())
                                  .build());
                          builder.addLayers(builder2.build());
                        });

              } else if (tileProvider instanceof TileProviderMbtiles) {
                ((TileProviderMbtiles) tileProvider)
                    .getVectorLayers()
                    .forEach(
                        vectorLayer -> {
                          ImmutableTileLayer.Builder builder2 =
                              ImmutableTileLayer.builder()
                                  .id(vectorLayer.getId())
                                  .title(vectorLayer.getId())
                                  .description(vectorLayer.getDescription())
                                  .dataType(dataType);
                          vectorLayer
                              .getMinzoom()
                              .ifPresent(zoom -> builder2.minTileMatrix(String.valueOf(zoom)));
                          vectorLayer
                              .getMaxzoom()
                              .ifPresent(zoom -> builder2.maxTileMatrix(String.valueOf(zoom)));
                          if ("points".equals(vectorLayer.getGeometryType().orElse("")))
                            builder2.geometryType(TileLayer.GeometryType.points);
                          else if ("lines".equals(vectorLayer.getGeometryType().orElse("")))
                            builder2.geometryType(TileLayer.GeometryType.lines);
                          else if ("points".equals(vectorLayer.getGeometryType().orElse("")))
                            builder2.geometryType(TileLayer.GeometryType.polygons);

                          if (!vectorLayer.getFields().isEmpty()) {
                            builder2.propertiesSchema(
                                ImmutableJsonSchemaObject.builder()
                                    .properties(
                                        vectorLayer.getFields().entrySet().stream()
                                            .collect(
                                                Collectors.toUnmodifiableMap(
                                                    entry -> entry.getKey(),
                                                    entry -> {
                                                      if ("number"
                                                          .equalsIgnoreCase(entry.getValue())) {
                                                        return ImmutableJsonSchemaNumber.builder()
                                                            .build();
                                                      } else if ("boolean"
                                                          .equalsIgnoreCase(entry.getValue())) {
                                                        return ImmutableJsonSchemaBoolean.builder()
                                                            .build();
                                                      }
                                                      return ImmutableJsonSchemaString.builder()
                                                          .build();
                                                    })))
                                    .build());
                          }

                          builder.addLayers(builder2.build());
                        });
              }
            });

    builder.links(links);

    return builder.build();
  }

  /**
   * derive the bbox as a sequence left, bottom, right, upper
   *
   * @param tileset the tile set metadata according to the OGC Tile Matrix Set standard
   * @return the bbox
   */
  public static List<Double> getBounds(TileSet tileset) {
    TilesBoundingBox bbox = tileset.getBoundingBox();
    return ImmutableList.of(
        bbox.getLowerLeft()[0].doubleValue(),
        bbox.getLowerLeft()[1].doubleValue(),
        bbox.getUpperRight()[0].doubleValue(),
        bbox.getUpperRight()[1].doubleValue());
  }

  // TODO: move to TileSet as @Value.Lazy
  /**
   * derive the minimum zoom level
   *
   * @param tileset the tile set metadata according to the OGC Tile Matrix Set standard
   * @return the zoom level
   */
  public static Optional<Integer> getMinzoom(TileSet tileset) {
    return tileset.getTileMatrixSetLimits().stream()
        .map(TileMatrixSetLimits::getTileMatrix)
        .map(Integer::valueOf)
        .min(Integer::compareTo);
  }

  // TODO: move to TileSet as @Value.Lazy
  /**
   * derive the maximum zoom level
   *
   * @param tileset the tile set metadata according to the OGC Tile Matrix Set standard
   * @return the zoom level
   */
  public static Optional<Integer> getMaxzoom(TileSet tileset) {
    return tileset.getTileMatrixSetLimits().stream()
        .map(TileMatrixSetLimits::getTileMatrix)
        .map(Integer::valueOf)
        .max(Integer::compareTo);
  }

  // TODO: move to TileSet as @Value.Lazy
  /**
   * derive the default view as longitude, latitude, zoom level
   *
   * @param tileset the tile set metadata according to the OGC Tile Matrix Set standard
   * @return the default view
   */
  public static List<Number> getCenter(TileSet tileset) {
    TilesBoundingBox bbox = tileset.getBoundingBox();
    double centerLon =
        tileset
            .getCenterPoint()
            .map(TilePoint::getCoordinates)
            .filter(coord -> coord.size() >= 2)
            .map(coord -> coord.get(0))
            .orElse(
                bbox.getLowerLeft()[0].doubleValue()
                    + (bbox.getUpperRight()[0].doubleValue() - bbox.getLowerLeft()[0].doubleValue())
                        * 0.5);
    double centerLat =
        tileset
            .getCenterPoint()
            .map(TilePoint::getCoordinates)
            .filter(coord -> coord.size() >= 2)
            .map(coord -> coord.get(1))
            .orElse(
                bbox.getLowerLeft()[1].doubleValue()
                    + (bbox.getUpperRight()[1].doubleValue() - bbox.getLowerLeft()[1].doubleValue())
                        * 0.5);
    int defaultZoomLevel =
        tileset
            .getCenterPoint()
            .map(TilePoint::getTileMatrix)
            .flatMap(level -> level)
            .map(Integer::valueOf)
            .orElse(0);
    return ImmutableList.of(centerLon, centerLat, defaultZoomLevel);
  }

  // TODO: replace with SchemaDeriverVectorLayer and move to VectorLayer as static of()
  /**
   * generate the tile set metadata according to the TileJSON spec
   *
   * @param apiData the API
   * @param collectionId the collection, empty = all collections in the dataset
   * @param tileMatrixSetId the well-known code of the tile matrix set
   * @param providers helper to access feature provide information
   * @param schemaInfo helper to derive the schema information
   * @return the tile set metadata
   */
  public static List<VectorLayer> getVectorLayers(
      OgcApiDataV2 apiData,
      Optional<String> collectionId,
      String tileMatrixSetId,
      FeaturesCoreProviders providers,
      SchemaInfo schemaInfo) {
    Map<String, FeatureTypeConfigurationOgcApi> featureTypesApi = apiData.getCollections();
    return featureTypesApi.values().stream()
        .filter(
            featureTypeApi ->
                collectionId.isEmpty() || featureTypeApi.getId().equals(collectionId.get()))
        .map(
            featureTypeApi -> {
              String featureTypeId =
                  apiData
                      .getExtension(FeaturesCoreConfiguration.class, featureTypeApi.getId())
                      .map(cfg -> cfg.getFeatureType().orElse(featureTypeApi.getId()))
                      .orElse(featureTypeApi.getId());
              Optional<GeoJsonConfiguration> geoJsonConfiguration =
                  featureTypeApi.getExtension(GeoJsonConfiguration.class);
              boolean flatten =
                  geoJsonConfiguration.map(GeoJsonConfiguration::isFlattened).isPresent();
              Optional<FeatureSchema> featureType =
                  providers
                      .getFeatureProvider(apiData, featureTypeApi)
                      .map(provider -> provider.getData().getTypes().get(featureTypeId));
              if (featureType.isEmpty()) return null;
              ImmutableVectorLayer.Builder builder =
                  ImmutableVectorLayer.builder()
                      .id(featureTypeApi.getId())
                      .description(featureTypeApi.getDescription().orElse(""));
              List<FeatureSchema> properties =
                  flatten
                      ? featureType.get().getAllNestedProperties()
                      : featureType.get().getProperties();
              // maps from the dotted path name to the path name with array brackets
              Map<String, String> propertyNameMap =
                  !flatten
                      ? ImmutableMap.of()
                      : schemaInfo
                          .getPropertyNames(apiData, featureTypeApi.getId(), false, true)
                          .stream()
                          .map(
                              name ->
                                  new AbstractMap.SimpleImmutableEntry<>(
                                      name.replace("[]", ""), name))
                          .collect(
                              ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
              AtomicReference<String> geometryType = new AtomicReference<>("unknown");
              properties.forEach(
                  property -> {
                    SchemaBase.Type propType = property.getType();
                    String propertyName =
                        !flatten || property.isObject()
                            ? property.getName()
                            : propertyNameMap.get(String.join(".", property.getFullPath()));
                    if (flatten && propertyName != null) {
                      propertyName = propertyName.replace("[]", ".1");
                    }
                    switch (propType) {
                      case FLOAT:
                      case INTEGER:
                      case STRING:
                      case BOOLEAN:
                      case DATETIME:
                        builder.putFields(propertyName, getType(propType));
                        break;
                      case GEOMETRY:
                        switch (property.getGeometryType().orElse(SimpleFeatureGeometry.ANY)) {
                          case POINT:
                          case MULTI_POINT:
                            geometryType.set("points");
                            break;
                          case LINE_STRING:
                          case MULTI_LINE_STRING:
                            geometryType.set("lines");
                            break;
                          case POLYGON:
                          case MULTI_POLYGON:
                            geometryType.set("polygons");
                            break;
                          case GEOMETRY_COLLECTION:
                          case ANY:
                          case NONE:
                          default:
                            geometryType.set("unknown");
                            break;
                        }
                        break;
                      case OBJECT:
                      case OBJECT_ARRAY:
                      case VALUE_ARRAY:
                      case UNKNOWN:
                      default:
                        // Fallback in MBTiles
                        builder.putFields(propertyName, "String");
                        break;
                    }
                  });
              builder.geometryType(geometryType.get());

              apiData
                  .getExtension(TilesConfiguration.class, featureTypeApi.getId())
                  .map(config -> config.getZoomLevelsDerived().get(tileMatrixSetId))
                  .ifPresent(minmax -> builder.minzoom(minmax.getMin()).maxzoom(minmax.getMax()));
              return builder.build();
            })
        .collect(Collectors.toList());
  }

  /**
   * map the provider types to the TileJSON/Mbtiles types
   *
   * @param type the provider type
   * @return the TileJSON/Mbtiles type
   */
  private static String getType(de.ii.xtraplatform.features.domain.SchemaBase.Type type) {
    switch (type) {
      case INTEGER:
      case FLOAT:
        return "Number";
      case BOOLEAN:
        return "Boolean";
      default:
      case DATETIME:
      case STRING:
        return "String";
    }
  }
}
