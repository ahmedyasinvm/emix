package com.emicollect.app.di

import android.content.Context
import androidx.room.Room
import com.emicollect.app.data.local.AppDatabase
import com.emicollect.app.data.local.dao.CustomerDao
import com.emicollect.app.data.local.dao.LoanDao
import com.emicollect.app.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "emi_collect_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideCustomerDao(database: AppDatabase): CustomerDao = database.customerDao()

    @Provides
    @Singleton
    fun provideLoanDao(database: AppDatabase): LoanDao = database.loanDao()

    @Provides
    @Singleton
    fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()
}
