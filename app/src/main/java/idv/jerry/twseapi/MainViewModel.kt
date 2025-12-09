package idv.jerry.twseapi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import idv.jerry.twseapi.domain.model.Stock
import idv.jerry.twseapi.domain.usecase.GetStockListUseCase
import idv.jerry.twseapi.domain.usecase.SyncStockDataUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SortOption {
    DEFAULT, // Original order
    CODE,
    NAME,
    PE_RATIO,
    DIVIDEND_YIELD,
    PB_RATIO,
    CLOSING_PRICE,
    CHANGE,
    TRANSACTION,
    TRADE_VOLUME,
    TRADE_VALUE
}

class MainViewModel(
    private val syncStockDataUseCase: SyncStockDataUseCase,
    private val getStockListUseCase: GetStockListUseCase
) : ViewModel() {

    private val _status = MutableStateFlow("Waiting to download...")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _stockList = MutableStateFlow<List<Stock>>(emptyList())
    val stockList: StateFlow<List<Stock>> = _stockList.asStateFlow()

    private var originalList: List<Stock> = emptyList()
    
    // Flag to track if data has been loaded initially
    private var isDataLoaded = false

    fun downloadData() {
        if (isDataLoaded) return // Prevent re-download if already loaded (e.g., config change)
        
        _status.value = "Downloading..."
        viewModelScope.launch {
            val result = syncStockDataUseCase()
            if (result.isSuccess) {
                _status.value = "Downloaded to: ${result.getOrNull()?.absolutePath}"
                loadData()
            } else {
                _status.value = "Error: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                getStockListUseCase()
            }
            originalList = list
            _stockList.value = list
            if (list.isNotEmpty()) {
                _status.value = "Loaded ${list.size} stocks"
                isDataLoaded = true
            }
        }
    }

    fun sortList(option: SortOption, ascending: Boolean = false) {
        val list = originalList
        if (list.isEmpty()) return

        val sortedList = when (option) {
            SortOption.DEFAULT -> list
            SortOption.CODE -> list.sortedBy { it.code } // Code is usually sorted ascending naturally, but we adhere to requested order
            SortOption.NAME -> list.sortedBy { it.name }
            SortOption.PE_RATIO -> list.sortedBy { it.peRatio?.toDoubleOrNull() ?: 0.0 }
            SortOption.DIVIDEND_YIELD -> list.sortedBy { it.dividendYield?.toDoubleOrNull() ?: 0.0 }
            SortOption.PB_RATIO -> list.sortedBy { it.pbRatio?.toDoubleOrNull() ?: 0.0 }
            SortOption.CLOSING_PRICE -> list.sortedBy { it.closingPrice?.replace(",", "")?.toDoubleOrNull() ?: 0.0 }
            SortOption.CHANGE -> list.sortedBy { 
                 val s = it.change?.replace("+", "")?.replace("−", "-") ?: "0"
                 s.toDoubleOrNull() ?: 0.0
            }
            SortOption.TRANSACTION -> list.sortedBy { it.transaction?.replace(",", "")?.toLongOrNull() ?: 0 }
            SortOption.TRADE_VOLUME -> list.sortedBy { it.tradeVolume?.replace(",", "")?.toLongOrNull() ?: 0 }
            SortOption.TRADE_VALUE -> list.sortedBy { it.tradeValue?.replace(",", "")?.toLongOrNull() ?: 0 }
        }

        _stockList.value = if (ascending) sortedList else sortedList.reversed()
        // Note: For DEFAULT, we just return original list.
        // If option is DEFAULT, ascending flag might not matter, or we treat it as is.
        if (option == SortOption.DEFAULT) {
             _stockList.value = originalList
        }
    }
}

class MainViewModelFactory(
    private val syncStockDataUseCase: SyncStockDataUseCase,
    private val getStockListUseCase: GetStockListUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(syncStockDataUseCase, getStockListUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
