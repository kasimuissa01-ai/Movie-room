package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.MovieViewModel
import com.example.ui.theme.CineCardBorder
import com.example.ui.theme.CineGold
import com.example.ui.theme.CineGreen
import com.example.ui.theme.CineSurface
import com.example.ui.theme.CineSurfaceElevated
import com.example.ui.theme.CineTextMuted
import com.example.ui.theme.CineTextPrimary
import com.example.ui.theme.CineTextSecondary
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

    var isAuthenticating by remember { mutableStateOf(false) }
    var authSuccessAnimation by remember { mutableStateOf(false) }
    var showAccountChooserDialog by remember { mutableStateOf(false) }
    var isSignUpMode by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var authErrorMessage by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            isAuthenticating = true
            viewModel.handleGoogleSignInIntentResult(
                context = context,
                data = result.data,
                onComplete = { success, msg ->
                    isAuthenticating = false
                    if (success) {
                        authSuccessAnimation = true
                        coroutineScope.launch {
                            delay(350)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            onAuthSuccess()
                        }
                    } else {
                        if (msg != "Cancelled") {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        } else {
            isAuthenticating = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("google_auth_screen")
    ) {
        // 1. Wallpaper background
        Image(
            painter = painterResource(id = R.drawable.img_auth_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Subtle gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // 3. Optional dismiss / close button
        if (canDismiss) {
            IconButton(
                onClick = onSkipGuest,
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .testTag("auth_dismiss_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // 4. Foreground Content Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP SECTION: Header Typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (canDismiss) 16.dp else 40.dp)
            ) {
                Text(
                    text = "Welcome to the",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "movie universe",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // BOTTOM SECTION: Google Auth Button, Supabase Email Sign In, & Guest Entry
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 36.dp)
            ) {
                // Direct Google Authentication Button
                Surface(
                    onClick = {
                        if (!isAuthenticating) {
                            try {
                                val authManager = com.example.data.firebase.AuthenticationManager(context)
                                val intent = authManager.createSignInIntent()
                                googleSignInLauncher.launch(intent)
                            } catch (e: Throwable) {
                                isAuthenticating = true
                                viewModel.signInWithGoogleDirect(
                                    context = context,
                                    onComplete = { success, msg ->
                                        isAuthenticating = false
                                        if (success) {
                                            authSuccessAnimation = true
                                            coroutineScope.launch {
                                                delay(350)
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                onAuthSuccess()
                                            }
                                        } else {
                                            if (msg != "Cancelled") {
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = BorderStroke(2.dp, Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.6f))
                        .testTag("google_auth_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Connecting with Google...",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Jiunge na Google",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.3.sp
                                )
                                Text(
                                    text = "Continue with Google",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Supabase Email & Password Sign In / Sign Up Button
                Surface(
                    onClick = { showAccountChooserDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.85f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .testTag("email_auth_button")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Sign In",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ingia kwa Barua Pepe (Email Login)",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Guest / Skip Option
                TextButton(
                    onClick = onSkipGuest,
                    modifier = Modifier.testTag("auth_guest_button")
                ) {
                    Text(
                        text = "Explore as Guest / Ruka kwa sasa",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 6. Supabase Email Authentication Modal Dialog
        if (showAccountChooserDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAccountChooserDialog = false
                    authErrorMessage = null
                },
                containerColor = CineSurface,
                title = {
                    Text(
                        text = if (isSignUpMode) "Fungua Akaunti (Sign Up)" else "Ingia kwa Barua Pepe (Log In)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Jina Kamili (Display Name)", color = CineTextSecondary) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CineGreen,
                                    unfocusedBorderColor = CineCardBorder
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = {
                                emailInput = it
                                authErrorMessage = null
                            },
                            label = { Text("Barua Pepe (Email)", color = CineTextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CineGreen,
                                unfocusedBorderColor = CineCardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = {
                                passwordInput = it
                                authErrorMessage = null
                            },
                            label = { Text("Neno la Siri (Password)", color = CineTextSecondary) },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CineGreen,
                                unfocusedBorderColor = CineCardBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        authErrorMessage?.let { err ->
                            Text(
                                text = err,
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        TextButton(
                            onClick = {
                                isSignUpMode = !isSignUpMode
                                authErrorMessage = null
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = if (isSignUpMode) "Una akaunti tayari? Ingia hapa" else "Huna akaunti? Jisajili",
                                color = CineGreen,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (emailInput.isBlank() || passwordInput.isBlank()) {
                                authErrorMessage = "Tafadhali weka barua pepe na neno la siri"
                                return@Button
                            }
                            isAuthenticating = true
                            if (isSignUpMode) {
                                viewModel.signUpWithSupabase(
                                    email = emailInput.trim(),
                                    password = passwordInput,
                                    displayName = nameInput.trim()
                                ) { success, msg ->
                                    isAuthenticating = false
                                    if (success) {
                                        showAccountChooserDialog = false
                                        authSuccessAnimation = true
                                        coroutineScope.launch {
                                            delay(350)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            onAuthSuccess()
                                        }
                                    } else {
                                        authErrorMessage = msg
                                    }
                                }
                            } else {
                                viewModel.signInWithSupabase(
                                    email = emailInput.trim(),
                                    password = passwordInput
                                ) { success, msg ->
                                    isAuthenticating = false
                                    if (success) {
                                        showAccountChooserDialog = false
                                        authSuccessAnimation = true
                                        coroutineScope.launch {
                                            delay(350)
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            onAuthSuccess()
                                        }
                                    } else {
                                        authErrorMessage = msg
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CineGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isSignUpMode) "Jisajili" else "Ingia",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAccountChooserDialog = false }) {
                        Text("Funga", color = CineTextSecondary)
                    }
                }
            )
        }
    }
}
