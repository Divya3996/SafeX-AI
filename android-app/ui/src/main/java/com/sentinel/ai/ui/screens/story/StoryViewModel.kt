package com.sentinel.ai.ui.screens.story

import androidx.lifecycle.ViewModel
import com.sentinel.ai.core.story.StoryController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StoryViewModel @Inject constructor(val controller: StoryController) : ViewModel()
