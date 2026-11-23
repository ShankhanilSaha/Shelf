package com.example.shelf.core.data.repository

import app.cash.turbine.test
import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfResult
import com.example.shelf.core.database.ShelfDatabase
import com.example.shelf.core.network.model.AuthorDto
import com.example.shelf.core.network.model.EditionDto
import com.example.shelf.core.network.model.KeyRefDto
import com.example.shelf.core.network.model.SearchDocDto
import com.example.shelf.core.network.model.SearchResponseDto
import com.example.shelf.core.network.model.WorkAuthorDto
import com.example.shelf.core.network.model.WorkDto
import com.example.shelf.core.testing.MutableClock
import com.example.shelf.core.testing.TestData
import com.example.shelf.core.testing.inMemoryShelfDatabase
import com.example.shelf.core.testing.network.FakeOpenLibraryDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class OfflineFirstBookRepositoryTest {
    private lateinit var db: ShelfDatabase
    private val network = FakeOpenLibraryDataSource()
    private val clock = MutableClock()
    private lateinit var repository: OfflineFirstBookRepository

    @Before
    fun setUp() {
        db = inMemoryShelfDatabase(RuntimeEnvironment.getApplication())
        repository = OfflineFirstBookRepository(db.bookDao(), network, clock)
        network.works["OL1W"] = WorkDto(
            key = "/works/OL1W",
            title = "Dune",
            description = buildJsonObject { put("value", "Spice must flow.") },
            subjects = listOf("Science fiction"),
            covers = listOf(42),
            authors = listOf(WorkAuthorDto(author = JsonPrimitive("/authors/OL7A"))),
            firstPublishDate = "1965",
        )
        network.firstEditions["OL1W"] = EditionDto(numberOfPages = 604)
        network.authors["OL7A"] = AuthorDto(key = "/authors/OL7A", name = "Frank Herbert")
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `refresh stores work, page count and author names in the cache`() = runTest {
        repository.observeBook("OL1W").test {
            assertNull(awaitItem())

            assertEquals(ShelfResult.Success(Unit), repository.refreshBook("OL1W"))

            val book = awaitItem()!!
            assertEquals("Dune", book.title)
            assertEquals(listOf("Frank Herbert"), book.authors)
            assertEquals("Spice must flow.", book.description)
            assertEquals(604, book.pageCount)
            assertEquals(1965, book.firstPublishYear)
            assertTrue(book.hasDetails)
        }
    }

    @Test
    fun `a book that did not come from search takes the earliest year from the search index`() = runTest {
        network.works["OL1W"] = network.works.getValue("OL1W").copy(firstPublishDate = "November 12, 1972")
        network.searchResponse = { query, _ ->
            if (query == "key:/works/OL1W") {
                SearchResponseDto(
                    numFound = 1,
                    docs = listOf(SearchDocDto(key = "/works/OL1W", firstPublishYear = 1954)),
                )
            } else {
                SearchResponseDto()
            }
        }

        assertEquals(ShelfResult.Success(Unit), repository.refreshBook("OL1W"))

        assertEquals(1954, repository.observeBook("OL1W").first()?.firstPublishYear)
    }

    @Test
    fun `a book seeded from search does not ask the index again`() = runTest {
        repository.cacheSummary(TestData.summary(1))

        repository.refreshBook("OL1W")

        assertTrue(network.requests.none { it.startsWith("search:") })
    }

    @Test
    fun `offline refresh fails but keeps the cached book`() = runTest {
        repository.cacheSummary(TestData.summary(1, title = "Dune (cached)"))
        network.offline = true

        val result = repository.refreshBook("OL1W")

        assertEquals(ShelfError.Network, (result as ShelfResult.Failure).error)
        assertEquals("Dune (cached)", repository.observeBook("OL1W").first()?.title)
    }

    @Test
    fun `failing edition and author requests still save the work`() = runTest {
        repository.cacheSummary(TestData.summary(1, authors = listOf("From search"), pageCount = 500))
        network.failingEditions = true
        network.failingAuthors = true

        assertEquals(ShelfResult.Success(Unit), repository.refreshBook("OL1W"))

        val book = repository.observeBook("OL1W").first()!!
        assertEquals("Spice must flow.", book.description)
        assertEquals(listOf("From search"), book.authors)
        assertEquals(500, book.pageCount)
    }

    @Test
    fun `an unknown work is reported as not found`() = runTest {
        val result = repository.refreshBook("OL999W")

        assertEquals(ShelfError.NotFound, (result as ShelfResult.Failure).error)
    }

    @Test
    fun `caching a summary never overwrites fetched details`() = runTest {
        repository.refreshBook("OL1W")

        repository.cacheSummary(TestData.summary(1, title = "Older search title"))

        assertEquals("Dune", repository.observeBook("OL1W").first()?.title)
    }

    @Test
    fun `isbn lookup returns the work and seeds the cache`() = runTest {
        network.editionsByIsbn["9780261103573"] = EditionDto(
            title = "The Fellowship of the Ring",
            numberOfPages = 416,
            works = listOf(KeyRefDto("/works/OL27513W")),
        )

        val result = repository.findWorkByIsbn("9780261103573")

        assertEquals(ShelfResult.Success("OL27513W"), result)
        val seeded = repository.observeBook("OL27513W").first()!!
        assertEquals("The Fellowship of the Ring", seeded.title)
        assertEquals("9780261103573", seeded.isbn)
    }

    @Test
    fun `isbn without a linked work, or unknown, is not found`() = runTest {
        network.editionsByIsbn["9780306406157"] = EditionDto(title = "Orphan edition")

        assertEquals(ShelfError.NotFound, (repository.findWorkByIsbn("9780306406157") as ShelfResult.Failure).error)
        assertEquals(ShelfError.NotFound, (repository.findWorkByIsbn("9781234567897") as ShelfResult.Failure).error)
    }
}
