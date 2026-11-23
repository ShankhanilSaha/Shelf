package com.example.shelf.core.network

import com.example.shelf.core.common.result.ShelfError
import com.example.shelf.core.common.result.ShelfException
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class OpenLibraryApiTest {
    private val server = MockWebServer()
    private lateinit var dataSource: OpenLibraryDataSource

    @Before
    fun setUp() {
        server.start()
        val api = OpenLibraryApiFactory.create(server.url("/").toString(), OkHttpClient())
        dataSource = RetrofitOpenLibraryDataSource(api)
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `search sends the documented query and parses documents`() = runTest {
        server.enqueue(MockResponse(body = SEARCH_JSON))

        val response = dataSource.search(query = "the hobbit", page = 2, limit = 20)

        val url = server.takeRequest().url
        assertEquals("/search.json", url.encodedPath)
        assertEquals("the hobbit", url.queryParameter("q"))
        assertEquals("2", url.queryParameter("page"))
        assertEquals("20", url.queryParameter("limit"))
        assertEquals(OpenLibraryApi.SEARCH_FIELDS, url.queryParameter("fields"))
        assertEquals(464, response.numFound)
        val doc = response.docs.first()
        assertEquals("/works/OL27482W", doc.key)
        assertEquals(listOf("J.R.R. Tolkien"), doc.authorNames)
        assertEquals(14627509L, doc.coverId)
        assertEquals(1937, doc.firstPublishYear)
        assertEquals(310, doc.numberOfPagesMedian)
        // Fields missing from a document fall back to defaults instead of failing the page.
        assertEquals(emptyList(), response.docs[1].authorNames)
        assertNull(response.docs[1].title)
    }

    @Test
    fun `work description may be an object with a value`() = runTest {
        server.enqueue(MockResponse(body = WORK_JSON))

        val work = dataSource.work("OL27482W")

        assertEquals("/works/OL27482W", server.takeRequest().url.encodedPath.removeSuffix(".json"))
        assertEquals("The Hobbit is a tale of high adventure.", work.descriptionText)
        assertEquals(listOf("/authors/OL26320A"), work.authorKeys)
        assertEquals(14627509L, work.firstCoverId)
        assertEquals(listOf("Fantasy", "Dragons"), work.subjects)
        assertEquals("January 1938", work.firstPublishDate)
    }

    @Test
    fun `work description may be a plain string and covers may start with -1`() = runTest {
        server.enqueue(
            MockResponse(
                body = """{"key":"/works/OL1W","title":"T","description":"  Plain text.  ","covers":[-1, 42],
                    |"authors":[{"author":"/authors/OL2A"}]}
                """.trimMargin(),
            ),
        )

        val work = dataSource.work("OL1W")

        assertEquals("Plain text.", work.descriptionText)
        assertEquals(42L, work.firstCoverId)
        assertEquals(listOf("/authors/OL2A"), work.authorKeys)
    }

    @Test
    fun `first edition carries the page count`() = runTest {
        server.enqueue(MockResponse(body = EDITIONS_JSON))

        val edition = dataSource.firstEdition("OL27482W")

        val url = server.takeRequest().url
        assertEquals("/works/OL27482W/editions.json", url.encodedPath)
        assertEquals("1", url.queryParameter("limit"))
        assertEquals(380, edition?.numberOfPages)
    }

    @Test
    fun `a work without editions has no first edition`() = runTest {
        server.enqueue(MockResponse(body = """{"size":0,"entries":[]}"""))

        assertNull(dataSource.firstEdition("OL1W"))
    }

    @Test
    fun `isbn lookup follows the redirect to the edition`() = runTest {
        server.enqueue(
            MockResponse.Builder().code(302).setHeader("Location", "/books/OL10236417M.json").build(),
        )
        server.enqueue(MockResponse(body = ISBN_EDITION_JSON))

        val edition = dataSource.editionByIsbn("9780261103573")

        assertEquals("/isbn/9780261103573.json", server.takeRequest().url.encodedPath)
        assertEquals("/books/OL10236417M.json", server.takeRequest().url.encodedPath)
        assertEquals("/works/OL27513W", edition.works.single().key)
        assertEquals(416, edition.numberOfPages)
    }

    @Test
    fun `author name falls back to the personal name`() = runTest {
        server.enqueue(MockResponse(body = """{"key":"/authors/OL1A","personal_name":"Ursula K. Le Guin"}"""))

        assertEquals("Ursula K. Le Guin", dataSource.author("OL1A").displayName)
    }

    @Test
    fun `errors are classified for the UI`() = runTest {
        server.enqueue(MockResponse(code = 404, body = """{"error":"notfound"}"""))
        server.enqueue(MockResponse(code = 503))
        server.enqueue(MockResponse(body = "<html>not json</html>"))

        assertEquals(ShelfError.NotFound, assertFailsWith<ShelfException> { dataSource.work("OL1W") }.error)
        assertEquals(ShelfError.Server, assertFailsWith<ShelfException> { dataSource.work("OL1W") }.error)
        assertEquals(ShelfError.Server, assertFailsWith<ShelfException> { dataSource.work("OL1W") }.error)
    }

    @Test
    fun `connection failures are network errors`() = runTest {
        server.close()

        val error = assertFailsWith<ShelfException> { dataSource.search("dune", page = 1, limit = 20) }

        assertEquals(ShelfError.Network, error.error)
    }

    private companion object {
        val SEARCH_JSON = """
            {"numFound":464,"start":20,"numFoundExact":true,"docs":[
              {"key":"/works/OL27482W","title":"The Hobbit","author_name":["J.R.R. Tolkien"],
               "first_publish_year":1937,"cover_i":14627509,"isbn":["9780261103573"],
               "number_of_pages_median":310},
              {"key":"/works/OL9W"}
            ]}
        """.trimIndent()

        val WORK_JSON = """
            {"key":"/works/OL27482W","title":"The Hobbit",
             "description":{"type":"/type/text","value":"The Hobbit is a tale of high adventure."},
             "authors":[{"author":{"key":"/authors/OL26320A"},"type":{"key":"/type/author_role"}}],
             "covers":[14627509],"subjects":["Fantasy","Dragons"],"first_publish_date":"January 1938",
             "links":[{"title":"x","url":"http://example.com"}]}
        """.trimIndent()

        val EDITIONS_JSON = """
            {"links":{"self":"/works/OL27482W/editions.json?limit=1"},"size":482,
             "entries":[{"works":[{"key":"/works/OL27482W"}],"title":"Hobit","key":"/books/OL62623719M",
             "covers":[15261276],"isbn_13":["9788611171982"],"number_of_pages":380}]}
        """.trimIndent()

        val ISBN_EDITION_JSON = """
            {"key":"/books/OL10236417M","title":"The Fellowship of the Ring","number_of_pages":416,
             "covers":[8311423,-1],"isbn_13":["9780261103573"],"works":[{"key":"/works/OL27513W"}]}
        """.trimIndent()
    }
}
