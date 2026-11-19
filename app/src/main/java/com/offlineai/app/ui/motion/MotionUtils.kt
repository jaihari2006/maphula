package com.offlineai.app.ui.motion

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.view.animation.PathInterpolator
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.offlineai.app.R

/**
 * High-performance fluid motion & tactile physics engine for Maphula.
 * Implements smooth spring-damped touch feedback, buttery entrance curves,
 * organic breathing pulses, and seamless layout morphing.
 */
object MotionUtils {

    // Standard buttery fast-out-slow-in cubic bezier curve (Material 3 standard)
    val BUTTERY_EASING = PathInterpolator(0.2f, 0.0f, 0.0f, 1.0f)
    val DECELERATE_EASING = DecelerateInterpolator(2.2f)
    val SPRING_OVERSHOOT = OvershootInterpolator(2.4f)

    /**
     * Attaches tactile spring-damped press feedback to any interactive view.
     * When touched down, it gently compresses (0.94x) with slight alpha dimming (0.90).
     * When released or cancelled, it rebounds elastically with smooth spring overshoot.
     * Preserves normal onClick listeners without consuming touch events.
     */
    @SuppressLint("ClickableViewAccessibility")
    fun applyButteryPress(
        view: View,
        pressedScale: Float = 0.94f,
        pressDuration: Long = 70L,
        releaseDuration: Long = 200L,
        tension: Float = 2.4f
    ) {
        view.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(pressedScale)
                        .scaleY(pressedScale)
                        .alpha(0.90f)
                        .setDuration(pressDuration)
                        .setInterpolator(DECELERATE_EASING)
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .alpha(1.0f)
                        .setDuration(releaseDuration)
                        .setInterpolator(OvershootInterpolator(tension))
                        .start()
                }
            }
            false // allow click/scroll handling to proceed unimpeded
        }
    }

    /**
     * Staggered buttery entrance for list items, cards, and chat bubbles.
     */
    fun animateEntrance(
        view: View,
        index: Int = 0,
        startDelayMultiplier: Long = 25L,
        duration: Long = 300L
    ) {
        view.alpha = 0f
        view.translationY = 30f
        view.scaleX = 0.96f
        view.scaleY = 0.96f

        val delay = (index.coerceAtMost(10) * startDelayMultiplier)
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(delay)
            .setDuration(duration)
            .setInterpolator(OvershootInterpolator(1.2f))
            .start()
    }

    /**
     * Silky slide & fade in for tab screens or switching containers.
     */
    fun animateSlideFadeIn(view: View, duration: Long = 240L) {
        view.alpha = 0f
        view.translationY = 25f
        view.scaleX = 0.98f
        view.scaleY = 0.98f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(duration)
            .setInterpolator(BUTTERY_EASING)
            .start()
    }

    /**
     * Butter-smooth fragment switch transition with scale & slide fade.
     */
    fun animateFragmentTransition(
        outgoingView: View?,
        incomingView: View?,
        duration: Long = 240L
    ) {
        outgoingView?.animate()
            ?.alpha(0f)
            ?.scaleX(0.97f)
            ?.scaleY(0.97f)
            ?.setDuration(duration / 2)
            ?.setInterpolator(DECELERATE_EASING)
            ?.start()

        incomingView?.let { v ->
            v.alpha = 0f
            v.translationY = 20f
            v.scaleX = 0.97f
            v.scaleY = 0.97f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(duration)
                .setInterpolator(BUTTERY_EASING)
                .start()
        }
    }

    /**
     * Quick elastic spring bounce for buttons upon action confirmation or toggle.
     */
    fun animateSpringBounce(view: View, duration: Long = 280L) {
        view.scaleX = 0.90f
        view.scaleY = 0.90f
        view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(duration)
            .setInterpolator(OvershootInterpolator(2.8f))
            .start()
    }

    /**
     * Starts an organic breathing pulse on a view during generation or active AI work.
     */
    fun startBreathingPulse(
        view: View,
        minScale: Float = 0.96f,
        maxScale: Float = 1.04f,
        duration: Long = 750L
    ) {
        stopBreathingPulse(view)

        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, minScale, maxScale).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
        }
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, minScale, maxScale).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
        }

        val animSet = AnimatorSet().apply {
            playTogether(scaleX, scaleY)
            start()
        }
        view.setTag(R.id.tag_pulse_animator, animSet)
    }

    /**
     * Gracefully stops the breathing pulse and springs back to normal scale.
     */
    fun stopBreathingPulse(view: View) {
        val animSet = view.getTag(R.id.tag_pulse_animator) as? AnimatorSet
        animSet?.cancel()
        view.setTag(R.id.tag_pulse_animator, null)
        view.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(160L)
            .setInterpolator(BUTTERY_EASING)
            .start()
    }

    /**
     * Smooth delayed layout transition with buttery fast-out-slow-in curves.
     */
    fun beginButteryTransition(container: ViewGroup, duration: Long = 260L) {
        val transition = AutoTransition().apply {
            this.duration = duration
            interpolator = BUTTERY_EASING
        }
        TransitionManager.beginDelayedTransition(container, transition)
    }

    /**
     * Smoothly morphs color properties between two ARGB color values.
     */
    fun animateColor(
        fromColor: Int,
        toColor: Int,
        duration: Long = 240L,
        onUpdate: (Int) -> Unit
    ): ValueAnimator {
        val animator = ValueAnimator.ofObject(ArgbEvaluator(), fromColor, toColor).apply {
            this.duration = duration
            interpolator = BUTTERY_EASING
            addUpdateListener { va ->
                onUpdate(va.animatedValue as Int)
            }
        }
        animator.start()
        return animator
    }

    /**
     * Smooth buttery RecyclerView scrolling that decelerates with natural physics.
     */
    fun smoothScrollButtery(recyclerView: RecyclerView, position: Int) {
        if (position < 0 || position >= (recyclerView.adapter?.itemCount ?: 0)) return
        val scroller = object : LinearSmoothScroller(recyclerView.context) {
            override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                return 35f / displayMetrics.densityDpi
            }
            override fun calculateTimeForDeceleration(dx: Int): Int {
                return (super.calculateTimeForDeceleration(dx) * 1.2f).toInt()
            }
        }
        scroller.targetPosition = position
        recyclerView.layoutManager?.startSmoothScroll(scroller)
    }

    /**
     * Executes the cinematic app opening animation starting directly from the launcher app icon:
     * 1. The actual launcher app icon emerges with tactile spring physics, mimicking home screen expansion.
     * 2. An ambient glow ring ripples outward from behind the app icon.
     * 3. The brand identity and sovereign badges unfurl from beneath the icon.
     * 4. The app icon expands dynamically into the foreground as the main app window zooms in.
     */
    fun animateAppOpen(
        splashView: View,
        appIconView: View,
        glowRingView: View,
        titleView: View,
        badgeView: View,
        taglineView: View,
        statusView: View,
        mainContentView: View,
        creatorCreditView: View? = null,
        onComplete: () -> Unit = {}
    ) {
        splashView.visibility = View.VISIBLE
        splashView.alpha = 1.0f

        // Initial state: App icon starts at home-screen scale
        appIconView.scaleX = 0.68f
        appIconView.scaleY = 0.68f
        appIconView.alpha = 0.0f

        glowRingView.scaleX = 0.60f
        glowRingView.scaleY = 0.60f
        glowRingView.alpha = 0.0f

        titleView.alpha = 0.0f
        titleView.translationY = -15f

        badgeView.alpha = 0.0f
        badgeView.translationY = 15f

        taglineView.alpha = 0.0f
        taglineView.translationY = 15f

        statusView.alpha = 0.0f
        statusView.translationY = 15f

        creatorCreditView?.alpha = 0.0f
        creatorCreditView?.translationY = 20f
        creatorCreditView?.scaleX = 0.90f
        creatorCreditView?.scaleY = 0.90f

        mainContentView.scaleX = 0.93f
        mainContentView.scaleY = 0.93f
        mainContentView.alpha = 0.4f

        val splashStartTime = System.currentTimeMillis()
        var isFinished = false
        fun finishSplash() {
            if (isFinished) return
            isFinished = true

            // Transition from app icon: Icon expands outward into the full screen
            appIconView.animate()
                .scaleX(1.35f)
                .scaleY(1.35f)
                .alpha(0.0f)
                .setDuration(360L)
                .setInterpolator(DECELERATE_EASING)
                .start()

            glowRingView.animate()
                .scaleX(1.8f)
                .scaleY(1.8f)
                .alpha(0.0f)
                .setDuration(360L)
                .setInterpolator(DECELERATE_EASING)
                .start()

            creatorCreditView?.animate()
                ?.alpha(0.0f)
                ?.setDuration(300L)
                ?.start()

            splashView.animate()
                .alpha(0.0f)
                .setDuration(380L)
                .setInterpolator(DECELERATE_EASING)
                .withEndAction {
                    splashView.visibility = View.GONE
                    onComplete()
                }
                .start()

            // Main app content zooms in to full viewport
            mainContentView.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(400L)
                .setInterpolator(BUTTERY_EASING)
                .start()
        }

        // Tap-to-skip only allowed after "App Created by Jaihari U S" has been displayed for at least 3.0 seconds
        splashView.setOnClickListener {
            val elapsed = System.currentTimeMillis() - splashStartTime
            if (elapsed >= 3600L) {
                finishSplash()
            }
        }

        // Phase 1: App icon springs into view (0 - 450ms)
        appIconView.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(450L)
            .setInterpolator(OvershootInterpolator(2.4f))
            .start()

        glowRingView.animate()
            .scaleX(1.25f)
            .scaleY(1.25f)
            .alpha(0.6f)
            .setDuration(500L)
            .setInterpolator(DECELERATE_EASING)
            .withEndAction {
                glowRingView.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .alpha(0.3f)
                    .setDuration(400L)
                    .start()
            }
            .start()

        // Phase 2: Brand title unfurls from icon (160ms - 520ms)
        titleView.animate()
            .alpha(1.0f)
            .translationY(0.0f)
            .setStartDelay(180L)
            .setDuration(360L)
            .setInterpolator(BUTTERY_EASING)
            .start()

        badgeView.animate()
            .alpha(1.0f)
            .translationY(0.0f)
            .setStartDelay(260L)
            .setDuration(360L)
            .setInterpolator(BUTTERY_EASING)
            .start()

        taglineView.animate()
            .alpha(1.0f)
            .translationY(0.0f)
            .setStartDelay(320L)
            .setDuration(360L)
            .setInterpolator(BUTTERY_EASING)
            .start()

        statusView.animate()
            .alpha(1.0f)
            .translationY(0.0f)
            .setStartDelay(380L)
            .setDuration(300L)
            .setInterpolator(BUTTERY_EASING)
            .start()

        // Creator Credit: "App Created by Jaihari U S" appears elegantly and is displayed for at least 3.2 seconds
        creatorCreditView?.animate()
            ?.alpha(1.0f)
            ?.translationY(0.0f)
            ?.scaleX(1.0f)
            ?.scaleY(1.0f)
            ?.setStartDelay(250L)
            ?.setDuration(400L)
            ?.setInterpolator(OvershootInterpolator(2.0f))
            ?.withEndAction {
                // Keep the creator credit displayed on screen for at least 3.2 seconds (3200ms)
                splashView.postDelayed({
                    finishSplash()
                }, 3200L)
            }
            ?.start()
    }
}

// Kotlin Extension functions for expressive ergonomics across the app
fun View.applyButteryPress(
    pressedScale: Float = 0.94f,
    pressDuration: Long = 70L,
    releaseDuration: Long = 200L,
    tension: Float = 2.4f
) = MotionUtils.applyButteryPress(this, pressedScale, pressDuration, releaseDuration, tension)

fun View.animateButteryEntrance(index: Int = 0, startDelayMultiplier: Long = 25L, duration: Long = 300L) =
    MotionUtils.animateEntrance(this, index, startDelayMultiplier, duration)

fun View.animateSlideFadeIn(duration: Long = 240L) =
    MotionUtils.animateSlideFadeIn(this, duration)

fun View.animateSpringBounce(duration: Long = 280L) =
    MotionUtils.animateSpringBounce(this, duration)

fun View.startBreathingPulse(minScale: Float = 0.96f, maxScale: Float = 1.04f, duration: Long = 750L) =
    MotionUtils.startBreathingPulse(this, minScale, maxScale, duration)

fun View.stopBreathingPulse() =
    MotionUtils.stopBreathingPulse(this)

fun ViewGroup.beginButteryTransition(duration: Long = 260L) =
    MotionUtils.beginButteryTransition(this, duration)

fun RecyclerView.smoothScrollButtery(position: Int) =
    MotionUtils.smoothScrollButtery(this, position)
