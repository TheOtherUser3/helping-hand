package com.example.helpinghand.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.helpinghand.ui.theme.ShoppingColors as C
import com.google.accompanist.pager.*
import kotlinx.coroutines.launch

// Data class for onboarding pages

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val backgroundColor: Color
)

@OptIn(ExperimentalPagerApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pages = listOf(
            OnboardingPage(
                title = "Welcome to Helping Hand",
                description = "Your all-in-one household management app. Keep track of shopping, cleaning, appointments, and more!",
                icon = Icons.Filled.Home,
                backgroundColor = C.Primary
            ),
            OnboardingPage(
                title = "Shopping & Meals",
                description = "Create shopping lists and generate meal suggestions based on your items. Tap the restaurant icon to get recipe ideas!",
                icon = Icons.Filled.ShoppingCart,
                backgroundColor = C.Primary.copy(alpha = 0.8f)
            ),
            OnboardingPage(
                title = "Cleaning Reminders",
                description = "Set intervals for cleaning tasks and never forget what needs attention. Track when items are due or overdue.",
                icon = Icons.Filled.CleaningServices,
                backgroundColor = C.Primary.copy(alpha = 0.9f)
            ),
            OnboardingPage(
                title = "Doctor Appointments",
                description = "Store doctor contacts, set visit intervals, and never miss an appointment. Add documents and phone numbers for easy access.",
                icon = Icons.Filled.CalendarToday,
                backgroundColor = C.Primary
            ),
            OnboardingPage(
                title = "Contacts",
                description = "Keep all your important contacts organized in one place with sticky headers by type.",
                icon = Icons.Filled.Contacts,
                backgroundColor = C.Primary.copy(alpha = 0.85f)
            ),
            OnboardingPage(
                title = "Need Help?",
                description = "Tap the ? button next to Settings anytime to view this guide again. Let's get started!",
                icon = Icons.Filled.Help,
                backgroundColor = C.Primary
            )
        )


    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = C.Background) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(C.SurfaceVariant)
            ) {
                // Pager
                HorizontalPager(
                    count = pages.size,
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    OnboardingPageContent(pages[page])
                }

                // Bottom Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(C.Background)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page Indicators
                    HorizontalPagerIndicator(
                        pagerState = pagerState,
                        activeColor = C.Primary,
                        inactiveColor = C.OnSurfaceVariant.copy(alpha = 0.3f),
                        indicatorWidth = 8.dp,
                        indicatorHeight = 8.dp,
                        spacing = 8.dp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Skip Button (hidden on last page)
                        if (pagerState.currentPage < pages.size - 1) {
                            TextButton(
                                onClick = { onFinish() }
                            ) {
                                Text(
                                    "Skip",
                                    color = C.OnSurfaceVariant,
                                    fontSize = 16.sp
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(80.dp))
                        }

                        // Next/Get Started Button
                        Button(
                            onClick = {
                                if (pagerState.currentPage < pages.size - 1) {
                                    scope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                } else {
                                    onFinish()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = C.Primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                text = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon with colored background
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(page.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Title
        Text(
            text = page.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = C.OnBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = page.description,
            fontSize = 16.sp,
            color = C.OnSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

// Onboarding Dialog (for help replay)
@OptIn(ExperimentalPagerApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnboardingDialog(
    onDismiss: () -> Unit
) {
    val pages = listOf(
            OnboardingPage(
                title = "Welcome Back!",
                description = "Here's a quick refresher on how to use Helping Hand.",
                icon = Icons.Filled.Home,
                backgroundColor = C.Primary
            ),
            OnboardingPage(
                title = "Shopping & Meals",
                description = "Create shopping lists and generate meal suggestions based on your items. Tap the restaurant icon to get recipe ideas!",
                icon = Icons.Filled.ShoppingCart,
                backgroundColor = C.Primary.copy(alpha = 0.8f)
            ),
            OnboardingPage(
                title = "Cleaning Reminders",
                description = "Set intervals for cleaning tasks and never forget what needs attention. Track when items are due or overdue.",
                icon = Icons.Filled.CleaningServices,
                backgroundColor = C.Primary.copy(alpha = 0.9f)
            ),
            OnboardingPage(
                title = "Doctor Appointments",
                description = "Store doctor contacts, set visit intervals, and never miss an appointment. Add documents and phone numbers for easy access.",
                icon = Icons.Filled.CalendarToday,
                backgroundColor = C.Primary
            ),
            OnboardingPage(
                title = "Contacts",
                description = "Keep all your important contacts organized in one place with sticky headers by type.",
                icon = Icons.Filled.Contacts,
                backgroundColor = C.Primary.copy(alpha = 0.85f)
            )
        )

    val pagerState = rememberPagerState()
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.8f)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = C.Background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(C.SurfaceVariant)
                        .padding(16.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = C.OnBackground
                        )
                    }
                    Text(
                        text = "Help Guide",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = C.OnBackground,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Pager
                HorizontalPager(
                    count = pages.size,
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(C.SurfaceVariant)
                ) { page ->
                    OnboardingPageContent(pages[page])
                }

                // Page Indicators
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(C.Background)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalPagerIndicator(
                        pagerState = pagerState,
                        activeColor = C.Primary,
                        inactiveColor = C.OnSurfaceVariant.copy(alpha = 0.3f),
                        indicatorWidth = 8.dp,
                        indicatorHeight = 8.dp,
                        spacing = 8.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous Button
                        TextButton(
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage > 0) {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            enabled = pagerState.currentPage > 0
                        ) {
                            Text(
                                "Previous",
                                color = if (pagerState.currentPage > 0) C.Primary else C.OnSurfaceVariant.copy(alpha = 0.3f)
                            )
                        }

                        // Next Button
                        Button(
                            onClick = {
                                scope.launch {
                                    if (pagerState.currentPage < pages.size - 1) {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    } else {
                                        onDismiss()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = C.Primary)
                        ) {
                            Text(
                                if (pagerState.currentPage < pages.size - 1) "Next" else "Got it!"
                            )
                        }
                    }
                }
            }
        }
    }
}