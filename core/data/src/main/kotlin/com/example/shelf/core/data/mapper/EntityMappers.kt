package com.example.shelf.core.data.mapper

import com.example.shelf.core.database.model.BookEntity
import com.example.shelf.core.database.model.GoalEntity
import com.example.shelf.core.database.model.ReadingLogEntity
import com.example.shelf.core.database.model.ShelfBookRelation
import com.example.shelf.core.database.model.ShelfEntryEntity
import com.example.shelf.core.model.Book
import com.example.shelf.core.model.BookSummary
import com.example.shelf.core.model.DailyGoal
import com.example.shelf.core.model.GoalChange
import com.example.shelf.core.model.ReadingLog
import com.example.shelf.core.model.ShelfBook
import com.example.shelf.core.model.ShelfEntry
import java.time.Instant
import java.time.LocalDate

internal fun BookEntity.toBook(): Book = Book(
    workKey = workKey,
    title = title,
    authors = authors,
    firstPublishYear = firstPublishYear,
    coverId = coverId,
    description = description,
    subjects = subjects,
    pageCount = pageCount,
    isbn = isbn,
    hasDetails = detailsFetchedAt != null,
)

/** A search result seeds the cache; details are filled in when the book is opened. */
internal fun BookSummary.toEntity(): BookEntity = BookEntity(
    workKey = workKey,
    title = title,
    authors = authors,
    firstPublishYear = firstPublishYear,
    coverId = coverId,
    description = null,
    subjects = emptyList(),
    pageCount = pageCount,
    isbn = isbn,
    detailsFetchedAt = null,
)

internal fun ShelfEntryEntity.toShelfEntry(): ShelfEntry = ShelfEntry(
    workKey = workKey,
    shelf = shelf,
    currentPage = currentPage.coerceAtLeast(0),
    rating = rating,
    notes = notes,
    addedAt = Instant.ofEpochMilli(addedAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    finishedAt = finishedAt?.let(Instant::ofEpochMilli),
)

internal fun ShelfEntry.toEntity(): ShelfEntryEntity = ShelfEntryEntity(
    workKey = workKey,
    shelf = shelf,
    currentPage = currentPage,
    rating = rating,
    notes = notes,
    addedAt = addedAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    finishedAt = finishedAt?.toEpochMilli(),
)

internal fun ShelfBookRelation.toShelfBook(): ShelfBook = ShelfBook(book = book.toBook(), entry = entry.toShelfEntry())

internal fun ReadingLogEntity.toReadingLog(): ReadingLog = ReadingLog(
    id = id,
    date = LocalDate.ofEpochDay(epochDay),
    pages = pages,
    minutes = minutes,
    workKey = workKey,
)

internal fun ReadingLog.toEntity(createdAt: Instant): ReadingLogEntity = ReadingLogEntity(
    id = id,
    workKey = workKey,
    epochDay = date.toEpochDay(),
    pages = pages,
    minutes = minutes,
    createdAt = createdAt.toEpochMilli(),
)

internal fun GoalEntity.toGoalChange(): GoalChange = GoalChange(
    effectiveFrom = LocalDate.ofEpochDay(effectiveFromEpochDay),
    goal = DailyGoal(type = type, target = target.coerceIn(DailyGoal.TARGET_RANGE)),
)
