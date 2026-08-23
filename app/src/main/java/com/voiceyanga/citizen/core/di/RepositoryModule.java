package com.voiceyanga.citizen.core.di;

import com.voiceyanga.citizen.data.repository.RealAuthRepository;
import com.voiceyanga.citizen.data.repository.RealReferenceRepository;
import com.voiceyanga.citizen.domain.repository.AuthRepository;
import com.voiceyanga.citizen.domain.repository.ReferenceRepository;
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
    public abstract AuthRepository bindAuthRepository(RealAuthRepository impl);

    @Binds
    @Singleton
    public abstract ReferenceRepository bindReferenceRepository(RealReferenceRepository impl);
}
