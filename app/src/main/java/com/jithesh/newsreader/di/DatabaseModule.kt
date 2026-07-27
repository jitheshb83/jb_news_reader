package com.jithesh.newsreader.di

import android.content.Context
import androidx.room.Room
import com.jithesh.newsreader.data.db.AppDatabase
import com.jithesh.newsreader.data.db.ArticleDao
import com.jithesh.newsreader.data.db.FeedDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "newsreader.db").build()

    @Provides
    fun provideFeedDao(db: AppDatabase): FeedDao = db.feedDao()

    @Provides
    fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()
}
