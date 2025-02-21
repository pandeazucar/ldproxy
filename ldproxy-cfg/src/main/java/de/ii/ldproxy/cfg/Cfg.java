/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.cfg;

import de.ii.xtraplatform.store.domain.entities.EntityData;
import java.io.IOException;
import java.nio.file.Path;

public interface Cfg {

  Builders builder();

  <T extends EntityData> void writeEntity(T data, Path... patches) throws IOException;

  <T extends EntityData> void writeDefaults(T data, Path... defaults) throws IOException;
}
