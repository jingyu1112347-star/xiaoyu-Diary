/*
 * Copyright 2025 Kyant0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kyant.backdrop.internal

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asSkiaColorFilter
import androidx.compose.ui.graphics.skiaImageFilter
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asSkikoRuntimeShader
import org.jetbrains.skia.ImageFilter

internal actual fun RenderEffect?.chain(other: RenderEffect): RenderEffect {
    return if (this != null) {
        ImageFilter.makeCompose(other.skiaImageFilter, this.skiaImageFilter).asComposeRenderEffect()
    } else {
        other
    }
}

internal actual fun RuntimeShaderEffect(
    runtimeShader: RuntimeShader,
    uniformShaderName: String
): RenderEffect {
    return ImageFilter.makeRuntimeShader(
        runtimeShader.asSkikoRuntimeShader(),
        uniformShaderName,
        null
    ).asComposeRenderEffect()
}

internal actual fun ColorFilterEffect(
    renderEffect: RenderEffect?,
    colorFilter: ColorFilter
): RenderEffect {
    return ImageFilter.makeColorFilter(
        colorFilter.asSkiaColorFilter(),
        renderEffect?.skiaImageFilter,
        null
    ).asComposeRenderEffect()
}
