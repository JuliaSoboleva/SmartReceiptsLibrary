package co.smartreceipts.android.model.impl

import co.smartreceipts.android.DefaultObjects
import co.smartreceipts.android.model.Category
import co.smartreceipts.android.sync.model.SyncState
import co.smartreceipts.android.utils.testParcel
import junit.framework.Assert
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.core.IsNot.not
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class ImmutableCategoryImplTest {

    companion object {

        private const val ID = 3
        private val CAT_UUID = UUID.randomUUID()
        private const val NAME = "name"
        private const val CODE = "code"
        private const val CUSTOM_ORDER_ID: Long = 15
    }

    // Class under test
    private lateinit var immutableCategory: ImmutableCategoryImpl

    private lateinit var syncState: SyncState

    @Before
    fun setUp() {
        syncState = DefaultObjects.newDefaultSyncState()
        immutableCategory =
                ImmutableCategoryImpl(ID, CAT_UUID, NAME, CODE, syncState, CUSTOM_ORDER_ID)
    }

    @Test
    fun getName() {
        assertEquals(NAME, immutableCategory.name)
    }

    @Test
    fun getUuid() {
        assertEquals(CAT_UUID, immutableCategory.uuid)
    }

    @Test
    fun getCode() {
        assertEquals(CODE, immutableCategory.code)
    }

    @Test
    fun getSyncState() {
        assertEquals(syncState, immutableCategory.syncState)
    }

    @Test
    fun getCustomOrderId() {
        assertEquals(CUSTOM_ORDER_ID, immutableCategory.customOrderId)
    }

    @Test
    fun equals() {
        assertEquals(immutableCategory, immutableCategory)
        assertEquals(
            immutableCategory,
            ImmutableCategoryImpl(ID, CAT_UUID, NAME, CODE, syncState, CUSTOM_ORDER_ID)
        )
        assertThat(immutableCategory, not(equalTo(Any())))
        assertThat(immutableCategory, not(equalTo(mock(Category::class.java))))
        assertThat(
            immutableCategory,
            not(equalTo(ImmutableCategoryImpl(0, CAT_UUID, NAME, CODE, syncState, CUSTOM_ORDER_ID)))
        )
        assertThat(
            immutableCategory,
            not(
                equalTo(
                    ImmutableCategoryImpl(ID, CAT_UUID, "wrong", CODE, syncState, CUSTOM_ORDER_ID)
                )
            )
        )
        assertThat(
            immutableCategory,
            not(
                equalTo(
                    ImmutableCategoryImpl(ID, CAT_UUID, NAME, "wrong", syncState, CUSTOM_ORDER_ID)
                )
            )
        )
        assertThat(
            immutableCategory,
            not(
                equalTo(
                    ImmutableCategoryImpl(ID, CAT_UUID, NAME, "wrong", syncState, (CUSTOM_ORDER_ID + 1))
                )
            )
        )
        assertThat(
            immutableCategory,
            not(equalTo(ImmutableCategoryImpl(ID, UUID.randomUUID(), NAME, CODE, syncState, CUSTOM_ORDER_ID)))
        )
    }

    @Test
    fun compare() {
        val category2 =
            ImmutableCategoryImpl(ID, CAT_UUID, NAME, CODE, syncState, (CUSTOM_ORDER_ID + 1))
        val category0 =
            ImmutableCategoryImpl(ID, CAT_UUID, NAME, CODE, syncState, (CUSTOM_ORDER_ID - 1))

        val list = mutableListOf<ImmutableCategoryImpl>().apply {
            add(immutableCategory)
            add(category2)
            add(category0)
            sort()
        }

        assertEquals(category0, list[0])
        assertEquals(immutableCategory, list[1])
        assertEquals(category2, list[2])
    }

    @Test
    fun parcelEquality() {
        val categoryFromParcel = immutableCategory.testParcel()

        junit.framework.Assert.assertNotSame(immutableCategory, categoryFromParcel)
        Assert.assertEquals(immutableCategory, categoryFromParcel)

    }

}