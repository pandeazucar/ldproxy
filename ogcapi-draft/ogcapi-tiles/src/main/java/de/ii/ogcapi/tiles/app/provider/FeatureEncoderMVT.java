/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app.provider;

import de.ii.ogcapi.features.core.domain.FeatureEncoderSfFlat;
import de.ii.ogcapi.features.core.domain.FeatureSfFlat;
import de.ii.ogcapi.tiles.domain.ImmutableMvtFeature;
import de.ii.ogcapi.tiles.domain.MvtFeature;
import de.ii.ogcapi.tiles.domain.provider.LevelTransformation;
import de.ii.ogcapi.tiles.domain.provider.TileCoordinates;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationContext;
import de.ii.ogcapi.tiles.domain.provider.TileGenerationOptions;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.core.MediaType;
import no.ecc.vectortile.VectorTileEncoder;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureEncoderMVT extends FeatureEncoderSfFlat {

  private static final Logger LOGGER = LoggerFactory.getLogger(FeatureEncoderMVT.class);
  public static final MediaType FORMAT = new MediaType("application", "vnd.mapbox-vector-tile");

  private final TileGenerationOptions parameters;
  private final TileCoordinates tile;
  private final VectorTileEncoder tileEncoder;
  private final AffineTransformation affineTransformation;
  private final String layerName;
  private final PrecisionModel tilePrecisionModel;
  private final GeometryFactory geometryFactoryTile;
  private final GeometryFactory geometryFactoryWorld;
  private final Polygon clipGeometry;
  private final List<String> groupBy;
  private final Set<MvtFeature> mergeFeatures;

  private long mergeCount = 0;
  private long featureCount = 0;
  private boolean full = true;

  // TODO: TilesConfiguration not available in xtraplatform, but new TileProviderData
  public FeatureEncoderMVT(TileGenerationContext encodingContext) {
    super(encodingContext);
    this.parameters = encodingContext.getParameters();
    this.tile = encodingContext.getCoordinates();
    this.tileEncoder = new VectorTileEncoder(tile.getTileMatrixSet().getTileExtent());
    this.affineTransformation = createTransformNativeToTile();
    this.layerName = Objects.requireNonNullElse(collectionId, "layer");
    this.tilePrecisionModel =
        new PrecisionModel(
            (double) tile.getTileMatrixSet().getTileExtent()
                / (double) tile.getTileMatrixSet().getTileSize());
    this.geometryFactoryTile = new GeometryFactory(tilePrecisionModel);
    this.geometryFactoryWorld = new GeometryFactory();

    final int size = tile.getTileMatrixSet().getTileSize();
    final int buffer = 8;
    CoordinateXY[] coords = new CoordinateXY[5];
    coords[0] = new CoordinateXY(-buffer, size + buffer);
    coords[1] = new CoordinateXY(size + buffer, size + buffer);
    coords[2] = new CoordinateXY(size + buffer, -buffer);
    coords[3] = new CoordinateXY(-buffer, -buffer);
    coords[4] = coords[0];
    this.clipGeometry = geometryFactoryTile.createPolygon(coords);

    final Map<String, List<LevelTransformation>> rules = parameters.getTransformations();
    this.groupBy =
        (Objects.nonNull(rules) && rules.containsKey(tile.getTileMatrixSet().getId()))
            ? rules.get(tile.getTileMatrixSet().getId()).stream()
                .filter(
                    rule ->
                        rule.getMax() >= tile.getLevel()
                            && rule.getMin() <= tile.getLevel()
                            && rule.getMerge().orElse(false))
                .map(LevelTransformation::getGroupBy)
                .findAny()
                .orElse(null)
            : null;
    this.mergeFeatures = new HashSet<>();
  }

  @Override
  public void onStart(ModifiableContext context) {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(
          "Start generating tile for collection {}, tile {}/{}/{}/{}.",
          collectionId,
          tile.getTileMatrixSet().getId(),
          tile.getLevel(),
          tile.getRow(),
          tile.getCol());
    }
    this.processingStart = System.nanoTime();
  }

  @Override
  public void onFeature(FeatureSfFlat feature) {
    long startFeature = System.nanoTime();
    featureCount++;

    Optional<Geometry> featureGeometry = feature.getJtsGeometry(geometryFactoryWorld);

    if (featureGeometry.isEmpty()) {
      return;
    }

    try {
      Geometry tileGeometry =
          TileGeometryUtil.getTileGeometry(
              featureGeometry.get(),
              affineTransformation,
              clipGeometry,
              tilePrecisionModel,
              parameters.getMinimumSizeInPixel());
      if (Objects.isNull(tileGeometry)) {
        return;
      }

      // in "full" tiles all features cover then whole tile
      full = full && tileGeometry.equals(clipGeometry);

      // if polygons have to be merged, store them for now and process at the end
      if (Objects.nonNull(groupBy) && tileGeometry.getGeometryType().contains("Polygon")) {
        mergeFeatures.add(
            new ImmutableMvtFeature.Builder()
                .id(++mergeCount)
                .properties(feature.getPropertiesAsMap())
                .geometry(tileGeometry)
                .build());
        return;
      }

      // Geometry is invalid -> log this information and skip it, if that option is used
      if (!tileGeometry.isValid()) {
        LOGGER.warn(
            "Feature {} in collection {} has an invalid tile geometry in tile {}/{}/{}/{}. Size in pixels: {}.",
            feature.getIdValue(),
            collectionId,
            tile.getTileMatrixSet().getId(),
            tile.getLevel(),
            tile.getRow(),
            tile.getCol(),
            featureGeometry.get().getArea());
        if (parameters.getIgnoreInvalidGeometries()) {
          return;
        }
      }

      // If we have an id that happens to be a long value, use it
      Long id = null;
      if (feature.getIdValue() != null) {
        try {
          id = Long.parseLong(feature.getIdValue());
        } catch (Exception e) {
          // nothing to do
        }
      }

      // Add the feature with the layer name, a Map with attributes and the JTS Geometry.
      if (Objects.nonNull(id)) {
        tileEncoder.addFeature(layerName, feature.getPropertiesAsMap(), tileGeometry, id);
      } else {
        tileEncoder.addFeature(layerName, feature.getPropertiesAsMap(), tileGeometry);
      }
      written++;

    } catch (Exception e) {
      LOGGER.error(
          "Error while processing feature {} in tile {}/{}/{}/{} in collection {}. The feature is skipped.",
          feature.getIdValue(),
          tile.getTileMatrixSet().getId(),
          tile.getLevel(),
          tile.getRow(),
          tile.getCol(),
          collectionId);
      if (LOGGER.isDebugEnabled(LogContext.MARKER.STACKTRACE)) {
        LOGGER.debug(LogContext.MARKER.STACKTRACE, "Stacktrace:", e);
      }
    }
    featureDuration += System.nanoTime() - startFeature;
  }

  @Override
  public void onEnd(ModifiableContext context) {
    long mergerStart = System.nanoTime();
    if (Objects.nonNull(groupBy) && mergeCount > 0) {
      FeatureMerger merger =
          new FeatureMerger(
              groupBy,
              allProperties,
              properties,
              geometryFactoryTile,
              tilePrecisionModel,
              String.format(
                  "Collection %s, tile %s/%d/%d/%d",
                  collectionId,
                  tile.getTileMatrixSet().getId(),
                  tile.getLevel(),
                  tile.getRow(),
                  tile.getCol()));
      merger
          .merge(mergeFeatures)
          .forEach(
              mergedFeature -> {
                Geometry geom = mergedFeature.getGeometry();
                // Geometry is invalid? -> log this information and skip it, if that option is used
                if (!geom.isValid()) {
                  LOGGER.warn(
                      "A merged feature in collection {} has an invalid tile geometry in tile {}/{}/{}/{}. Properties: {}",
                      collectionId,
                      tile.getTileMatrixSet().getId(),
                      tile.getLevel(),
                      tile.getRow(),
                      tile.getCol(),
                      mergedFeature.getProperties());
                  if (parameters.getIgnoreInvalidGeometries()) return;
                }
                tileEncoder.addFeature(layerName, mergedFeature.getProperties(), geom);
                written++;
              });
    }
    long mergerDuration = (System.nanoTime() - mergerStart) / 1000000;

    long encoderStart = System.nanoTime();

    byte[] mvt = tileEncoder.encode();
    push(mvt);

    if (featureCount == 0) {
      // TODO header/trailer/field "OATiles-hint: empty", also include info in tile cache
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "Collection {}, tile {}/{}/{}/{} is empty.",
            collectionId,
            tile.getTileMatrixSet().getId(),
            tile.getLevel(),
            tile.getRow(),
            tile.getCol());
      }
    } else if (featureCount == written && full) {
      // TODO header/trailer/field "OATiles-hint: full", also include info in tile cache
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "Collection {}, tile {}/{}/{}/{} is full.",
            collectionId,
            tile.getTileMatrixSet().getId(),
            tile.getLevel(),
            tile.getRow(),
            tile.getCol());
      }
    }

    if (LOGGER.isDebugEnabled()) {
      long encoderDuration = (System.nanoTime() - encoderStart) / 1000000;
      long transformerDuration = (System.nanoTime() - transformerStart) / 1000000;
      long processingDuration = (System.nanoTime() - processingStart) / 1000000;
      int kiloBytes = mvt.length / 1024;
      String text =
          String.format(
              "Collection %s, tile %s/%d/%d/%d written. Features returned: %d, written: %d, total duration: %dms, processing: %dms, feature processing: %dms, merging: %dms, encoding: %dms, size: %dkB.",
              collectionId,
              tile.getTileMatrixSet().getId(),
              tile.getLevel(),
              tile.getRow(),
              tile.getCol(),
              context.metadata().getNumberReturned().orElse(0),
              written,
              transformerDuration,
              processingDuration,
              featureDuration / 1000000,
              mergerDuration,
              encoderDuration,
              kiloBytes);

      if (processingDuration > 1000 || kiloBytes > 250) {
        LOGGER.debug(text);
      } else if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(text);
      }
    }
  }

  private AffineTransformation createTransformNativeToTile() {

    BoundingBox bbox = tile.getBoundingBox();

    double xMin = bbox.getXmin();
    double xMax = bbox.getXmax();
    double yMin = bbox.getYmin();
    double yMax = bbox.getYmax();

    double tileSize = tile.getTileMatrixSet().getTileSize();

    double xScale = tileSize / (xMax - xMin);
    double yScale = tileSize / (yMax - yMin);

    double xOffset = -xMin * xScale;
    double yOffset = yMin * yScale + tileSize;

    return new AffineTransformation(xScale, 0.0d, xOffset, 0.0d, -yScale, yOffset);
  }
}
