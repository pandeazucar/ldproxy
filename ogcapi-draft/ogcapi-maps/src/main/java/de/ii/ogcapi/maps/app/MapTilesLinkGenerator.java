/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.maps.app;

import com.google.common.collect.ImmutableList;
import de.ii.ogcapi.foundation.domain.DefaultLinksGenerator;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.foundation.domain.ImmutableLink;
import de.ii.ogcapi.foundation.domain.Link;
import de.ii.ogcapi.foundation.domain.URICustomizer;
import de.ii.ogcapi.tiles.domain.TileSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** This class is responsible for generating the links in the json Files. */
public class MapTilesLinkGenerator extends DefaultLinksGenerator {

  /**
   * generates the Links on the landing page /{apiId}/
   *
   * @param uriBuilder the URI, split in host, path and query
   * @param i18n module to get linguistic text
   * @param language the requested language (optional)
   * @return a List with links
   */
  public List<Link> generateLandingPageLinks(
      URICustomizer uriBuilder, TileSet.DataType dataType, I18n i18n, Optional<Locale> language) {

    return ImmutableList.<Link>builder()
        .add(
            new ImmutableLink.Builder()
                .href(
                    uriBuilder
                        .copy()
                        .removeLastPathSegment("collections")
                        .ensureNoTrailingSlash()
                        .ensureLastPathSegments("map", "tiles")
                        .removeParameters("f")
                        .toString())
                .rel("http://www.opengis.net/def/rel/ogc/1.0/tilesets-" + dataType.toString())
                .title(i18n.get("mapTilesLink", language))
                .build())
        .build();
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
      URICustomizer uriBuilder, TileSet.DataType dataType, I18n i18n, Optional<Locale> language) {

    return ImmutableList.<Link>builder()
        .add(
            new ImmutableLink.Builder()
                .href(
                    uriBuilder
                        .copy()
                        .ensureNoTrailingSlash()
                        .ensureLastPathSegments("map", "tiles")
                        .removeParameters("f")
                        .toString())
                .rel("http://www.opengis.net/def/rel/ogc/1.0/tilesets-" + dataType.toString())
                .title(i18n.get("mapTilesLink", language))
                .build())
        .build();
  }
}
