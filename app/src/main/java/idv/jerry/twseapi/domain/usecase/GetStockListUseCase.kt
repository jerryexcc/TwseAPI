package idv.jerry.twseapi.domain.usecase

import idv.jerry.twseapi.domain.model.Stock
import idv.jerry.twseapi.domain.repository.IStockRepository

class GetStockListUseCase(private val repository: IStockRepository) {
    operator fun invoke(): List<Stock> {
        return repository.loadStockData()
    }
}
