1. **啟動與初始化 (MainActivity)**  
   • **進入點**：使用者點擊 App icon，系統啟動 MainActivity，執行 onCreate。  
   • **依賴注入 (Dependency Injection)**：  
     ◦ 在 onCreate 中手動建立了 `StockRepositoryImpl` (資料層)。  
     ◦ 建立了 `SyncStockDataUseCase` (負責下載) 與 `GetStockListUseCase` (負責讀取) (領域層)。  
     ◦ 最後建立 `MainViewModelFactory` 並透過它取得 `MainViewModel` (UI 控制層)。  
   • **設置 UI**：呼叫 `setContent`，將 `MainScreen` 放入畫面中。

2. **UI 渲染與觸發下載 (MainScreen)**  
   • **畫面建構**：Compose 開始繪製 `MainScreen`。  
     此時 UI 透過 `collectAsState` 觀察 ViewModel 中的 `status` 和 `stockList`。初始狀態 `stockList` 為空。  
   • **觸發下載 (關鍵點)**：  
     ◦ 程式碼中有 `LaunchedEffect(Unit)` 區塊。  
     ◦ 當 `MainScreen` 第一次被放入畫面時，這個區塊會自動執行一次。  
     ◦ 它呼叫了 `viewModel.downloadData()`。

3. **ViewModel 處理邏輯 (MainViewModel)**  
   • **防止重複讀取**：`downloadData()` 會先檢查 `isDataLoaded` 變數。  
     如果已經載入過（例如手機旋轉導致 Activity 重建，但 ViewModel 還活著），就會直接跳過，不會重新下載。  
   • **啟動協程**：如果是第一次載入，ViewModel 會開啟一個 Coroutine (協程) 呼叫 UseCase。

4. **資料下載與快取 (Repository & Network)**  
   • **平行下載**：`SyncStockDataUseCase` 呼叫 Repository。  
     Repository 使用 `async/awaitAll` 同時向證交所發出三個 API 請求：  
     i. `BWIBBU_ALL` (本益比等資訊)  
     ii. `STOCK_DAY_AVG_ALL` (月平均價)  
     iii. `STOCK_DAY_ALL` (成交量、漲跌幅)  
   • **寫入快取**：資料下載成功後，Retrofit 取得的 JSON 串流會直接被寫入 App 的 `cacheDir` (內部儲存空間) 中的三個 `.json` 檔案。

5. **資料讀取與合併 (Data Merging)**  
   • **載入資料**：下載完成後，ViewModel 接著呼叫 `loadData()` -> `GetStockListUseCase`。  
   • **解析 JSON**：Repository 從快取資料夾讀取那三個 `.json` 檔案，並用 Gson 解析成物件列表。  
   • **資料合併**：Repository 透過 **股票代號 (Code)** 作為 Key，將三份資料合併成一個完整的 `Stock` 物件列表 (包含價格、漲跌、本益比等所有欄位)。

6. **更新 UI (Recomposition)**  
   • **狀態更新**：ViewModel 拿到合併後的 `List<Stock>`，將其更新到 `_stockList` (`StateFlow`)。  
     同時將 `isDataLoaded` 設為 true。  
   • **重組 (Recomposition)**：因為 `MainScreen` 正在觀察 `stockList`，當資料變更時，Compose 會自動觸發 UI 更新。  
   • **列表渲染**：  
     ◦ `LazyColumn` 接收到新的股票清單。  
     ◦ 針對每一筆股票資料繪製 `StockItem`。  
     ◦ 在 `StockItem` 中，根據數值大小決定文字顏色 (例如：收盤價 > 月均價顯示紅色，反之綠色)。
