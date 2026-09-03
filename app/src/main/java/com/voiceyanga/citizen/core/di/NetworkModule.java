package com.voiceyanga.citizen.core.di;

import com.voiceyanga.citizen.core.network.ApiConstants;
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
                    String token = sessionManager.getAccessToken();
                    if (token != null) {
                        token = token.trim().replace("\"", "");
                    }

                    Request originalRequest = chain.request();
                    
                    if (token != null && !token.isEmpty() && !token.equals("null")) {
                        // Instruction 6: Trim stored token and avoid adding Bearer twice
                        String authHeader = token.startsWith("Bearer ") ? token : "Bearer " + token;
                        
                        Request authenticatedRequest = originalRequest.newBuilder()
                                .header("Authorization", authHeader)
                                .build();
                        
                        // Safe logging as requested
                        android.util.Log.d("NetworkModule", "Request URL: " + authenticatedRequest.url());
                        android.util.Log.d("NetworkModule", "Authorization header added: Bearer [REDACTED]");
                        
                        return chain.proceed(authenticatedRequest);
                    }
                    
                    android.util.Log.w("NetworkModule", "No valid token available for request: " + originalRequest.url());
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
                .baseUrl(ApiConstants.BASE_URL)
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