package com.xiaoyou.diary.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Choreographer
import android.view.PixelCopy
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/* ------------------------------------------------------------------------------------------------------------------
 * 透明液态玻璃（Liquid Glass / Glassmorphism）
 *
 * 视觉参考：Kyant0/AndroidLiquidGlass（Apache-2.0），其折射思路：
 *   1. 把玻璃后面的内容作为纹理；
 *   2. 用 AGSL 着色器沿圆角 SDF 法线对背景采样坐标做凸透镜位移（refraction）；
 *   3. 叠加边缘高光、顶部反光与极薄 tint。
 *
 * 本项目仍在 Compose UI 1.6（没有 1.7 的 rememberGraphicsLayer），因此背景纹理改用
 * PixelCopy 实时截取【Activity 主窗口】得到。玻璃本体渲染在独立 Popup 窗口（第二个
 * Surface）中，被抓取的主窗口永远不含玻璃自身；双缓冲 Ping-Pong 预渲染、vsync 对齐
 * 原子交换。抓帧在主窗口 OnPreDraw 当场发起（每帧一次、inFlight 背压，跟不上自动丢
 * 最新帧），页面一滚动当帧入队，静止时零抓帧；纹理发布后新/旧画面做 64ms 交叉淡入，
 * 离散抓帧的台阶被抹平为轻微拖影，玻璃内容滑动时连续跟手。
 * 再用原生 RuntimeShader（API 33+）绘制折射。
 * API 26-32 降级为「高透背景 + 高光棱边」（不模糊），API <26 为纯渐变玻璃。
 * ---------------------------------------------------------------------------------------------------------------- */

/** 玻璃形状：圆角胶囊 / 正圆玻璃球 */
enum class GlassShape { PILL, CIRCLE }

/** 玻璃外观参数（refraction* 为像素值；tint 透明度极低，保证高透明而非磨砂） */
class GlassStyle(
    val tintTop: Color,
    val tintMid: Color,
    val tintBottom: Color,
    val rimBright: Color,
    val rimDim: Color,
    val sheen: Color,
    val refractionHeight: Float,
    val refractionAmount: Float,
    val rimWidth: Float
)

/**
 * 玻璃后面的实时画面（Activity 主窗口的截屏）。
 *
 * 双 Surface Ping-Pong 架构：
 *  - Surface-A：Activity 主窗口，只渲染页面内容；
 *  - Surface-B：Dock 所在的独立悬浮窗口（Compose Popup），玻璃、图标、文字全部画在此处。
 * PixelCopy 永远只抓 Surface-A，玻璃自身（连同其图标/文字）物理上不可能进入背景纹理，
 * 因此不需要任何「抓帧闸门」，抓帧过程中 Surface-B 的画面一帧都不变，
 * 白雾自反馈、文字重影、抓帧闪烁从根上消除。
 *
 * Ping-Pong：front 正被 Surface-B 的 GPU 采样时，PixelCopy 把最新画面预渲染进 back；
 * 抓图完成后在最近一个 vsync（Choreographer.doFrame）里一次性交换两个缓冲引用并发布
 * 新 ImageBitmap——胶囊与 FAB 气泡共享同一状态，同一帧原子切换，不会半新半旧。
 * 不支持 PixelCopy 的旧设备 bitmap 为 null，玻璃自动降级为高透渐变。
 */
class WindowBackdrop internal constructor() {
    internal var bitmapState = mutableStateOf<ImageBitmap?>(null, neverEqualPolicy())
    val bitmap: ImageBitmap? get() = bitmapState.value

    // 交叉淡入：新纹理发布后，旧纹理以 blend（1→0）短暂叠加 64ms，
    // 把离散抓帧的台阶感抹平为连续动效，滑动时只留轻微拖影而无跳变
    internal var previousState = mutableStateOf<ImageBitmap?>(null)
    internal var blendState = mutableStateOf(0f)
    val previous: ImageBitmap? get() = previousState.value
    val blend: Float get() = blendState.value

    // 区域抓图：纹理只覆盖屏幕底部一条，此值是该条相对屏幕顶部的 y 偏移（px）。
    // 绘制采样时用它把屏幕绝对坐标换算进纹理；0 表示尚无有效纹理。
    internal var captureTopPx: Int = 0

    /** 当前设备是否支持真正的折射（AGSL RuntimeShader，Android 13+） */
    val refractionSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** 外部内容即将变化（按下 FAB、切换检测等）时调用，让背景短时提速刷新 */
    private var burstFrames: Int = 0

    fun kick() {
        burstFrames = 20
    }

    internal fun consumeBurst(): Boolean {
        val b = burstFrames > 0
        if (b) burstFrames--
        return b
    }
}

/** 新旧背景纹理交叉淡入时长：足够抹掉抓帧台阶，又短到不产生明显拖影 */
private const val CROSSFADE_MS = 64f

/** 抓取区域占屏幕高度比例：玻璃全部贴底，只抓底部一条即可 */
private const val CAPTURE_HEIGHT_FRACTION = 0.28f

/** 沿 ContextWrapper 链找到 Activity（在 Popup 独立窗口里也能拿到主窗口） */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun rememberWindowBackdrop(active: Boolean = true): WindowBackdrop {
    val view = LocalView.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val backdrop = remember { WindowBackdrop() }

    DisposableEffect(active, view) {
        // 玻璃在独立 Popup 窗口中渲染，这里抓的是 Activity 主窗口（Surface-A），
        // 其中永远不含玻璃自身
        val window = context.findActivity()?.window
        if (!active || window == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            onDispose { }
        } else {
            val handler = Handler(Looper.getMainLooper())
            // 发布给 GPU 的区域纹理尺寸（屏幕底部一条，非全屏）
            var surfaceW = 0
            var surfaceH = 0
            // PixelCopy 全屏抓图缓冲的完整屏幕尺寸
            var fullW = 0
            var fullH = 0
            // 抓取区域顶部 y（窗口坐标）。玻璃全部贴底，GPU 侧只需底部约 28%
            // （且不少于 600px，给 FAB 凸起动画留足余量）：上传/采样像素量降到
            // 全屏 1/4 以下，中端机滑动时纹理更新才能跑满帧率、玻璃跟手。
            var regionTop = 0
            // 抓图单缓冲（全屏）：仅 PixelCopy 写入、回调内当场 CPU 裁剪读取，
            // inFlight 保证上一轮抓图+裁剪结束后才会发起下一轮，无需双缓冲
            var capBitmap: Bitmap? = null
            // 软件画布：把全屏抓图的底部一条裁剪进区域 back（1:1 纯像素拷贝）
            val regionCanvas = android.graphics.Canvas()
            val copySrc = android.graphics.Rect()
            val copyDst = android.graphics.Rect()
            // 三缓冲轮转（交叉淡入需要同时持有新/旧两张纹理）：
            //  front —— 已发布、正被玻璃窗口 GPU 采样的区域纹理，绝不在被采样时写入；
            //  back  —— 裁剪目标，每帧由全屏抓图拷入底部一条；
            //  fade  —— 上一代 front，发布后 64ms 淡出期内仍被叠加采样，不可提前写入。
            // 轮转顺序：裁 back → 交换时 fade←front、front←back、back←旧 fade。
            // 进入 back 槽的缓冲是两代之前的纹理，淡出必然已结束，覆盖安全。
            var front: Bitmap? = null
            var back: Bitmap? = null
            var fade: Bitmap? = null
            var inFlight = false
            var frameTick = 0
            var started = false
            var disposed = false
            var errorBackoff = 0
            // 变化检测：主窗口画面静止（哈希不变）则只预渲染、不交换，零视觉变化也省电
            var acceptedHash: Int? = null
            // 启动后先快速抓若干帧，让玻璃尽快拿到首屏背景
            var fastFrames = 12
            // 已排队、等待最近 vsync 原子发布的缓冲（PixelCopy 回调线程=主线程）
            var pendingSwap: Bitmap? = null
            // 交叉淡入起始时刻（doFrame 时间基）；NO_TIME 表示当前无淡出
            var fadeStartNanos = Long.MIN_VALUE
            // 第三张缓冲分配失败（低内存）后置 false，永久退回双缓冲、关闭淡入
            var tripleSupported = true
            // 抓帧失败退避期内，OnPreDraw 触发抓帧的最小间隔
            var lastCaptureAt = 0L

            // 变化检测采样：纹理本身就只是底部一条，在其上均匀取 5x6 网格即可
            // 覆盖胶囊与 FAB 后方的真实页面。坐标按位图尺寸比例计算并 clamp，
            // 避免 getPixel 越界后退化为每帧换帧，失去静止省电优化。
            fun contentHash(bm: Bitmap): Int {
                var h = 17
                val xMargin = 60.coerceAtMost((bm.width - 1) / 2)
                val yTop = 8.coerceAtMost(bm.height - 1)
                val yBottom = (bm.height - 9).coerceAtLeast(yTop)
                for (gx in 0..4) {
                    for (gy in 0..5) {
                        val x = xMargin + gx * (bm.width - 1 - 2 * xMargin) / 4
                        val y = yTop + gy * (yBottom - yTop) / 5
                        h = 31 * h + bm.getPixel(x, y)
                    }
                }
                return h
            }

            fun surfaceSize(): Size? {
                return try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val wm = context.getSystemService(WindowManager::class.java)
                        val b = wm.currentWindowMetrics.bounds
                        if (b.width() > 0 && b.height() > 0) Size(b.width(), b.height()) else null
                    } else {
                        @Suppress("DEPRECATION")
                        val display = view.display
                        val p = Point()
                        @Suppress("DEPRECATION")
                        display?.getRealSize(p)
                        if (p.x > 0 && p.y > 0) Size(p.x, p.y) else null
                    }
                } catch (e: Exception) {
                    null
                }
            }

            fun ensureBuffers(): Pair<Bitmap, Bitmap>? {
                val s = surfaceSize() ?: return null
                val regionH = maxOf(600, (s.height * CAPTURE_HEIGHT_FRACTION).toInt())
                    .coerceAtMost(s.height)
                val top = s.height - regionH
                if (s.width != fullW || s.height != fullH ||
                    capBitmap == null || front == null || back == null
                ) {
                    fullW = s.width
                    fullH = s.height
                    surfaceW = s.width
                    surfaceH = regionH
                    regionTop = top
                    backdrop.captureTopPx = top
                    try { capBitmap?.recycle() } catch (_: Exception) {}
                    try { front?.recycle() } catch (_: Exception) {}
                    try { back?.recycle() } catch (_: Exception) {}
                    try { fade?.recycle() } catch (_: Exception) {}
                    capBitmap = Bitmap.createBitmap(fullW, fullH, Bitmap.Config.ARGB_8888)
                    front = Bitmap.createBitmap(surfaceW, surfaceH, Bitmap.Config.ARGB_8888)
                    back = Bitmap.createBitmap(surfaceW, surfaceH, Bitmap.Config.ARGB_8888)
                    fade = null
                    acceptedHash = null
                } else if (regionH != surfaceH || regionTop != top) {
                    // 同一屏幕尺寸下显示比例变化（罕见）：只需重建区域缓冲
                    surfaceH = regionH
                    regionTop = top
                    backdrop.captureTopPx = top
                    try { front?.recycle() } catch (_: Exception) {}
                    try { back?.recycle() } catch (_: Exception) {}
                    try { fade?.recycle() } catch (_: Exception) {}
                    front = Bitmap.createBitmap(surfaceW, surfaceH, Bitmap.Config.ARGB_8888)
                    back = Bitmap.createBitmap(surfaceW, surfaceH, Bitmap.Config.ARGB_8888)
                    fade = null
                    acceptedHash = null
                }
                return capBitmap!! to back!!
            }

            // vsync 原子交换：只在 doFrame 里改一次共享状态，胶囊与 FAB 气泡当帧同时
            // 换用新纹理，像素缓冲引用的交换也是一次性完成，杜绝半新半旧的撕裂/闪烁。
            // 同时把旧 front 放入 fade 槽并启动 64ms 交叉淡入；第三个抓图目标优先
            // 复用两代之前的 fade 缓冲，首轮懒分配，分配失败则退回无淡入的双缓冲。
            val swapCallback = object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    val incoming = pendingSwap ?: return
                    pendingSwap = null
                    if (disposed) return
                    val oldFront = front
                    val oldFade = fade
                    front = incoming
                    fade = oldFront
                    back = when {
                        oldFade != null -> oldFade
                        tripleSupported -> try {
                            Bitmap.createBitmap(surfaceW, surfaceH, Bitmap.Config.ARGB_8888)
                        } catch (e: OutOfMemoryError) {
                            // 内存不足：放弃交叉淡入，永久退回双缓冲轮转（旧 front 直接当 back）
                            tripleSupported = false
                            fade = null
                            oldFront
                        }
                        else -> {
                            fade = null
                            oldFront
                        }
                    }
                    // back 绝不允许指向刚发布的 incoming（否则下一张抓图会覆写采样中的纹理）
                    if (back === incoming) back = oldFront
                    backdrop.bitmapState.value = front?.asImageBitmap()
                    if (fade != null && oldFront != null) {
                        fadeStartNanos = frameTimeNanos
                        backdrop.previousState.value = oldFront.asImageBitmap()
                        backdrop.blendState.value = 1f
                    } else {
                        fadeStartNanos = Long.MIN_VALUE
                        backdrop.previousState.value = null
                        backdrop.blendState.value = 0f
                    }
                }
            }

            fun requestCapture() {
                if (inFlight || disposed) return
                // 全屏抓图缓冲与区域发布三缓冲由 ensureBuffers 一起给出：
                // PixelCopy 只走全屏重载（该路径在更多系统版本上稳定），抓完再用
                // 软件 Canvas 把底部一条 1:1 裁剪进区域 back。GPU 只上传/采样区域纹理，
                // 带宽收益与区域直抓相同，又避开了带裁剪区的 PixelCopy 重载。
                val (capture, regionBack) = ensureBuffers() ?: return
                inFlight = true
                try {
                    PixelCopy.request(window, capture, { result ->
                        inFlight = false
                        if (disposed) {
                            // onDispose 发生在本次抓图飞行期间：写入此刻才结束，
                            // 在此补回收全部缓冲，防止 bitmap 随注销泄漏
                            try { capBitmap?.recycle() } catch (_: Exception) {}
                            try { front?.recycle() } catch (_: Exception) {}
                            try { back?.recycle() } catch (_: Exception) {}
                            try { fade?.recycle() } catch (_: Exception) {}
                            capBitmap = null
                            front = null; back = null; fade = null
                            return@request
                        }
                        if (result == PixelCopy.SUCCESS) {
                            errorBackoff = 0
                            // 全屏 → 底部区域，1:1 像素拷贝（~3MB，内存拷贝亚毫秒级）
                            try {
                                copySrc.set(0, regionTop, surfaceW, regionTop + surfaceH)
                                copyDst.set(0, 0, surfaceW, surfaceH)
                                regionCanvas.setBitmap(regionBack)
                                regionCanvas.drawBitmap(capture, copySrc, copyDst, null)
                            } catch (e: Exception) {
                                errorBackoff++
                                return@request
                            }
                            // 与上一帧「背景区」对比：画面静止则裁剪结果不发布，保持旧纹理
                            val hash = try {
                                contentHash(regionBack)
                            } catch (e: Exception) {
                                null
                            }
                            if (hash == null || hash != acceptedHash) {
                                if (hash != null) {
                                    acceptedHash = hash
                                    // 内容正在变化（滚动/动画）：续一段高速刷新追上运动
                                    fastFrames = maxOf(fastFrames, 10)
                                }
                                pendingSwap = regionBack
                                Choreographer.getInstance().removeFrameCallback(swapCallback)
                                Choreographer.getInstance().postFrameCallback(swapCallback)
                            }
                            // 哈希相同 → 已裁剪但不交换，玻璃画面零变化
                        } else {
                            // 连续失败（如 Surface 未就绪）时退避，避免空转压垮低端机
                            errorBackoff++
                        }
                    }, handler)
                } catch (e: Exception) {
                    inFlight = false
                    errorBackoff++
                }
            }

            val frameCallback = object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    if (!started || disposed) return
                    frameTick++
                    if (backdrop.consumeBurst()) {
                        fastFrames = maxOf(fastFrames, 16)
                    }
                    if (fastFrames > 0) fastFrames--
                    // 主窗口本身不含玻璃，抓帧对玻璃窗口外观毫无影响（无闸门、无跳帧）。
                    // 正常滑动期的抓帧由 OnPreDraw 当场发起（每帧一次、inFlight 背压）；
                    // 这里的轮询心跳仅作兜底：启动首帧、burst 提速、失败退避、Surface 恢复。
                    val interval = when {
                        errorBackoff > 3 -> 90
                        fastFrames > 0 -> 4
                        else -> 30
                    }
                    if (frameTick % interval == 0) {
                        if (!inFlight) requestCapture()
                    }
                    // 推进交叉淡入：每帧把旧纹理叠加 alpha 往下收，64ms 收尾。
                    // 持续滑动时交换每帧发生、淡入被不断重启，等效轻微拖影，
                    // 正好掩盖离散抓帧的台阶感；停下后最后一次淡入干净结束。
                    if (fadeStartNanos != Long.MIN_VALUE) {
                        val ageMs = (frameTimeNanos - fadeStartNanos) / 1_000_000f
                        val b = (1f - ageMs / CROSSFADE_MS).coerceIn(0f, 1f)
                        if (b <= 0f) {
                            fadeStartNanos = Long.MIN_VALUE
                            backdrop.previousState.value = null
                            backdrop.blendState.value = 0f
                        } else if (b != backdrop.blendState.value) {
                            backdrop.blendState.value = b
                        }
                    }
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }

            // 监听 Activity 主窗口（Surface-A）的真实绘制：Compose 滚动、动画、状态
            // 变更都会触发 decorView 重绘。监听必须挂在【主窗口】的 ViewTreeObserver
            // 上，而不是 Popup 自己——Popup 重绘与背景内容无关。
            // 在 OnPreDraw【当场】请求 PixelCopy（遍历绘制的同一帧入队），比等下一个
            // Choreographer 回调再请求少排一次队，是滑动跟手的最低延迟路径；
            // inFlight 天然背压：GPU/硬件合成器跟不上时自动丢帧，永远只抓最新画面。
            val decorView = window.decorView
            val preDrawListener = ViewTreeObserver.OnPreDrawListener {
                if (started && !disposed && !inFlight) {
                    if (errorBackoff > 3) {
                        // 退避期把绘制驱动的抓帧限制到 10fps，避免 Surface 异常时空转
                        val now = System.nanoTime()
                        if (now - lastCaptureAt >= 100_000_000L) {
                            lastCaptureAt = now
                            requestCapture()
                        }
                    } else {
                        lastCaptureAt = System.nanoTime()
                        requestCapture()
                    }
                }
                true
            }
            fun attachPreDrawListener() {
                val vto = decorView.viewTreeObserver
                if (vto.isAlive) vto.addOnPreDrawListener(preDrawListener)
            }
            fun detachPreDrawListener() {
                val vto = decorView.viewTreeObserver
                if (vto.isAlive) vto.removeOnPreDrawListener(preDrawListener)
            }

            // 启动/停止必须幂等：Choreographer 的回调队列不会对同一 callback 实例去重，
            // 若不先 remove 就再次 postFrameCallback，同一 frameCallback 会在队列里
            // 叠加多份，每个 vsync 触发多次 doFrame 且各自自我续投，形成常驻多实例
            // （双倍帧计数、双倍 PixelCopy 探测与耗电），等价于传统 View 体系的
            // “重复 inflate / 重复 addView 未移除”泄漏。
            fun startFrameLoop() {
                if (started || disposed) return
                started = true
                attachPreDrawListener()
                val choreographer = Choreographer.getInstance()
                choreographer.removeFrameCallback(frameCallback)
                choreographer.postFrameCallback(frameCallback)
            }

            fun stopFrameLoop() {
                if (!started) return
                started = false
                detachPreDrawListener()
                Choreographer.getInstance().removeFrameCallback(frameCallback)
            }

            val lifecycleObserver = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> startFrameLoop()
                    Lifecycle.Event.ON_PAUSE -> stopFrameLoop()
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
            // 在 RESUMED 状态下 addObserver 会同步回放 ON_RESUME，帧循环已由
            // observer 经 startFrameLoop() 幂等启动，此处不能再无条件 post 一次。

            onDispose {
                disposed = true
                stopFrameLoop()
                detachPreDrawListener()
                Choreographer.getInstance().removeFrameCallback(swapCallback)
                lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
                if (inFlight) {
                    // PixelCopy 仍在向抓图缓冲写入，延迟到回调返回后再回收，
                    // 避免向已 recycle 的 bitmap 写入导致崩溃或 native 泄漏
                } else {
                    try { capBitmap?.recycle() } catch (_: Exception) {}
                    try { front?.recycle() } catch (_: Exception) {}
                    try { back?.recycle() } catch (_: Exception) {}
                    try { fade?.recycle() } catch (_: Exception) {}
                    capBitmap = null
                    front = null; back = null; fade = null
                }
            }
        }
    }

    return backdrop
}

/* ------------------------------------------------------------------------------------------------------------------
 * 着色器：圆角 SDF / 折射位移改编自 Kyant0/AndroidLiquidGlass（Apache-2.0），
 * 正圆版本为本项目自写。
 * ---------------------------------------------------------------------------------------------------------------- */

private const val ROUNDED_RECT_SDF = """
float radiusAt(float2 coord, float4 radii) {
    if (coord.x >= 0.0) {
        if (coord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (coord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}
float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}
float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}"""

private const val REFRACTION_RECT_SHADER = """
uniform shader content;
uniform float2 size;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;

$ROUNDED_RECT_SDF

float circleMap(float x) { return 1.0 - sqrt(1.0 - x * x); }
// 注意：AGSL 不允许函数参数为 shader 类型，只能直接引用全局 content
half4 sampleLinear(float2 uv) {
    return content.eval(uv);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = coord - halfSize;
    float radius = radiusAt(coord, cornerRadii);
    float sd = sdRoundedRect(centeredCoord, halfSize, radius);
    if (-sd >= refractionHeight) { return sampleLinear(coord); }
    sd = min(sd, 0.0);
    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + depthEffect * normalize(centeredCoord));
    return sampleLinear(coord + d * grad);
}"""

/** 正圆玻璃球折射（FAB 按下时凸起的透明气泡） */
private const val REFRACTION_CIRCLE_SHADER = """
uniform shader content;
uniform float2 size;
uniform float radius;
uniform float refractionHeight;
uniform float refractionAmount;

float circleMap(float x) { return 1.0 - sqrt(1.0 - x * x); }
half4 sampleLinear(float2 uv) {
    return content.eval(uv);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 p = coord - halfSize;
    float dist = length(p);
    float sd = dist - radius;
    if (-sd >= refractionHeight) { return sampleLinear(coord); }
    sd = min(sd, 0.0);
    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    float2 grad = dist > 0.0001 ? p / dist : float2(0.0);
    return sampleLinear(coord + d * grad);
}"""

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class GlassRuntime(
    val rectShader: RuntimeShader,
    val circleShader: RuntimeShader
)

/**
 * 透明液态玻璃表面：真实折射玻璃后方画面（Android 13+），再叠加极薄 tint、
 * 顶部反光与通透棱边高光。不做磨砂模糊，背景始终清晰可见。
 *
 * @param backdrop [rememberWindowBackdrop] 提供的实时窗口画面
 * @param shape 胶囊 / 正圆
 * @param style 玻璃外观（tint、高光、折射高度/强度、棱边宽度）
 * @param corner 胶囊圆角（默认完全圆角）
 */
fun Modifier.liquidGlass(
    backdrop: WindowBackdrop,
    shape: GlassShape,
    style: GlassStyle,
    corner: Dp = 999.dp
): Modifier = composed {
    // 玻璃在屏幕上的绝对边界（= Activity 主窗口坐标）。背景纹理抓自整个主窗口，
    // 采样偏移必须用屏幕绝对坐标；玻璃位于 Popup 独立窗口时，positionInWindow 只
    // 是相对该 Popup 窗口的局部坐标，故用宿主 View 的屏幕位置 + root 内坐标合成。
    var screenBounds by remember { mutableStateOf(Rect.Zero) }
    var runtime by remember { mutableStateOf<GlassRuntime?>(null) }
    var shaderFailed by remember { mutableStateOf(false) }
    // BitmapShader / Matrix / Paint 按三张循环缓冲复用，避免每帧新建一堆
    // 原生对象造成 GC 抖动（三缓冲实例恒定，缓存大小恒为 3，随 bitmap 回收自动清除）
    val drawCache = remember { BackdropDrawCache() }
    val density = LocalDensity.current
    val hostView = LocalView.current
    val cornerPx = with(density) { corner.toPx() }

    Modifier
        .onGloballyPositioned { coords ->
            val viewLoc = IntArray(2)
            hostView.getLocationOnScreen(viewLoc)
            val pos = coords.positionInRoot()
            val left = viewLoc[0] + pos.x
            val top = viewLoc[1] + pos.y
            screenBounds = Rect(left, top, left + coords.size.width, top + coords.size.height)
            // 着色器只创建一次；每帧绘制时绑定最新的主窗口画面作为 content
            // 个别 ROM 的 AGSL 编译器可能有差异，失败即永久降级为透明背景，绝不崩溃
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                runtime == null && !shaderFailed
            ) {
                try {
                    runtime = GlassRuntime(
                        rectShader = RuntimeShader(REFRACTION_RECT_SHADER),
                        circleShader = RuntimeShader(REFRACTION_CIRCLE_SHADER)
                    )
                } catch (e: Exception) {
                    shaderFailed = true
                }
            }
        }
        .then(
            Modifier.drawWithContent {
                val w = size.width
                val h = size.height
                if (w <= 1f || h <= 1f) {
                    drawContent()
                    return@drawWithContent
                }
                val radius = if (shape == GlassShape.CIRCLE) {
                    minOf(w, h) / 2f
                } else {
                    cornerPx.coerceAtMost(minOf(w, h) / 2f)
                }

                // 折射纹理层：玻璃在独立 Popup 窗口（Surface-B），背景纹理抓自不含玻璃的
                // 主窗口（Surface-A），纹理里永远没有玻璃自身，不存在正反馈/重影，
                // 因此每一帧都正常绘制，抓帧过程中这一层也绝不消失——不会有任何闪烁。
                run {
                    val rt = runtime
                    val currentBitmap = backdrop.bitmap?.asAndroidBitmap()
                    // 上一张纹理与淡出权重：64ms 内叠加旧画面，台阶感被淡入抹平
                    val prevBitmap = backdrop.previous?.asAndroidBitmap()
                    val blend = backdrop.blend
                    val sourceTop = backdrop.captureTopPx.toFloat()
                    // 首次布局完成前 screenBounds 为 Zero，此时不能采样，否则会透出屏幕左上角画面
                    val boundsReady = screenBounds.width > 1f && screenBounds.height > 1f && sourceTop > 0f
                    if (boundsReady && rt != null && currentBitmap != null &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        try {
                            drawRefractedBackdrop(
                                drawCache, rt, currentBitmap, shape, screenBounds, sourceTop,
                                w, h, radius, style
                            )
                            if (prevBitmap != null && blend > 0.01f) {
                                drawRefractedBackdrop(
                                    drawCache, rt, prevBitmap, shape, screenBounds, sourceTop,
                                    w, h, radius, style, alpha = blend
                                )
                            }
                        } catch (e: Exception) {
                            // 个别驱动运行期异常时降级为直接透出
                            drawPlainBackdrop(drawCache, currentBitmap, screenBounds, sourceTop, w, h, radius)
                            if (prevBitmap != null && blend > 0.01f) {
                                drawPlainBackdrop(
                                    drawCache, prevBitmap, screenBounds, sourceTop, w, h, radius, alpha = blend
                                )
                            }
                        }
                    } else if (boundsReady) {
                        val fallback = backdrop.bitmap
                        if (fallback != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            drawPlainBackdrop(
                                drawCache, fallback.asAndroidBitmap(), screenBounds, sourceTop, w, h, radius
                            )
                            if (prevBitmap != null && blend > 0.01f) {
                                drawPlainBackdrop(
                                    drawCache, prevBitmap, screenBounds, sourceTop, w, h, radius, alpha = blend
                                )
                            }
                        }
                    }
                }

                // 玻璃体内极薄环境 tint（垂直向微弱明度变化，模拟厚度）
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to style.tintTop,
                        0.5f to style.tintMid,
                        1f to style.tintBottom
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )

                // 顶部斜向反光
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            style.sheen,
                            Color.Transparent,
                            style.sheen.copy(alpha = style.sheen.alpha * 0.4f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h * 0.55f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius)
                )

                // 通透棱边：上缘明亮、两侧收敛、底部回光，真实玻璃边缘
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to style.rimBright,
                        0.42f to style.rimDim,
                        0.72f to style.rimDim.copy(alpha = style.rimDim.alpha * 0.4f),
                        1f to style.rimBright.copy(alpha = style.rimBright.alpha * 0.7f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
                    style = Stroke(width = style.rimWidth)
                )

                drawContent()
            }
        )
}

/** 每帧绘制用的原生对象缓存：三张循环缓冲各对应一个 BitmapShader，反复复用 */
private class BackdropDrawCache {
    private val shaders = java.util.WeakHashMap<Bitmap, BitmapShader>()
    val matrix = Matrix()
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun shaderFor(bitmap: Bitmap): BitmapShader =
        shaders.getOrPut(bitmap) {
            BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
}

/** API 33+：对背景做圆角/圆形凸透镜折射后透出 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun ContentDrawScope.drawRefractedBackdrop(
    cache: BackdropDrawCache,
    rt: GlassRuntime,
    sourceBitmap: Bitmap,
    shape: GlassShape,
    bounds: Rect,
    sourceTop: Float,
    w: Float,
    h: Float,
    radius: Float,
    style: GlassStyle,
    alpha: Float = 1f
) {
    // 区域纹理第 0 行对应屏幕 sourceTop 行：屏幕点 y 在纹理中的坐标为 y - sourceTop，
    // 即在原全屏偏移 -bounds.top 上把纹理下移（+sourceTop）
    cache.matrix.setTranslate(-bounds.left, sourceTop - bounds.top)
    val contentShader = cache.shaderFor(sourceBitmap).apply { setLocalMatrix(cache.matrix) }
    val shader = if (shape == GlassShape.CIRCLE) rt.circleShader else rt.rectShader
    shader.setInputShader("content", contentShader)
    shader.setFloatUniform("size", w, h)
    if (shape == GlassShape.CIRCLE) {
        shader.setFloatUniform("radius", radius)
        shader.setFloatUniform("refractionHeight", style.refractionHeight)
        shader.setFloatUniform("refractionAmount", -style.refractionAmount)
    } else {
        shader.setFloatUniform("cornerRadii", radius, radius, radius, radius)
        shader.setFloatUniform("refractionHeight", style.refractionHeight)
        shader.setFloatUniform("refractionAmount", -style.refractionAmount)
        shader.setFloatUniform("depthEffect", 0.55f)
    }
    with(cache.paint) {
        this.shader = shader
        this.alpha = (alpha.coerceIn(0f, 1f) * 255f).toInt()
    }
    drawContext.canvas.nativeCanvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, cache.paint)
}

/** API 26-32：直接透出玻璃后方窗口画面（无折射位移），保持高透明而非模糊 */
private fun ContentDrawScope.drawPlainBackdrop(
    cache: BackdropDrawCache,
    bitmap: Bitmap,
    bounds: Rect,
    sourceTop: Float,
    w: Float,
    h: Float,
    radius: Float,
    alpha: Float = 1f
) {
    cache.matrix.setTranslate(-bounds.left, sourceTop - bounds.top)
    val contentShader = cache.shaderFor(bitmap).apply { setLocalMatrix(cache.matrix) }
    with(cache.paint) {
        shader = contentShader
        this.alpha = (alpha.coerceIn(0f, 1f) * 255f).toInt()
    }
    drawContext.canvas.nativeCanvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, cache.paint)
}
