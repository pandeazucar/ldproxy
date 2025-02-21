/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.common.app.html;

import com.google.common.base.Charsets;
import de.ii.ogcapi.common.domain.ConformanceDeclaration;
import de.ii.ogcapi.foundation.domain.I18n;
import de.ii.ogcapi.html.domain.HtmlConfiguration;
import de.ii.ogcapi.html.domain.NavigationDTO;
import de.ii.ogcapi.html.domain.OgcApiView;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class OgcApiConformanceDeclarationView extends OgcApiView {

  private final ConformanceDeclaration conformanceDeclaration;

  public OgcApiConformanceDeclarationView(
      ConformanceDeclaration conformanceDeclaration,
      final List<NavigationDTO> breadCrumbs,
      String urlPrefix,
      HtmlConfiguration htmlConfig,
      boolean noIndex,
      I18n i18n,
      Optional<Locale> language) {
    super(
        "conformanceDeclaration.mustache",
        Charsets.UTF_8,
        null,
        breadCrumbs,
        htmlConfig,
        noIndex,
        urlPrefix,
        conformanceDeclaration.getLinks(),
        conformanceDeclaration.getTitle().orElse(i18n.get("conformanceDeclarationTitle", language)),
        conformanceDeclaration
            .getDescription()
            .orElse(i18n.get("conformanceDeclarationDescription", language)));
    this.conformanceDeclaration = conformanceDeclaration;
  }

  public List<String> getClasses() {
    return conformanceDeclaration.getConformsTo().stream().sorted().collect(Collectors.toList());
  }
}
