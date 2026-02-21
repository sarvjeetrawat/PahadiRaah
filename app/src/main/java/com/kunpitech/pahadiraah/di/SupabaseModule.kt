package com.kunpitech.pahadiraah.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    /**
     * Single SupabaseClient instance shared across the entire app.
     * Keys are read from BuildConfig, which reads from local.properties at build time.
     * Never hardcode keys here.
     */
    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl  = "https://ehnrzwrzzksvxlpgvwvy.supabase.co",
        supabaseKey  = "sb_publishable_Y--8WAyapHbhDl9HDPSoow_Vy7rq1Fu"
    ) {
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}