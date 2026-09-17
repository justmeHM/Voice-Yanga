package com.voiceyanga.citizen.core.di;

import android.content.Context;
import android.content.Intent;
import com.voiceyanga.citizen.BuildConfig;
import com.voiceyanga.citizen.data.local.SessionManager;
import com.voiceyanga.citizen.data.remote.api.ApiService;
import com.voiceyanga.citizen.data.remote.dto.ApiEnvelope;
import com.voiceyanga.citizen.data.remote.dto.RefreshData;
import com.voiceyanga.citizen.data.remote.dto.RefreshRequest;
import com.voiceyanga.citizen.feature.auth.LoginActivity;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
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
            @ApplicationContext Context context,
            HttpLoggingInterceptor loggingInterceptor, 
            SessionManager sessionManager) {
        
        Interceptor bearerInterceptor = chain -> {
            Request original = chain.request();
            String path = original.url().encodedPath();

            // Do not attach access token to login, register, or refresh endpoints
            if (path.contains("/auth/login") || path.contains("/auth/register") || path.contains("/auth/refresh")) {
                return chain.proceed(original);
            }

            String host = original.url().host();
            String apiHost = android.net.Uri.parse(BuildConfig.API_BASE_URL).getHost();

            Request.Builder requestBuilder = original.newBuilder();
            
            if (host.equalsIgnoreCase(apiHost)) {
                requestBuilder.header("Accept", "application/json");
                String token = sessionManager.getAccessToken();
                if (token != null && !token.isEmpty()) {
                    android.util.Log.d("NetworkModule", "Attaching token to request: " + path);
                    requestBuilder.header("Authorization", "Bearer " + token);
                } else {
                    android.util.Log.w("NetworkModule", "Token is missing for request: " + path);
                }
            }
            
            Response response = chain.proceed(requestBuilder.build());
            if (!response.isSuccessful()) {
                android.util.Log.e("NetworkModule", "Request failed: " + path + " | Code: " + response.code());
            }
            return response;
        };

        Authenticator authenticator = new Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) throws IOException {
                // responseCount >= 2 prevents infinite retry loops
                if (responseCount(response) >= 2) {
                    return null;
                }

                // Only handle TOKEN_EXPIRED specifically
                String errorBody = "";
                try (ResponseBody peekBody = response.peekBody(1024)) {
                    errorBody = peekBody.string();
                } catch (Exception ignored) {}

                if (!errorBody.contains("TOKEN_EXPIRED")) {
                    return null;
                }

                synchronized (this) {
                    String currentToken = sessionManager.getAccessToken();
                    String requestToken = response.request().header("Authorization");

                    // Check if token was already refreshed by a concurrent request
                    if (requestToken != null && !requestToken.contains(currentToken)) {
                        return response.request().newBuilder()
                                .header("Authorization", "Bearer " + currentToken)
                                .build();
                    }

                    String refreshToken = sessionManager.getRefreshToken();
                    if (refreshToken == null || refreshToken.isEmpty()) {
                        handleSessionExpired(sessionManager, context);
                        return null;
                    }

                    // Attempt token refresh using a dedicated Retrofit instance to avoid interceptor recursion
                    Retrofit refreshRetrofit = new Retrofit.Builder()
                            .baseUrl(BuildConfig.API_BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    ApiService refreshApi = refreshRetrofit.create(ApiService.class);
                    retrofit2.Response<ApiEnvelope<RefreshData>> refreshResponse = 
                            refreshApi.refresh(new RefreshRequest(refreshToken)).execute();

                    if (refreshResponse.isSuccessful() && refreshResponse.body() != null && refreshResponse.body().success) {
                        RefreshData data = refreshResponse.body().data;
                        // Atomically save rotated tokens
                        sessionManager.saveTokens(data.accessToken, data.refreshToken);

                        return response.request().newBuilder()
                                .header("Authorization", "Bearer " + data.accessToken)
                                .build();
                    } else {
                        // Refresh failed (e.g. 400 or 401)
                        handleSessionExpired(sessionManager, context);
                        return null;
                    }
                }
            }
        };
        
        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(bearerInterceptor)
                .authenticator(authenticator)
                .followRedirects(true)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private void handleSessionExpired(SessionManager sessionManager, Context context) {
        sessionManager.clearSession();
        Intent intent = new Intent(context, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    @Provides
    @Singleton
    public Retrofit provideRetrofit(OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
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
