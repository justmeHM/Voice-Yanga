package com.voiceyanga.citizen.core.di;

import com.voiceyanga.citizen.data.repository.FakeAuthRepository;
import com.voiceyanga.citizen.domain.repository.AuthRepository;
import javax.inject.Singleton;
import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;

/**
 * Hilt module to provide repository implementations.
 * [Rule 55] Allows easy switching between Fake and Production implementations.
 */
@Module
@InstallIn(SingletonComponent.class)
public abstract class RepositoryModule {

    @Binds
    @Singleton
    public abstract AuthRepository bindAuthRepository(FakeAuthRepository impl);
}
