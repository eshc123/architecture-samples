/*
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.architecture.blueprints.todoapp.taskdetail

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.android.architecture.blueprints.todoapp.Event
import com.example.android.architecture.blueprints.todoapp.R
import com.example.android.architecture.blueprints.todoapp.data.Result
import com.example.android.architecture.blueprints.todoapp.data.Result.Success
import com.example.android.architecture.blueprints.todoapp.data.Task
import com.example.android.architecture.blueprints.todoapp.domain.ActivateTaskUseCase
import com.example.android.architecture.blueprints.todoapp.domain.CompleteTaskUseCase
import com.example.android.architecture.blueprints.todoapp.domain.DeleteTaskUseCase
import com.example.android.architecture.blueprints.todoapp.domain.GetTaskUseCase
import com.example.android.architecture.blueprints.todoapp.util.EspressoIdlingResource
import com.example.android.architecture.blueprints.todoapp.util.wrapEspressoIdlingResource
import kotlinx.coroutines.launch

/**
 * Listens to user actions from the list item in ([TasksFragment]) and redirects them to the
 * Fragment's actions listener.
 */
class TaskDetailViewModel(
    private val getTaskUseCase: GetTaskUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val completeTaskUseCase: CompleteTaskUseCase,
    private val activateTaskUseCase: ActivateTaskUseCase

) : ViewModel() {

    private val _task = MutableLiveData<Task>()
    val task: LiveData<Task> = _task

    private val _isDataAvailable = MutableLiveData<Boolean>()
    val isDataAvailable: LiveData<Boolean> = _isDataAvailable

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _editTaskCommand = MutableLiveData<Event<Unit>>()
    val editTaskCommand: LiveData<Event<Unit>> = _editTaskCommand

    private val _deleteTaskCommand = MutableLiveData<Event<Unit>>()
    val deleteTaskCommand: LiveData<Event<Unit>> = _deleteTaskCommand

    private val _snackbarText = MutableLiveData<Event<Int>>()
    val snackbarMessage: LiveData<Event<Int>> = _snackbarText

    // This LiveData depends on another so we can use a transformation.
    val completed: LiveData<Boolean> = Transformations.map(_task) { input: Task? ->
        input?.isCompleted ?: false
    }

    val taskId: String?
        get() = _task.value?.id

    fun deleteTask() = viewModelScope.launch {
        taskId?.let {
            deleteTaskUseCase(it)
            _deleteTaskCommand.value = Event(Unit)
        }
    }

    fun editTask() {
        _editTaskCommand.value = Event(Unit)
    }

    fun setCompleted(completed: Boolean) = viewModelScope.launch {
        val task = _task.value ?: return@launch
        if (completed) {
            completeTaskUseCase(task)
            showSnackbarMessage(R.string.task_marked_complete)
        } else {
            activateTaskUseCase(task)
            showSnackbarMessage(R.string.task_marked_active)
        }
    }

    fun start(taskId: String?) {
        _dataLoading.value = true

        wrapEspressoIdlingResource {
            viewModelScope.launch {
                if (taskId != null) {
                    getTaskUseCase(taskId, false).let { result ->
                        if (result is Success) {
                            onTaskLoaded(result.data)
                        } else {
                            onDataNotAvailable(result)
                        }
                    }
                }
                _dataLoading.value = false
                EspressoIdlingResource.decrement() // Set app as idle.
            }
        }
    }

    private fun setTask(task: Task?) {
        this._task.value = task
        _isDataAvailable.value = task != null
    }

    private fun onTaskLoaded(task: Task) {
        setTask(task)
    }

    private fun onDataNotAvailable(result: Result<Task>) {
        _task.value = null
        _isDataAvailable.value = false
    }

    fun onRefresh() {
        taskId?.let { start(it) }
    }

    private fun showSnackbarMessage(@StringRes message: Int) {
        _snackbarText.value = Event(message)
    }
}