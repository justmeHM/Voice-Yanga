package com.voiceyanga.citizen.core.di;

import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.AuthResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import javax.inject.Provider;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Module
@InstallIn(SingletonComponent.class)
public class NetworkModule {

    private static final String BASE_URL = "http://192.168.43.35:3000/api/v1/";

    @Provides
    @Singleton
    public HttpLoggingInterceptor provideLoggingInterceptor() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return logging;
    }

    @Provides
    @Singleton
    public OkHttpClient provideOkHttpClient(
            HttpLoggingInterceptor loggingInterceptor, 
            SessionManager sessionManager,
            Provider<ApiService> apiServiceProvider) {
        
        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(chain -> {
                    String token = sessionManager.getAccessToken();
                    Request.Builder builder = chain.request().newBuilder();
                    if (token != null && !token.isEmpty()) {
                        builder.addHeader("Authorization", "Bearer " + token);
                    }
                    return chain.proceed(builder.build());
                })
                .authenticator((route, response) -> {
                    // This is called when we get a 401
                    String refreshToken = sessionManager.getRefreshToken();
                    if (refreshToken == null) return null;

                    synchronized (this) {
                        // Double check if token was already refreshed by another thread
                        String currentToken = sessionManager.getAccessToken();
                        String responseToken = response.request().header("Authorization");
                        if (responseToken != null && !responseToken.equals("Bearer " + currentToken)) {
                            // Already refreshed
                            return response.request().newBuilder()
                                    .header("Authorization", "Bearer " + currentToken)
                                    .build();
                        }

                        try {
                            Map<String, String> body = new HashMap<>();
                            body.put("refreshToken", refreshToken);
                            retrofit2.Response<AuthResponse> refreshResponse = apiServiceProvider.get().refreshToken(body).execute();
                            
                            if (refreshResponse.isSuccessful() && refreshResponse.body() != null) {
                                AuthResponse newAuth = refreshResponse.body();
                                sessionManager.saveTokens(newAuth.getAccessToken(), newAuth.getRefreshToken());
                                return response.request().newBuilder()
                                        .header("Authorization", "Bearer " + newAuth.getAccessToken())
                                        .build();
                            } else {
                                sessionManager.clearSession();
                                return null;
                            }
                        } catch (IOException e) {
                            return null;
                        }
                    }
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }

    @Provides
    @Singleton
    public ApiService provideApiService(Retrofit retrofit) {
        return retrofit.create(ApiService.class);
    }
}