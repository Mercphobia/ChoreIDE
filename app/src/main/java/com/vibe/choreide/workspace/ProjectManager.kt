package com.vibe.choreide.workspace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class ProjectManager : ViewModel() {

    private val repository = FileRepository()

    data class UiState(
        val projects: List<File> = emptyList(),
        val currentProject: File? = null,
        val currentFile: File? = null,
        val currentFileContent: String = "",
        val isLoading: Boolean = false,
        val error: String? = null
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun refreshProjects() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val projects = repository.listProjects()
            _state.value = _state.value.copy(projects = projects, isLoading = false)
        }
    }

    fun openProject(project: File) {
        _state.value = _state.value.copy(currentProject = project)
    }

    fun openFile(file: File) {
        viewModelScope.launch {
            repository.readFile(file)
                .onSuccess { content ->
                    _state.value = _state.value.copy(currentFile = file, currentFileContent = content)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(error = e.message)
                }
        }
    }

    fun saveCurrentFile(content: String) {
        val file = _state.value.currentFile ?: return
        viewModelScope.launch {
            repository.writeFile(file, content)
                .onSuccess {
                    _state.value = _state.value.copy(currentFileContent = content)
                }
                .onFailure { e ->
                    _state.value = _state.value.copy(error = e.message)
                }
        }
    }

    fun createProject(name: String, template: AospTemplate, onDone: (Result<File>) -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = ProjectGenerator.generate(name, template)
            _state.value = _state.value.copy(isLoading = false)
            result.onSuccess { refreshProjects() }
            onDone(result)
        }
    }
}
