package com.connort6.expensemonitor.ui.views

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update


class IconPickerViewModel : ViewModel() {

    private val _pickerResult = MutableStateFlow(PickerResult())
    val pickerResult = _pickerResult.asStateFlow()

    private val _iconFilterRegex = MutableStateFlow(Regex(".+"))
    val iconRegexFilter = _iconFilterRegex.asStateFlow()

    fun updateResult(result: PickerResult) {
        _pickerResult.update { result }
    }

    fun cleanResult() {
        _pickerResult.update { PickerResult() }
    }

    fun updateRegex(regex: Regex) {
        _iconFilterRegex.update { regex }
    }
}