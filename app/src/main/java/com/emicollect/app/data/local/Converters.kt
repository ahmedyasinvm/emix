package com.emicollect.app.data.local

import androidx.room.TypeConverter
import com.emicollect.app.data.local.entity.EmiFrequency
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromEmiFrequency(value: String?): EmiFrequency? {
        return value?.let { EmiFrequency.valueOf(it) }
    }

    @TypeConverter
    fun emiFrequencyToString(frequency: EmiFrequency?): String? {
        return frequency?.name
    }

    @TypeConverter
    fun fromTransactionType(value: String?): com.emicollect.app.data.local.entity.TransactionType? {
        return value?.let { com.emicollect.app.data.local.entity.TransactionType.valueOf(it) }
    }

    @TypeConverter
    fun transactionTypeToString(type: com.emicollect.app.data.local.entity.TransactionType?): String? {
        return type?.name
    }
}
