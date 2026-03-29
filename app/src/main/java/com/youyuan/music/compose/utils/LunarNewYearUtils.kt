package com.youyuan.music.compose.utils

import android.icu.util.ChineseCalendar
import android.icu.util.TimeZone
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone as JavaTimeZone

object LunarNewYearUtils {
    enum class NewYearDay {
        NONE,
        CHU_XI,
        CHU_YI
    }

    fun getNewYearDay(
        currentTimeMillis: Long = System.currentTimeMillis(),
        timeZone: JavaTimeZone = JavaTimeZone.getDefault()
    ): NewYearDay {
        val today = chineseCalendarAt(currentTimeMillis, timeZone)

        val todayMonth = today.get(ChineseCalendar.MONTH)
        val todayDay = today.get(ChineseCalendar.DAY_OF_MONTH)
        val todayLeapMonth = today.get(ChineseCalendar.IS_LEAP_MONTH)

        if (todayMonth == 0 && todayDay == 1 && todayLeapMonth == 0) {
            return NewYearDay.CHU_YI
        }

        val tomorrowMillis = Calendar.getInstance(timeZone).run {
            timeInMillis = currentTimeMillis
            add(Calendar.DAY_OF_YEAR, 1)
            timeInMillis
        }
        val tomorrow = chineseCalendarAt(tomorrowMillis, timeZone)
        val tomorrowMonth = tomorrow.get(ChineseCalendar.MONTH)
        val tomorrowDay = tomorrow.get(ChineseCalendar.DAY_OF_MONTH)
        val tomorrowLeapMonth = tomorrow.get(ChineseCalendar.IS_LEAP_MONTH)

        if (tomorrowMonth == 0 && tomorrowDay == 1 && tomorrowLeapMonth == 0) {
            return NewYearDay.CHU_XI
        }

        return NewYearDay.NONE
    }

    fun isNewYearFestivalDay(
        currentTimeMillis: Long = System.currentTimeMillis(),
        timeZone: JavaTimeZone = JavaTimeZone.getDefault()
    ): Boolean {
        return getNewYearDay(currentTimeMillis, timeZone) != NewYearDay.NONE
    }

    private fun chineseCalendarAt(
        currentTimeMillis: Long,
        timeZone: JavaTimeZone
    ): ChineseCalendar {
        val icuTimeZone = TimeZone.getTimeZone(timeZone.id)
        return ChineseCalendar(icuTimeZone, Locale.CHINA).apply {
            timeInMillis = currentTimeMillis
        }
    }
}
