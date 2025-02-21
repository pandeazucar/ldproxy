/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.http.client.utils.URIBuilder;

public class DefaultLinksGenerator {

  public List<Link> generateLinks(
      URICustomizer uriBuilder,
      ApiMediaType mediaType,
      List<ApiMediaType> alternateMediaTypes,
      I18n i18n,
      Optional<Locale> language) {
    uriBuilder.removeParameters("lang").ensureNoTrailingSlash();

    final ImmutableList.Builder<Link> builder =
        new ImmutableList.Builder<Link>()
            .add(
                new ImmutableLink.Builder()
                    .href(
                        alternateMediaTypes.isEmpty()
                            ? uriBuilder.toString()
                            : uriBuilder.copy().setParameter("f", mediaType.parameter()).toString())
                    .rel("self")
                    .type(mediaType.type().toString())
                    .title(i18n.get("selfLink", language))
                    .build())
            .addAll(
                alternateMediaTypes.stream()
                    .map(generateAlternateLink(uriBuilder.copy(), i18n, language))
                    .collect(Collectors.toList()));

    return builder.build();
  }

  private Function<ApiMediaType, Link> generateAlternateLink(
      final URIBuilder uriBuilder, I18n i18n, Optional<Locale> language) {
    return mediaType ->
        new ImmutableLink.Builder()
            .href(uriBuilder.setParameter("f", mediaType.parameter()).toString())
            .rel("alternate")
            .type(mediaType.type().toString())
            .title(i18n.get("alternateLink", language) + " " + mediaType.label())
            .build();
  }
}
