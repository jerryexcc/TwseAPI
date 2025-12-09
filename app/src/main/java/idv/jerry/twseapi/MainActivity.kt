package idv.jerry.twseapi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import idv.jerry.twseapi.data.repository.StockRepositoryImpl
import idv.jerry.twseapi.domain.model.Stock
import idv.jerry.twseapi.domain.usecase.GetStockListUseCase
import idv.jerry.twseapi.domain.usecase.SyncStockDataUseCase
import idv.jerry.twseapi.ui.theme.TwseAPITheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Dependency Injection (Manual)
        val cacheDir = cacheDir
        val repository = StockRepositoryImpl(cacheDir)
        val syncStockDataUseCase = SyncStockDataUseCase(repository)
        val getStockListUseCase = GetStockListUseCase(repository)
        
        val viewModelFactory = MainViewModelFactory(syncStockDataUseCase, getStockListUseCase)
        
        setContent {
            TwseAPITheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        factory = viewModelFactory,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    factory: MainViewModelFactory,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel(factory = factory)
) {
    val status by viewModel.status.collectAsState()
    val stockList by viewModel.stockList.collectAsState()
    
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedStock by remember { mutableStateOf<Stock?>(null) }
    
    // Auto download on launch
    LaunchedEffect(Unit) {
        viewModel.downloadData()
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Filter Icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = { showBottomSheet = true },
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Filter"
                    )
                }
            }
            
            StockList(stockList, onStockClick = { selectedStock = it })
        }
        
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                },
                sheetState = sheetState
            ) {
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(
                        text = "排序方式",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    ListItem(
                        headlineContent = { Text("依股票代號降序") },
                        modifier = Modifier.clickable {
                            viewModel.sortList(SortOption.CODE, ascending = false)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showBottomSheet = false }
                        }
                    )

                    ListItem(
                        headlineContent = { Text("依股票代號升序") },
                        modifier = Modifier.clickable {
                            viewModel.sortList(SortOption.CODE, ascending = true)
                            scope.launch { sheetState.hide() }.invokeOnCompletion { showBottomSheet = false }
                        }
                    )
                }
            }
        }

        if (selectedStock != null) {
            AlertDialog(
                onDismissRequest = { selectedStock = null },
                title = { Text(text = "${selectedStock!!.name} (${selectedStock!!.code})") },
                text = {
                    Column {
                        Text("本益比: ${selectedStock!!.peRatio ?: "-"}")
                        Text("殖利率(%): ${selectedStock!!.dividendYield ?: "-"}")
                        Text("股價淨值比: ${selectedStock!!.pbRatio ?: "-"}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedStock = null }) {
                        Text("確定")
                    }
                }
            )
        }
    }
}

@Composable
fun StockList(stocks: List<Stock>, onStockClick: (Stock) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(stocks) { stock ->
            StockItem(stock, onClick = { onStockClick(stock) })
            HorizontalDivider()
        }
    }
}

@Composable
fun StockItem(stock: Stock, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        // Stock Code (Top Left)
        Text(
            text = stock.code,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        
        // Stock Name (Below Code)
        Text(
            text = stock.name,
            fontSize = 24.sp,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 8.dp)
        )
        
        // Determine Closing Price Color
        val closingPriceVal = stock.closingPrice?.replace(",", "")?.toDoubleOrNull()
        val monthlyAvgVal = stock.monthlyAveragePrice?.replace(",", "")?.toDoubleOrNull()
        val closingPriceColor = when {
            closingPriceVal != null && monthlyAvgVal != null -> {
                if (closingPriceVal > monthlyAvgVal) Color.Red
                else if (closingPriceVal < monthlyAvgVal) Color.Green
                else Color.Unspecified
            }
            else -> Color.Unspecified
        }

        // 4x3 Grid (Centered)
        Column(
             modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
             horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text(text = "開盤價", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(text = stock.openingPrice ?: "-", textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text(text = "收盤價", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(text = stock.closingPrice ?: "-", textAlign = TextAlign.Center, color = closingPriceColor, modifier = Modifier.weight(1f))
            }
            // Row 2
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text(text = "最高價", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(text = stock.highestPrice ?: "-", textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Text(text = "最低價", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(text = stock.lowestPrice ?: "-", textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
            // Row 3
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Text(text = "漲跌價差", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                
                val change = stock.change
                val changeColor = if (change != null) {
                    if (change.startsWith("-") || change.startsWith("−")) Color.Green 
                    else if (change == "0.0000" || change == "0.00" || change == "0") Color.Gray 
                    else Color.Red
                } else Color.Unspecified
                
                Text(text = change ?: "-", textAlign = TextAlign.Center, color = changeColor, modifier = Modifier.weight(1f))
                
                Text(text = "月平均價", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(text = stock.monthlyAveragePrice ?: "-", textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        
        // Bottom Info
        // 成交筆數(title) 成交筆數(數值) 成交股數(title) 成交股數(數值) 成交金額(title) 成交金額(數值)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "成交筆數", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stock.transaction ?: "-", fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "成交股數", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stock.tradeVolume ?: "-", fontSize = 12.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "成交金額", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stock.tradeValue ?: "-", fontSize = 12.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    TwseAPITheme {
        // Preview logic
    }
}
