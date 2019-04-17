package shanshin.gleb.diplom;

import android.app.Application;
import android.content.Intent;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import shanshin.gleb.diplom.api.AuthApi;
import shanshin.gleb.diplom.model.RefreshToken;
import shanshin.gleb.diplom.responses.AuthSuccessResponse;

public class App extends Application {
    private static App instance;
    String server;
    Gson gson;
    private Retrofit retrofit;
    private SharedPreferencesHandler sharedPrefsHandler;
    private GeneralUtils utils;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        server = getResources().getString(R.string.server_url);
        gson = new GsonBuilder().create();
        sharedPrefsHandler = new SharedPreferencesHandler();
        utils = new GeneralUtils();
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(new ExpiredTokenInterceptor())
                .build();

        retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
                .baseUrl(server)
                .client(okHttpClient)
                .build();

    }

    public GeneralUtils getUtils() {
        return utils;
    }

    public static App getInstance() {
        return instance;
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    public void initializeDialog(BottomSheetDialog bottomSheetDialog, String name, String buttonText) {
        TextView stockName = bottomSheetDialog.findViewById(R.id.stock_name);
        Button button = bottomSheetDialog.findViewById(R.id.dialogButton);
        stockName.setText(name);
        button.setText(buttonText);
    }


    public void updateTokens() {
        AuthApi authApi = getInstance().getRetrofit().create(AuthApi.class);
        try {
            Response<AuthSuccessResponse> response = authApi.refreshToken(App.getInstance().getDataHandler().getAccessToken(), new RefreshToken(App.getInstance().getDataHandler().getRefreshToken())).execute();
            if (response.code() != 401) {
                AuthSuccessResponse successResponse = response.body();
                App.getInstance().getDataHandler().saveTokens(successResponse.accessToken, successResponse.refreshToken);
            } else {
                startActivity(new Intent(this, MainActivity.class));
                App.getInstance().getDataHandler().saveTokens("", "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SharedPreferencesHandler getDataHandler() {
        return sharedPrefsHandler;
    }
}
