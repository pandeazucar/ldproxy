/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.app;

import com.google.common.collect.ImmutableMap;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.html.domain.ImmutableMapClient;
import de.ii.ogcapi.html.domain.MapClient;
import de.ii.ogcapi.html.domain.MapClient.Popup;
import de.ii.xtraplatform.crs.domain.BoundingBox;
import de.ii.xtraplatform.services.domain.GenericView;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class StyleView extends GenericView {
  public final String title;
  public final String styleUrl;
  public final String layerIds;
  public final Map<String, String> bbox;
  public final MapClient mapClient;
  public final String urlPrefix;

  public StyleView(
      String styleUrl,
      OgcApiDataV2 apiData,
      Optional<BoundingBox> spatialExtent,
      String styleId,
      boolean popup,
      boolean layerControl,
      Map<String, Collection<String>> layerMap,
      String urlPrefix) {
    super("/templates/style", null);
    this.title = "Style " + styleId;
    this.styleUrl = styleUrl;
    this.layerIds =
        "{"
            + layerMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(
                    entry ->
                        "\""
                            + entry.getKey()
                            + "\": [ \""
                            + String.join("\", \"", entry.getValue())
                            + "\" ]")
                .collect(Collectors.joining(", "))
            + "}";

    this.bbox =
        spatialExtent
            .map(
                bbox ->
                    ImmutableMap.of(
                        "minLng", Double.toString(bbox.getXmin()),
                        "minLat", Double.toString(bbox.getYmin()),
                        "maxLng", Double.toString(bbox.getXmax()),
                        "maxLat", Double.toString(bbox.getYmax())))
            .orElse(null);

    this.mapClient =
        new ImmutableMapClient.Builder()
            .styleUrl(styleUrl)
            .popup(popup ? Optional.of(Popup.CLICK_PROPERTIES) : Optional.empty())
            .savePosition(true)
            .layerGroupControl(layerControl ? Optional.of(layerMap.entrySet()) : Optional.empty())
            .build();

    this.urlPrefix = urlPrefix;
  }
}
