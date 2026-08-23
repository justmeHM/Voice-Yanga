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
            SessionManager sessionManager) {
        
        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(chain -> {
                    String rawToken = sessionManager.getAccessToken();
                    String cleanToken = (rawToken == null) ? null : rawToken.replace("\"", "")
                                     .replace("Bearer ", "")
                                     .trim();

                    Request originalRequest = chain.request();
                    
                    android.util.Log.d("NetworkModule", "Request URL: " + originalRequest.url());
                    android.util.Log.d("NetworkModule", "Authorization header exists: " + (cleanToken != null && !cleanToken.isEmpty()));

                    if (cleanToken != null && !cleanToken.isEmpty()) {
                        Request authenticatedRequest = originalRequest.newBuilder()
                                .header("Authorization", "Bearer " + cleanToken)
                                .build();
                        return chain.proceed(authenticatedRequest);
                    }
                    return chain.proceed(originalRequest);
                })
                .authenticator((route, response) -> {
                    if (responseCount(response) >= 2) {
                        return null;
                    }

                    // Check for specific backend error message
                    String bodyString;
                    try {
                        okhttp3.ResponseBody body = response.peekBody(Long.MAX_VALUE);
                        bodyString = body.string();
                    } catch (Exception ignored) {
                        bodyString = "";
                    }

                    if (bodyString.contains("Token is invalid or expired")) {
                        android.util.Log.e("NetworkModule", "Token expired. Clearing session.");
                        sessionManager.clearSession();
                        // Instructions say "log in again, save new 'token', and retry".
                        // In background sync, we return null to stop the loop and let the 
                        // app handle the cleared session.
                        return null;
                    }
                    return null;
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

    private int responseCount(Response response) {
        int count = 1;
        Response prior = response.priorResponse();
        while (prior != null) {
            count++;
            prior = prior.priorResponse();
        }
        return count;
    }
}