package shanshin.gleb.diplom;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.View;
import android.widget.SearchView;
import android.widget.TextView;


import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;
import shanshin.gleb.diplom.api.StocksApi;
import shanshin.gleb.diplom.api.TransactionApi;
import shanshin.gleb.diplom.model.Stock;
import shanshin.gleb.diplom.model.TransactionStock;
import shanshin.gleb.diplom.responses.DefaultErrorResponse;
import shanshin.gleb.diplom.responses.StocksResponse;
import shanshin.gleb.diplom.responses.TransactionHistoryResponse;

public class StockSearchActivity extends AppCompatActivity implements StockContatiner {
    SearchView searchView;
    TextView titleText;
    StocksApi stocksApi;
    TransactionApi transactionApi;
    RecyclerView stocksView;
    StockAdapter stockAdapter;
    String lastQuery = "";
    BottomSheetDialog bottomSheetDialog;
    final int DEFAULT_COUNT = 50;
    static final int TRANSACTION_HISTORY = 1;
    static final int SEARCH_STOCKS = 2;
    int activityCode;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        initializeViews();
        updateStockList("");
    }

    private void initializeViews() {
        searchView = findViewById(R.id.searchView);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                titleText.setVisibility(View.GONE);
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                titleText.setVisibility(View.VISIBLE);
                return false;
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                updateStockList(s);
                return false;
            }
        });

        titleText = findViewById(R.id.title);

        activityCode = getIntent().getIntExtra("activityCode", 0);
        if (activityCode == TRANSACTION_HISTORY) {
            titleText.setText(getString(R.string.transaction_history_title));
            transactionApi = App.getInstance().getRetrofit().create(TransactionApi.class);
        } else if (activityCode == SEARCH_STOCKS) {
            titleText.setText(getString(R.string.stock_search_title));
            stocksApi = App.getInstance().getRetrofit().create(StocksApi.class);
        } else
            throw new RuntimeException(getString(R.string.wrong_code_exception));


        stocksView = findViewById(R.id.stocksView);
        stockAdapter = new StockAdapter(this, new ArrayList<Stock>(), new ArrayList<TransactionStock>(), activityCode);
        stocksView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        stocksView.setAdapter(stockAdapter);

        bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_dialog_layout, null);
        bottomSheetDialog.setContentView(sheetView);

    }

    private void updateStockList(String query) {
        int itemCount = stockAdapter.getItemCount();
        int lastLength = lastQuery.length();

        if (activityCode == SEARCH_STOCKS) {
            stockAdapter.setStocks(App.getInstance().getUtils().localQuery(query, stockAdapter.getStocks()));
        } else {
            stockAdapter.setHistoryStocks(App.getInstance().getUtils().localTransactionQuery(query, stockAdapter.getHistoryStocks()));
        }
        lastQuery = query;

        if (lastLength < query.length() && itemCount < DEFAULT_COUNT) {
            return;
        }
        if (activityCode == TRANSACTION_HISTORY)
            transactionApi.getTransactionHistory(App.getInstance().getDataHandler().getAccessToken(), query, DEFAULT_COUNT, 0).enqueue(new Callback<TransactionHistoryResponse>() {
                @Override
                public void onResponse(Call<TransactionHistoryResponse> call, Response<TransactionHistoryResponse> response) {
                    try {
                        handleHistoryResponse(response);
                    } catch (Exception ignored) {
                        Log.d("tagged", ignored.toString());
                    }
                }

                @Override
                public void onFailure(Call<TransactionHistoryResponse> call, Throwable t) {
                    Log.d("tagged", t.toString());
                }
            });
        else
            stocksApi.getStocksWithOffset(App.getInstance().getDataHandler().getAccessToken(), query, DEFAULT_COUNT,0).enqueue(new Callback<StocksResponse>() {
                @Override
                public void onResponse(Call<StocksResponse> call, Response<StocksResponse> response) {
                    try {
                        handleStocksResponse(response);
                    } catch (Exception ignored) {
                        Log.d("tagged", response.raw().toString());
                    }
                }

                @Override
                public void onFailure(Call<StocksResponse> call, Throwable t) {

                }
            });
    }

    private boolean handleResponseErrors(boolean isSuccessful, ResponseBody errorBody) throws IOException {
        if (!isSuccessful && errorBody != null) {
            Converter<ResponseBody, DefaultErrorResponse> errorConverter =
                    App.getInstance().getRetrofit().responseBodyConverter(DefaultErrorResponse.class, new Annotation[0]);
            DefaultErrorResponse errorResponse = errorConverter.convert(errorBody);
            App.getInstance().getUtils().showError(errorResponse.message);
            return false;
        } else {
            return true;
        }
    }

    private void handleHistoryResponse(Response<TransactionHistoryResponse> response) throws IOException {
        if (handleResponseErrors(response.isSuccessful(), response.errorBody())) {
            TransactionHistoryResponse transactionResponse = response.body();
            stockAdapter.setHistoryStocks(transactionResponse.items);
        }
    }

    private void handleStocksResponse(Response<StocksResponse> response) throws IOException {
        if (handleResponseErrors(response.isSuccessful(), response.errorBody())) {
            StocksResponse stocksResponse = response.body();
            stockAdapter.setStocks(stocksResponse.items);
        }
    }

    @Override
    public void stockClicked(final Stock stock) {
        App.getInstance().getDialogHandler().initializeDialog(bottomSheetDialog, stock, true, this);
    }

    @Override
    public void requestSuccess() {
        updateStockList(lastQuery);

    }

    @Override
    public void requestError() {

    }

}
