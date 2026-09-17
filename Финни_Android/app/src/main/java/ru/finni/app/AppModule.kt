package ru.finni.app

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.finni.core.data.FinniDatabase
import ru.finni.core.data.ProfileRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FinniDatabase =
        FinniDatabase.get(context)

    @Provides
    @Singleton
    fun provideProfileRepository(db: FinniDatabase): ProfileRepository =
        ProfileRepository(db)
}
