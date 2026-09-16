package com.mazur.tarot.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.mazur.tarot.TarotApplication

@Composable
fun tarotApp(): TarotApplication = LocalContext.current.applicationContext as TarotApplication
