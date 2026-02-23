package com.kunpitech.pahadiraah.di

import com.kunpitech.pahadiraah.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds @Singleton
    abstract fun bindRouteRepository(impl: RouteRepositoryImpl): RouteRepository

    @Binds @Singleton
    abstract fun bindBookingRepository(impl: BookingRepositoryImpl): BookingRepository

    @Binds @Singleton
    abstract fun bindReviewRepository(impl: ReviewRepositoryImpl): ReviewRepository

    @Binds @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    @Binds @Singleton
    abstract fun bindVehicleRepository(impl: VehicleRepositoryImpl): VehicleRepository
}