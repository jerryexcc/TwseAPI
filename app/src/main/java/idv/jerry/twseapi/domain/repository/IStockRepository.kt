package idv.jerry.twseapi.domain.repository

import idv.jerry.twseapi.domain.model.Stock
import java.io.File

interface IStockRepository {
    suspend fun fetchAndSaveStockData(): Result<File>
    fun loadStockData(): List<Stock>
}
