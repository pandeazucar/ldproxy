/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.tiles.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.foundation.domain.ApiMediaType;
import de.ii.ogcapi.foundation.domain.ApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaType;
import de.ii.ogcapi.foundation.domain.ImmutableApiMediaTypeContent;
import de.ii.ogcapi.foundation.domain.OgcApiDataV2;
import de.ii.ogcapi.tiles.domain.TileFormatExtension;
import de.ii.ogcapi.tiles.domain.TileSet;
import de.ii.ogcapi.tiles.domain.TileSet.DataType;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;

@Singleton
@AutoBind
public class TileFormatWEBP extends TileFormatExtension {

  public static final ApiMediaType MEDIA_TYPE =
      new ImmutableApiMediaType.Builder()
          .type(new MediaType("image", "webp"))
          .label("WebP")
          .parameter("webp")
          .build();

  @Inject
  TileFormatWEBP() {}

  @Override
  public ApiMediaType getMediaType() {
    return MEDIA_TYPE;
  }

  @Override
  public ApiMediaTypeContent getContent(OgcApiDataV2 apiData, String path) {
    if (path.equals("/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}")
        || path.equals(
            "/collections/{collectionId}/tiles/{tileMatrixSetId}/{tileMatrix}/{tileRow}/{tileCol}"))
      return new ImmutableApiMediaTypeContent.Builder()
          .schema(SCHEMA_TILE)
          .schemaRef(SCHEMA_REF_TILE)
          .ogcApiMediaType(MEDIA_TYPE)
          .build();

    return null;
  }

  @Override
  public String getExtension() {
    return "webp";
  }

  @Override
  public DataType getDataType() {
    return TileSet.DataType.map;
  }
}
