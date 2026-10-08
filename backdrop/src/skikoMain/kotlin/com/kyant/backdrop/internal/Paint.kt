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

import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.skiaPaint
import com.kyant.backdrop.RuntimeShader
import com.kyant.backdrop.asSkikoRuntimeShader
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.MaskFilter

internal actual fun Paint.blur(radius: Float) {
    this.skiaPaint.maskFilter =
        if (radius > 0f) MaskFilter.makeBlur(FilterBlurMode.NORMAL, radius)
        else null
}

internal actual fun Paint.setRuntimeShader(runtimeShader: RuntimeShader?) {
    this.skiaPaint.shader = runtimeShader?.asSkikoRuntimeShader()?.makeShader()
}
