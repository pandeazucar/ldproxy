/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.features.jsonfg.app;

import com.github.azahnen.dagger.annotations.AutoBind;
import de.ii.ogcapi.features.jsonfg.domain.ImmutableJsonFgConfiguration.Builder;
import de.ii.ogcapi.foundation.domain.ApiBuildingBlock;
import de.ii.ogcapi.foundation.domain.ExtensionConfiguration;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @title Features JSON-FG
 * @langEn The Features JSON-FG module can be enabled for any API provided through ldproxy with a
 *     feature provider. It enables the provisioning of the Features and Feature resources in
 *     JSON-FG.
 * @langDe Das Modul *Features JSON-FG* kann für jede über ldproxy bereitgestellte API mit einem
 *     Feature-Provider aktiviert werden. Es aktiviert die Bereitstellung der Ressourcen Features
 *     und Feature in JSON-FG.
 * @conformanceEn The module is based on the [drafts for
 *     JSON-FG](https://github.com/opengeospatial/ogc-feat-geo-json). The implementation will change
 *     as the draft is further standardized. *
 * @conformanceDe Das Modul basiert auf den [Entwürfen für
 *     JSON-FG](https://github.com/opengeospatial/ogc-feat-geo-json). Die Implementierung wird sich
 *     im Zuge der weiteren Standardisierung des Entwurfs noch ändern.
 * @example {@link de.ii.ogcapi.features.jsonfg.domain.JsonFgConfiguration}
 * @propertyTable {@link de.ii.ogcapi.features.jsonfg.domain.ImmutableJsonFgConfiguration}
 */
@Singleton
@AutoBind
public class JsonFgBuildingBlock implements ApiBuildingBlock {

  @Inject
  public JsonFgBuildingBlock() {}

  @Override
  public ExtensionConfiguration getDefaultConfiguration() {
    return new Builder()
        .enabled(false)
        .describedby(true)
        .coordRefSys(true)
        .geojsonCompatibility(true)
        .build();
  }
}
