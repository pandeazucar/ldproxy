/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.DefaultLinksGenerator;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableLink;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileSet.DataType;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** This class is responsible for generating the links in the json Files. */
public class TilesLinkGenerator extends DefaultLinksGenerator {

  /**
   * generates the Links on the landing page /{apiId}/
   *
   * @param uriBuilder the URI, split in host, path and query
   * @param i18n module to get linguistic text
   * @param language the requested language (optional)
   * @return a List with links
   */
  public List<Link> generateLandingPageLinks(
      URICustomizer uriBuilder, DataType dataType, I18n i18n, Optional<Locale> language) {

    return ImmutableList.<Link>builder()
        .add(
            new ImmutableLink.Builder()
                .href(
                    uriBuilder
                        .copy()
                        .removeLastPathSegment("collections")
                        .ensureNoTrailingSlash()
                        .ensureLastPathSegment("tiles")
                        .removeParameters("f")
                        .toString())
                .rel("http://www.opengis.net/def/rel/ogc/1.0/tilesets-" + dataType.toString())
                .title(i18n.get("tilesLink", language))
                .build())
        .build();
  }

  /**
   * generates the URI templates on the page .../tile
   *
   * @param uriBuilder the URI, split in host, path and query
   * @param i18n module to get linguistic text
   * @param language the requested language (optional)
   * @return a list with links
   */
  public List<Link> generateTileSetsLinks(
      URICustomizer uriBuilder,
      ApiMediaType mediaType,
      List<ApiMediaType> alternateMediaTypes,
      List<TileFormatExtension> tileFormats,
      I18n i18n,
      Optional<Locale> language) {

    final ImmutableList.Builder<Link> builder =
        new ImmutableList.Builder<Link>()
            .addAll(
                super.generateLinks(uriBuilder, mediaType, alternateMediaTypes, i18n, language));

    return builder.build();
  }

  /**
   * generates the URI templates on the page .../tiles/{tileMatrixSetId}
   *
   * @param uriBuilder the URI, split in host, path and query
   * @param collectionId
   * @param i18n module to get linguistic text
   * @param language the requested language (optional)
   * @return a list with links
   */
  public List<Link> generateTileSetLinks(
      URICustomizer uriBuilder,
      ApiMediaType mediaType,
      List<ApiMediaType> alternateMediaTypes,
      String tileMatrixSetId,
      Optional<String> collectionId,
      List<TileFormatExtension> tileFormats,
      I18n i18n,
      Optional<Locale> language) {

    final ImmutableList.Builder<Link> builder =
        new ImmutableList.Builder<Link>()
            .addAll(
                super.generateLinks(uriBuilder, mediaType, alternateMediaTypes, i18n, language));

    builder.add(
        new ImmutableLink.Builder()
            .href(
                uriBuilder
                    .copy()
                    .clearParameters()
                    .ensureNoTrailingSlash()
                    .removeLastPathSegments(collectionId.isPresent() ? 4 : 2)
                    .ensureLastPathSegments("tileMatrixSets", tileMatrixSetId)
                    .toString())
            .rel("http://www.opengis.net/def/rel/ogc/1.0/tiling-scheme")
            .title(i18n.get("tilingSchemeLink", language))
            .build());

    tileFormats.forEach(
        format ->
            builder.add(
                new ImmutableLink.Builder()
                    .href(
                        uriBuilder.copy().clearParameters().ensureNoTrailingSlash().toString()
                            + "/{tileMatrix}/{tileRow}/{tileCol}?f="
                            + format.getMediaType().parameter())
                    .rel("item")
                    .type(format.getMediaType().type().toString())
                    .title(i18n.get("tileLinkTemplate" + format.getMediaType().label(), language))
                    .templated(true)
                    .build()));

    return builder.build();
  }

  /**
   * generates the URI templates on the page .../tiles/{tileMatrixSetId}
   *
   * @param uriBuilder the URI, split in host, path and query
   * @param collectionId
   * @param i18n module to get linguistic text
   * @param language the requested language (optional)
   * @return a list with links
   */
  public List<Link> generateTileSetEmbeddedLinks(
      URICustomizer uriBuilder,
      String tileMatrixSetId,
      Optional<String> collectionId,
      List<TileFormatExtension> tileFormats,
      I18n i18n,
      Optional<Locale> language) {

    final ImmutableList.Builder<Link> builder =
        new ImmutableList.Builder<Link>()
            .add(
                new ImmutableLink.Builder()
                    .href(
                        uriBuilder
                            .copy()
                            .clearParameters()
                            .ensureNoTrailingSlash()
                            .ensureLastPathSegment(tileMatrixSetId)
                            .toString())
                    .rel("self")
                    .title(
                        i18n.get("tileSetLink", language)
                            .replace("{{tileMatrixSetId}}", tileMatrixSetId))
                    .build());

    builder.add(
        new ImmutableLink.Builder()
            .href(
                uriBuilder
                    .copy()
                    .clearParameters()
                    .ensureNoTrailingSlash()
                    .removeLastPathSegments(collectionId.isPresent() ? 3 : 1)
                    .ensureLastPathSegments("tileMatrixSets", tileMatrixSetId)
                    .toString())
            .rel("http://www.opengis.net/def/rel/ogc/1.0/tiling-scheme")
            .title(i18n.get("tilingSchemeLink", language))
            .build());

    tileFormats.forEach(
        format ->
            builder.add(
                new ImmutableLink.Builder()
                    .href(
                        uriBuilder
                                .copy()
                                .clearParameters()
                                .ensureNoTrailingSlash()
                                .ensureLastPathSegment(tileMatrixSetId)
                                .toString()
                            + "/{tileMatrix}/{tileRow}/{tileCol}?f="
                            + format.getMediaType().parameter())
                    .rel("item")
                    .type(format.getMediaType().type().toString())
                    .title(
                        i18n.get("tileLinkTemplate" + format.getMediaType().label(), language)
                            .replace("{{tileMatrixSetId}}", tileMatrixSetId))
                    .templated(true)
                    .build()));

    return builder.build();
  }

  /**
   * generates the links on the page /{apiId}/collections/{collectionId}
   *
   * @param uriBuilder the URI, split in host, path and query
   * @param i18n module to get linguistic text
   * @param language the requested language (optional)
   * @return a list with links
   */
  public List<Link> generateCollectionLinks(
      URICustomizer uriBuilder, DataType dataType, I18n i18n, Optional<Locale> language) {

    return ImmutableList.<Link>builder()
        .add(
            new ImmutableLink.Builder()
                .href(
                    uriBuilder
                        .copy()
                        .ensureNoTrailingSlash()
                        .ensureLastPathSegment("tiles")
                        .removeParameters("f")
                        .toString())
                .rel("http://www.opengis.net/def/rel/ogc/1.0/tilesets-" + dataType.toString())
                .title(i18n.get("tilesLink", language))
                .build())
        .build();
  }
}
