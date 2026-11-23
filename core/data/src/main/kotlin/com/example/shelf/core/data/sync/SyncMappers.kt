package com.example.shelf.core.data.sync

import com.example.shelf.core.database.model.BookEntity
import com.example.shelf.core.database.model.ReadingLogEntity
import com.example.shelf.core.database.model.ShelfBookRelation
import com.example.shelf.core.database.model.ShelfEntryEntity

internal fun ShelfBookRelation.toRemoteBook(): RemoteBook = RemoteBook(
    workKey = entry.workKey,
    title = book.title,
    authors = book.authors,
    coverId = book.coverId,
    pageCount = book.pageCount,
    firstPublishYear = book.firstPublishYear,
    shelf = entry.shelf,
    currentPage = entry.currentPage,
    rating = entry.rating,
    notes = entry.notes,
    addedAt = entry.addedAt,
    updatedAt = entry.updatedAt,
    finishedAt = entry.finishedAt,
    deleted = entry.deleted,
)

/** A cache row for a book that came from another device; details are fetched when it is opened. */
internal fun RemoteBook.toBookSeed(): BookEntity = BookEntity(
    workKey = workKey,
    title = title,
    authors = authors,
    firstPublishYear = firstPublishYear,
    coverId = coverId,
    description = null,
    subjects = emptyList(),
    pageCount = pageCount,
    isbn = null,
    detailsFetchedAt = null,
)

/** The pulled version as a clean (already synced) row. */
internal fun RemoteBook.toShelfEntryEntity(): ShelfEntryEntity = ShelfEntryEntity(
    workKey = workKey,
    shelf = shelf,
    currentPage = currentPage,
    rating = rating,
    notes = notes,
    addedAt = addedAt,
    updatedAt = updatedAt,
    finishedAt = finishedAt,
    dirty = false,
    deleted = deleted,
)

internal fun ReadingLogEntity.toRemoteLog(): RemoteLog = RemoteLog(
    id = id,
    workKey = workKey,
    epochDay = epochDay,
    pages = pages,
    minutes = minutes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deleted = deleted,
)

/** The pulled version as a clean (already synced) row. */
internal fun RemoteLog.toEntity(): ReadingLogEntity = ReadingLogEntity(
    id = id,
    workKey = workKey,
    epochDay = epochDay,
    pages = pages,
    minutes = minutes,
    createdAt = createdAt,
    updatedAt = updatedAt,
    dirty = false,
    deleted = deleted,
)
