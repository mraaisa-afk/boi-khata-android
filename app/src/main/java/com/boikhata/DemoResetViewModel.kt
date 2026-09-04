package com.boikhata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boikhata.core.database.DemoResetter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DemoResetViewModel @Inject constructor(
    private val resetter: DemoResetter,
) : ViewModel() {
    fun reset(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            resetter.reset()
            onComplete()
        }
    }
}
