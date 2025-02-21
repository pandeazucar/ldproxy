/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import com.google.common.base.Splitter;
import de.ii.ogcapi.foundation.domain.OgcApi;
import de.ii.ogcapi.tilematrixsets.domain.TileMatrixSetRepository;
import de.ii.ogcapi.tiles.domain.TilesProviders;
import de.ii.xtraplatform.base.domain.AppConfiguration;
import de.ii.xtraplatform.base.domain.LogContext;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.crs.domain.OgcCrs;
import de.ii.xtraplatform.store.domain.entities.EntityRegistry;
import de.ii.xtraplatform.web.domain.DropwizardPlugin;
import io.dropwizard.servlets.tasks.Task;
import io.dropwizard.setup.Environment;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @author zahnen
 */
@Singleton
@AutoBind
public class PurgeTileCacheTask extends Task implements DropwizardPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(PurgeTileCacheTask.class);
  private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private final EntityRegistry entityRegistry;
  private final TileMatrixSetRepository tileMatrixSetRepository;
  private final TilesProviders tilesProviders;

  @Inject
  protected PurgeTileCacheTask(
      EntityRegistry entityRegistry,
      TileMatrixSetRepository tileMatrixSetRepository,
      TilesProviders tilesProviders) {
    super("purge-tile-cache");
    this.entityRegistry = entityRegistry;
    this.tileMatrixSetRepository = tileMatrixSetRepository;
    this.tilesProviders = tilesProviders;
  }

  @Override
  public void init(AppConfiguration configuration, Environment environment) {
    environment.admin().addTask(this);
  }

  @Override
  public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("Purge tile cache request: {}", parameters);
    }

    Optional<String> apiId = getId(parameters);

    if (apiId.isEmpty()) {
      output.println("No api id given");
      output.flush();
      return;
    }

    Optional<OgcApi> ogcApi = entityRegistry.getEntity(OgcApi.class, apiId.get());

    if (ogcApi.isEmpty()) {
      output.println("No api with the given id found");
      output.flush();
      return;
    }

    Optional<String> collectionId = getCollectionId(parameters);

    if (collectionId.isPresent()
        && !ogcApi.get().getData().isCollectionEnabled(collectionId.get())) {
      output.println("No collection with the given id found");
      output.flush();
      return;
    }

    Optional<String> tileMatrixSetId = getTileMatrixSetId(parameters);

    if (tileMatrixSetId.isPresent()
        && tileMatrixSetRepository.get(tileMatrixSetId.get()).isEmpty()) {
      output.println("No tile matrix set with the given id found");
      output.flush();
      return;
    }

    List<String> bbox = getBBox(parameters);

    if (bbox.size() > 0 && bbox.size() != 4) {
      output.println("Invalid bbox given");
      output.flush();
      return;
    }

    Optional<BoundingBox> boundingBox =
        bbox.isEmpty()
            ? Optional.empty()
            : Optional.of(
                BoundingBox.of(
                    Double.parseDouble(bbox.get(0)),
                    Double.parseDouble(bbox.get(1)),
                    Double.parseDouble(bbox.get(2)),
                    Double.parseDouble(bbox.get(3)),
                    OgcCrs.CRS84));

    try (MDC.MDCCloseable closeable =
        LogContext.putCloseable(LogContext.CONTEXT.SERVICE, apiId.get())) {
      tilesProviders.deleteTiles(ogcApi.get(), collectionId, tileMatrixSetId, boundingBox);
    }
  }

  private Optional<String> getId(Map<String, List<String>> parameters) {
    return getValueList(parameters.get("api")).findFirst();
  }

  private Optional<String> getCollectionId(Map<String, List<String>> parameters) {
    return getValueList(parameters.get("collection")).findFirst();
  }

  private Optional<String> getTileMatrixSetId(Map<String, List<String>> parameters) {
    return getValueList(parameters.get("tileMatrixSet")).findFirst();
  }

  private List<String> getBBox(Map<String, List<String>> parameters) {
    return getValueList(parameters.get("bbox")).collect(Collectors.toList());
  }

  private Stream<String> getValueList(Collection<String> values) {
    if (Objects.isNull(values)) {
      return Stream.empty();
    }
    return values.stream()
        .flatMap(
            value -> {
              if (value.contains(",")) {
                return SPLITTER.splitToList(value).stream();
              }
              return Stream.of(value);
            });
  }
}
