package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.MovieViewModel
import com.example.ui.theme.CineGreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GoogleAuthScreen(
    viewModel: MovieViewModel,
    onAuthSuccess: () -> Unit,
    onSkipGuest: () -> Unit,
    canDismiss: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var fullNameInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isNameFocused by remember { mutableStateOf(false) }
    var isPhoneFocused by remember { mutableStateOf(false) }

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    val nameCardElevation by animateFloatAsState(
        targetValue = if (isNameFocused) 14f else 6f,
        animationSpec = tween(250),
        label = "name_elevation"
    )

    val phoneCardElevation by animateFloatAsState(
        targetValue = if (isPhoneFocused) 14f else 6f,
        animationSpec = tween(250),
        label = "phone_elevation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("google_auth_screen")
    ) {
        // 1. Cinematic illustration background kept clearly visible without heavy dark overlays
        Image(
            painter = painterResource(id = R.drawable.img_auth_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Light, subtle gradient at bottom so illustration shines through
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.15f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.50f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // 3. Dismiss button if accessible from Profile
        if (canDismiss) {
            IconButton(
                onClick = onSkipGuest,
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .testTag("auth_dismiss_button")
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.35f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }

        // 4. Clean bottom sheet layout with 2 white round border cards
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400)) + slideInVertically(
                animationSpec = tween(400, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 3 }
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .navigationBarsPadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // CARD 1: Round border white card for NAME
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    border = BorderStroke(
                        width = if (isNameFocused) 2.dp else 1.dp,
                        color = if (isNameFocused) CineGreen else Color.White.copy(alpha = 0.9f)
                    ),
                    shadowElevation = nameCardElevation.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(nameCardElevation.dp, RoundedCornerShape(26.dp))
                        .testTag("auth_name_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Name",
                            tint = if (isNameFocused) CineGreen else Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        TextField(
                            value = fullNameInput,
                            onValueChange = {
                                fullNameInput = it
                                errorMessage = null
                            },
                            textStyle = TextStyle(
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            placeholder = {
                                Text(
                                    text = "Name",
                                    color = Color(0xFF64748B),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                disabledTextColor = Color.Black,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isNameFocused = it.isFocused }
                                .testTag("auth_name_input")
                        )
                    }
                }

                // CARD 2: Round border white card for PHONE NUMBER
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    border = BorderStroke(
                        width = if (isPhoneFocused) 2.dp else 1.dp,
                        color = if (isPhoneFocused) CineGreen else Color.White.copy(alpha = 0.9f)
                    ),
                    shadowElevation = phoneCardElevation.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(phoneCardElevation.dp, RoundedCornerShape(26.dp))
                        .testTag("auth_phone_card")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Phone",
                            tint = if (isPhoneFocused) CineGreen else Color(0xFF64748B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        TextField(
                            value = phoneInput,
                            onValueChange = {
                                phoneInput = it
                                errorMessage = null
                            },
                            textStyle = TextStyle(
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            placeholder = {
                                Text(
                                    text = "Phone Number",
                                    color = Color(0xFF64748B),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                disabledTextColor = Color.Black,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = Color.Black
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { isPhoneFocused = it.isFocused }
                                .testTag("auth_phone_input")
                        )
                    }
                }

                // Error message (clean & minimal)
                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Clean Continue Button
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        if (fullNameInput.trim().isBlank()) {
                            errorMessage = "Please enter your name"
                            return@Button
                        }
                        if (phoneInput.trim().length < 6) {
                            errorMessage = "Please enter a valid phone number"
                            return@Button
                        }

                        isAuthenticating = true
                        errorMessage = null

                        viewModel.signInWithPhoneAndName(
                            context = context,
                            name = fullNameInput.trim(),
                            phoneNumber = phoneInput.trim()
                        ) { success, msg ->
                            isAuthenticating = false
                            if (success) {
                                coroutineScope.launch {
                                    delay(200)
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    onAuthSuccess()
                                }
                            } else {
                                errorMessage = msg
                            }
                        }
                    },
                    enabled = !isAuthenticating,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CineGreen,
                        disabledContainerColor = CineGreen.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("auth_submit_phone_button")
                ) {
                    if (isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Continue",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Minimal Skip option
                TextButton(
                    onClick = onSkipGuest,
                    modifier = Modifier.testTag("auth_guest_button")
                ) {
                    Text(
                        text = "Skip",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
