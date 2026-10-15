package com.swan1127.repland.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swan1127.repland.data.profile.LocalPersonalProfileStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PersonalProfileOperation(val loading: Boolean = true, val working: Boolean = false, val error: String? = null)

class PersonalProfileViewModel(private val store: LocalPersonalProfileStore) : ViewModel() {
    val profile = store.profile
    private val mutableOperation = MutableStateFlow(PersonalProfileOperation())
    val operation = mutableOperation.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            try {
                store.load()
                mutableOperation.value = PersonalProfileOperation(loading = false)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                mutableOperation.value = PersonalProfileOperation(loading = false, error = "个人资料暂时无法读取，请重试")
            }
        }
    }

    fun saveUsername(value: String, onSaved: () -> Unit) = change("用户名未保存，请重试", onSaved) { store.saveUsername(value) }
    fun saveAvatar(uri: Uri) = change("这张图片暂时无法使用，请重新选择") { store.saveAvatar(uri) }
    fun removeAvatar() = change("头像未能重置，请重试") { store.removeAvatar() }
    fun pickerFailed() { mutableOperation.value = operation.value.copy(error = "照片选择器暂时无法打开，请重试") }
    fun dismissError() { mutableOperation.value = operation.value.copy(error = null) }

    private fun change(message: String, onSaved: () -> Unit = {}, action: suspend () -> Unit) {
        if (operation.value.working || operation.value.loading || profile.value == null) return
        mutableOperation.value = PersonalProfileOperation(loading = false, working = true)
        viewModelScope.launch {
            try {
                action()
                mutableOperation.value = PersonalProfileOperation(loading = false)
                onSaved()
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                mutableOperation.value = PersonalProfileOperation(loading = false, error = message)
            }
        }
    }

    class Factory(private val store: LocalPersonalProfileStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(PersonalProfileViewModel::class.java))
            return PersonalProfileViewModel(store) as T
        }
    }
}
