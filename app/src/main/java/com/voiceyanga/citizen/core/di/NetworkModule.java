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
import okhttp3.HttpUrl;
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
        return createOkHttpClient(
                context,
                loggingInterceptor,
                sessionManager,
                BuildConfig.API_BASE_URL,
                BuildConfig.API_ORIGIN);
    }

    public OkHttpClient createOkHttpClient(
            Context context,
            HttpLoggingInterceptor loggingInterceptor,
            SessionManager sessionManager,
            String apiBaseUrl,
            String apiOrigin) {
        Interceptor bearerInterceptor = chain -> {
            Request original = chain.request();
            String path = original.url().encodedPath();
            String host = original.url().host();
            String apiOriginHost = HttpUrl.get(apiOrigin).host();
            boolean isApiHost = host.equalsIgnoreCase(apiOriginHost);

            // Do not attach access token to login, register, or refresh endpoints
            if (path.contains("/auth/login") || path.contains("/auth/register") || path.contains("/auth/refresh")) {
                return chain.proceed(original);
            }

            Request.Builder requestBuilder = original.newBuilder();
            boolean authAttached = false;
            
            if (isApiHost) {
                String token = sessionManager.getAccessToken();
                if (token != null && !token.isEmpty()) {
                    requestBuilder.header("Authorization", "Bearer " + token);
                    authAttached = true;
                }
                
                if (path.startsWith("/api/v1")) {
                    requestBuilder.header("Accept", "application/json");
                }
            }
            
            Response response = chain.proceed(requestBuilder.build());
            
            boolean isPhotoPath = path.startsWith("/uploads") || (path.contains("/media/") && path.endsWith("/access"));
            if (isPhotoPath && BuildConfig.DEBUG) {
                boolean redirected = response.priorResponse() != null || response.code() == 307 || response.code() == 302;
                android.util.Log.d("PHOTO_HTTP", String.format("apiHost=%b authAttached=%b result=%d redirected=%b",
                        isApiHost, authAttached, response.code(), redirected));
            }

            return response;
        };

        Authenticator authenticator = new Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) throws IOException {
                if (responseCount(response) >= 2) {
                    return null;
                }

                if (response.code() != 401) {
                    return null;
                }

                String path = response.request().url().encodedPath();
                boolean isPhoto = path.startsWith("/uploads") || (path.contains("/media/") && path.endsWith("/access"));

                boolean isTokenExpired;
                try (ResponseBody peekBody = response.peekBody(1024)) {
                    String errorBody = peekBody.string();
                    isTokenExpired = errorBody.contains("TOKEN_EXPIRED") || errorBody.isEmpty();
                } catch (Exception ignored) {
                    isTokenExpired = true;
                }

                if (!isTokenExpired) {
                    return null;
                }

                synchronized (this) {
                    String currentToken = sessionManager.getAccessToken();
                    String requestToken = response.request().header("Authorization");

                    if (requestToken != null && currentToken != null && !requestToken.contains(currentToken)) {
                        if (isPhoto && BuildConfig.DEBUG) {
                            android.util.Log.d("PHOTO_REFRESH", "attempted=false shared=true result=success");
                        }
                        return response.request().newBuilder()
                                .header("Authorization", "Bearer " + currentToken)
                                .build();
                    }

                    String refreshToken = sessionManager.getRefreshToken();
                    if (refreshToken == null || refreshToken.isEmpty()) {
                        if (isPhoto && BuildConfig.DEBUG) {
                            android.util.Log.d("PHOTO_REFRESH", "attempted=true shared=false result=failure");
                        }
                        handleSessionExpired(sessionManager, context);
                        return null;
                    }

                    Retrofit refreshRetrofit = new Retrofit.Builder()
                            .baseUrl(apiBaseUrl)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();

                    ApiService refreshApi = refreshRetrofit.create(ApiService.class);
                    retrofit2.Response<ApiEnvelope<RefreshData>> refreshResponse = 
                            refreshApi.refresh(new RefreshRequest(refreshToken)).execute();

                    if (refreshResponse.isSuccessful() && refreshResponse.body() != null && refreshResponse.body().success) {
                        RefreshData data = refreshResponse.body().data;
                        sessionManager.saveTokens(data.accessToken, data.refreshToken);

                        if (isPhoto && BuildConfig.DEBUG) {
                            android.util.Log.d("PHOTO_REFRESH", "attempted=true shared=false result=success");
                        }

                        return response.request().newBuilder()
                                .header("Authorization", "Bearer " + data.accessToken)
                                .build();
                    } else {
                        if (isPhoto && BuildConfig.DEBUG) {
                            android.util.Log.d("PHOTO_REFRESH", "attempted=true shared=false result=failure");
                        }
                        handleSessionExpired(sessionManager, context);
                        return null;
                    }
                }
            }
        };

        Interceptor redirectScrubber = chain -> {
            Request request = chain.request();
            HttpUrl originUrl = HttpUrl.get(apiOrigin);
            boolean isApiHost = request.url().host().equalsIgnoreCase(originUrl.host()) && request.url().port() == originUrl.port();
            
            if (!isApiHost) {
                return chain.proceed(request.newBuilder()
                        .removeHeader("Authorization")
                        .build());
            }
            return chain.proceed(request);
        };
        
        return new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(bearerInterceptor)
                .addNetworkInterceptor(redirectScrubber)
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
