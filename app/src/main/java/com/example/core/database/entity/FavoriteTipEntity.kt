package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.DailyStudyTip

/**
 * Room Entity to locally store favorited Daily Study Tips for Class 12 Commerce students.
 */
@Entity(tableName = "favorite_study_tips")
data class FavoriteTipEntity(
    @PrimaryKey
    val id: String,
    val subjectId: String,
    val subjectName: String,
    val headline: String,
    val tipContent: String,
    val actionStep: String,
    val language: String,
    val source: String,
    val savedAtEpochMillis: Long = System.currentTimeMillis()
) {
    fun toDailyStudyTip(): DailyStudyTip {
        return DailyStudyTip(
            id = id,
            subjectId = subjectId,
            subjectName = subjectName,
            headline = headline,
            tipContent = tipContent,
            actionStep = actionStep,
            language = language,
            source = source,
            timestampEpochMillis = savedAtEpochMillis
        )
    }

    companion object {
        fun fromDailyStudyTip(tip: DailyStudyTip): FavoriteTipEntity {
            return FavoriteTipEntity(
                id = tip.id,
                subjectId = tip.subjectId,
                subjectName = tip.subjectName,
                headline = tip.headline,
                tipContent = tip.tipContent,
                actionStep = tip.actionStep,
                language = tip.language,
                source = tip.source,
                savedAtEpochMillis = System.currentTimeMillis()
            )
        }
    }
}
