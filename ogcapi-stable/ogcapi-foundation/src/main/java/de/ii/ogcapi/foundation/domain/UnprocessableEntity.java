/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.foundation.domain;

public class UnprocessableEntity extends RuntimeException {

  private static final long serialVersionUID = -2187376341627253652L;

  public UnprocessableEntity() {
    super();
  }

  public UnprocessableEntity(String message) {
    super(message);
  }

  public UnprocessableEntity(String message, Throwable cause) {
    super(message, cause);
  }

  public UnprocessableEntity(Throwable cause) {
    super(cause);
  }
}
