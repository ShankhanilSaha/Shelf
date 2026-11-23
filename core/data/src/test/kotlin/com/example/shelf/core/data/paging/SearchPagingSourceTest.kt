package com.example.shelf.core.data.paging

import androidx.paging.PagingConfig
import androidx.paging.PagingSource.LoadResult
import androidx.paging.testing.TestPager
import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.shelfError
import com.example.shelf.core.model.BookSummary
import com.example.shelf.core.network.OpenLibraryApiFactory
import com.example.shelf.core.network.OpenLibraryDataSource
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SearchPagingSourceTest {
    private val server = MockWebServer()
    private lateinit var network: OpenLibraryDataSource
    private val config = PagingConfig(pageSize = 20, initialLoadSize = 20, enablePlaceholders = false)

    @Before
    fun setUp() {
        server.start()
        network = OpenLibraryApiFactory.createDataSource(server.url("/").toString(), OkHttpClient())
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun pager(query: String = "dune") = TestPager(config, SearchPagingSource(network, query))

    @Test
    fun `refresh loads page one of twenty and points at page two`() = runTest {
        server.enqueue(MockResponse(body = searchPage(numFound = 45, firstId = 1, count = 20)))

        val page = assertIs<LoadResult.Page<Int, BookSummary>>(pager().refresh())

        assertEquals(20, page.data.size)
        assertEquals("OL1W", page.data.first().workKey)
        assertNull(page.prevKey)
        assertEquals(2, page.nextKey)
        val url = server.takeRequest().url
        assertEquals("dune", url.queryParameter("q"))
        assertEquals("1", url.queryParameter("page"))
        assertEquals("20", url.queryParameter("limit"))
    }

    @Test
    fun `append walks pages until numFound is reached`() = runTest {
        server.enqueue(MockResponse(body = searchPage(numFound = 45, firstId = 1, count = 20)))
        server.enqueue(MockResponse(body = searchPage(numFound = 45, firstId = 21, count = 20)))
        server.enqueue(MockResponse(body = searchPage(numFound = 45, firstId = 41, count = 5)))
        val pager = pager()

        pager.refresh()
        val second = assertIs<LoadResult.Page<Int, BookSummary>>(pager.append())
        val third = assertIs<LoadResult.Page<Int, BookSummary>>(pager.append())

        assertEquals(1, second.prevKey)
        assertEquals(3, second.nextKey)
        assertEquals(5, third.data.size)
        assertNull(third.nextKey)
        assertEquals(listOf("1", "2", "3"), List(3) { server.takeRequest().url.queryParameter("page") })
    }

    @Test
    fun `results with a blank title are skipped without ending paging early`() = runTest {
        server.enqueue(
            MockResponse(
                body = """{"numFound":40,"start":0,"docs":[
                    {"key":"/works/OL1W","title":"   "},
                    {"key":"/works/OL2W","title":"Kept"},
                    {"key":"/books/OL3M","title":"Not a work"}
                ]}""",
            ),
        )

        val page = assertIs<LoadResult.Page<Int, BookSummary>>(pager().refresh())

        assertEquals(listOf("OL2W"), page.data.map { it.workKey })
        assertEquals(2, page.nextKey)
    }

    @Test
    fun `an empty page ends paging`() = runTest {
        server.enqueue(MockResponse(body = """{"numFound":0,"start":0,"docs":[]}"""))

        val page = assertIs<LoadResult.Page<Int, BookSummary>>(pager().refresh())

        assertEquals(emptyList(), page.data)
        assertNull(page.nextKey)
    }

    @Test
    fun `server errors become a classified LoadResult Error`() = runTest {
        server.enqueue(MockResponse(code = 500))

        val error = assertIs<LoadResult.Error<Int, BookSummary>>(pager().refresh())

        assertEquals(ShelfError.Server, error.throwable.shelfError)
    }

    @Test
    fun `refresh key re-centres on the page around the anchor`() = runTest {
        server.enqueue(MockResponse(body = searchPage(numFound = 60, firstId = 1, count = 20)))
        server.enqueue(MockResponse(body = searchPage(numFound = 60, firstId = 21, count = 20)))
        val pager = pager()
        pager.refresh()
        pager.append()

        val source = SearchPagingSource(network, "dune")
        assertEquals(2, source.getRefreshKey(pager.getPagingState(anchorPosition = 25)))
        assertEquals(1, source.getRefreshKey(pager.getPagingState(anchorPosition = 3)))
    }

    private fun searchPage(numFound: Int, firstId: Int, count: Int): String {
        val docs = (firstId until firstId + count).joinToString(",") { id ->
            """{"key":"/works/OL${id}W","title":"Book $id","author_name":["Author $id"]}"""
        }
        return """{"numFound":$numFound,"start":${firstId - 1},"docs":[$docs]}"""
    }
}
