/*
 * Copyright 2022 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ogcapi.styles.domain

import com.google.common.collect.ImmutableList
import de.ii.ogcapi.foundation.domain.AbstractExtensionConfigurationSpec
import de.ii.ogcapi.foundation.domain.MergeBase
import de.ii.ogcapi.foundation.domain.MergeCollection
import de.ii.ogcapi.foundation.domain.MergeMinimal
import de.ii.ogcapi.foundation.domain.MergeSimple

@SuppressWarnings('ClashingTraitMethods')
class StylesConfigurationSpec extends AbstractExtensionConfigurationSpec implements MergeBase<StylesConfiguration>, MergeMinimal<StylesConfiguration>, MergeSimple<StylesConfiguration>, MergeCollection<StylesConfiguration> {
    @Override
    StylesConfiguration getFull() {
        return new ImmutableStylesConfiguration.Builder()
                .enabled(true)
                .managerEnabled(true)
                .validationEnabled(true)
                .webmapWithPopup(true)
                .webmapWithLayerControl(true)
                .layerControlAllLayers(true)
                .defaultStyle("foo")
                .addStyleEncodings("foo", "bar")
                .build()
    }

    @Override
    StylesConfiguration getMinimal() {
        return new ImmutableStylesConfiguration.Builder()
                .build()
    }

    @Override
    StylesConfiguration getMinimalFullMerged() {
        return getFull()
    }

    @Override
    StylesConfiguration getSimple() {
        return new ImmutableStylesConfiguration.Builder()
                .enabled(false)
                .managerEnabled(false)
                .validationEnabled(false)
                .webmapWithPopup(false)
                .webmapWithLayerControl(false)
                .layerControlAllLayers(false)
                .defaultStyle("bar")
                .build()
    }

    @Override
    StylesConfiguration getSimpleFullMerged() {
        return new ImmutableStylesConfiguration.Builder()
                .from(getFull())
                .from(getSimple())
                .build()
    }

    @Override
    StylesConfiguration getCollection() {
        return new ImmutableStylesConfiguration.Builder()
                .addStyleEncodings("bar")
                .build()
    }

    @Override
    StylesConfiguration getCollectionFullMerged() {
        return new ImmutableStylesConfiguration.Builder()
                .from(getFull())
                .styleEncodings(ImmutableList.of("foo", "bar"))
                .build()
    }
}
