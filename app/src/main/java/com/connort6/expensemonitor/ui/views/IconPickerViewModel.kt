package com.connort6.expensemonitor.ui.views

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class IconPickerViewModel : ViewModel() {

    private val _pickerResult = MutableStateFlow<PickerResult?>(null)
    val pickerResult = _pickerResult.asStateFlow()

    fun updateResult(result: PickerResult) {
        _pickerResult.update { result }
    }
}