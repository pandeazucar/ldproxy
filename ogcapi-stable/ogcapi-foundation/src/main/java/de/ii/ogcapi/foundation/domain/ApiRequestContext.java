/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.ws.rs.core.Request;
import org.immutables.value.Value;

public interface ApiRequestContext {
  ApiMediaType getMediaType();

  List<ApiMediaType> getAlternateMediaTypes();

  Optional<Locale> getLanguage();

  OgcApi getApi();

  URICustomizer getUriCustomizer();

  String getStaticUrlPrefix();

  Map<String, String> getParameters();

  Optional<Request> getRequest();

  @Value.Default
  default int getMaxResponseLinkHeaderSize() {
    return 2048;
  }

  default String getApiUri() {
    return getUriCustomizer()
        .copy()
        .cutPathAfterSegments(getApi().getData().getSubPath().toArray(new String[0]))
        .clearParameters()
        .toString();
  }
}
