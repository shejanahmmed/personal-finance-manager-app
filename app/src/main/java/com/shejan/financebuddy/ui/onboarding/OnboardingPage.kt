package com.shejan.financebuddy.ui.onboarding

import androidx.compose.runtime.Composable

/**
 * Data model for a single onboarding slide.
 *
 * @param title     Bold headline text for the slide.
 * @param subtitle  Supporting description text.
 * @param visual    A Composable lambda that renders the slide's abstract visual art.
 */
data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val visual: @Composable () -> Unit,
)
