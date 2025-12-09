package idv.jerry.twseapi.domain.usecase

import idv.jerry.twseapi.domain.repository.IStockRepository
import java.io.File

class SyncStockDataUseCase(private val repository: IStockRepository) {
    suspend operator fun invoke(): Result<File> {
        return repository.fetchAndSaveStockData()
    }
}
