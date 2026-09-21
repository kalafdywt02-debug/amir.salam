package com.example.commandcenter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.commandcenter.data.Rule
import com.example.commandcenter.data.RuleRepository
import com.example.commandcenter.domain.RuleEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AmirSalamViewModel(private val repo: RuleRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    val search: StateFlow<String> = query

    val rules: StateFlow<List<Rule>> = repo.rules.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val serviceOn: StateFlow<Boolean> = repo.serviceOn.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    fun setServiceOn(value: Boolean) { viewModelScope.launch { repo.setServiceOn(value) } }

    val filteredRules: StateFlow<List<Rule>> = combine(rules, query) { rules, q ->
        if (q.isBlank()) rules else rules.filter {
            it.source.contains(q, true) || it.replacement.contains(q, true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init { viewModelScope.launch { repo.initializeIfEmpty() } }

    fun setQuery(value: String) { query.value = value }

    fun add(source: String, replacement: String, exact: Boolean, sensitive: Boolean) {
        if (source.isBlank() || replacement.isBlank()) return
        viewModelScope.launch {
            val nextId = (rules.value.maxOfOrNull { it.id } ?: 0L) + 1
            repo.save(rules.value + Rule(nextId, RuleEngine.normalize(source.trim()), replacement.trim(), true, exact, sensitive))
        }
    }

    fun toggle(rule: Rule) = update(rule.copy(enabled = !rule.enabled))
    fun update(rule: Rule) = viewModelScope.launch { repo.save(rules.value.map { if (it.id == rule.id) rule else it }) }
    fun delete(rule: Rule) = viewModelScope.launch { repo.save(rules.value.filterNot { it.id == rule.id }) }
    fun duplicate(rule: Rule) = viewModelScope.launch {
        val id = (rules.value.maxOfOrNull { it.id } ?: 0L) + 1
        repo.save(rules.value + rule.copy(id = id))
    }

    fun transform(text: String): String = RuleEngine.replace(text, rules.value)
}
