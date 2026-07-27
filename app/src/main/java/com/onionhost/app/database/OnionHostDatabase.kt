package com.onionhost.app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.onionhost.app.database.dao.LogDao
import com.onionhost.app.database.dao.VisitorStatDao
import com.onionhost.app.database.dao.WebsiteDao
import com.onionhost.app.database.entity.LogEntity
import com.onionhost.app.database.entity.VisitorStatEntity
import com.onionhost.app.database.entity.WebsiteEntity

@Database(
    entities = [WebsiteEntity::class, VisitorStatEntity::class, LogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class OnionHostDatabase : RoomDatabase() {
    abstract fun websiteDao(): WebsiteDao
    abstract fun visitorStatDao(): VisitorStatDao
    abstract fun logDao(): LogDao
}
