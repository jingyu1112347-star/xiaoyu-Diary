package com.xiaoyou.diary

import android.content.Context
import com.xiaoyou.diary.data.repository.BehaviorRepository
import com.xiaoyou.diary.data.storage.DiaryStorage
import com.xiaoyou.diary.data.storage.SettingsStorage

/**
 * 简易 Service Locator，提供全局单例
 */
object ServiceLocator {
    @Volatile private var repo: BehaviorRepository? = null
    @Volatile private var settings: SettingsStorage? = null
    @Volatile private var diary: DiaryStorage? = null

    fun provideRepository(context: Context): BehaviorRepository {
        return repo ?: synchronized(this) {
            repo ?: BehaviorRepository(context).also { repo = it }
        }
    }

    fun provideSettings(context: Context): SettingsStorage {
        return settings ?: synchronized(this) {
            settings ?: SettingsStorage(context).also { settings = it }
        }
    }

    fun provideDiary(context: Context): DiaryStorage {
        return diary ?: synchronized(this) {
            diary ?: DiaryStorage(context).also { diary = it }
        }
    }
}
